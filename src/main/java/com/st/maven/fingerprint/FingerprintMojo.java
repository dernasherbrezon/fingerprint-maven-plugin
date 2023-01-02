package com.st.maven.fingerprint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class FingerprintMojo extends AbstractMojo {

	/*
	 * All resources should have absolute paths: Valid: <img src="/img/test.png"> . Invalid: <img src="test.png"> All resources should point to existing files without any pre-processing: Valid: <img src="/img/test.png"> . Invalid: <img src="<c:if test="${var}">/img/test.png</c:if>"
	 */
	public static final Pattern LINK_PATTERN = Pattern.compile("(<link[^>]+href=\")(.*?)(\"[^>]*>)");
	public static final Pattern SCRIPT_PATTERN = Pattern.compile("(\")([^\\s]*?\\.js)(\")");
	public static final Pattern IMG_PATTERN = Pattern.compile("(<img[^>]+src=\")([^\\}\\{]*?)(\"[^>]+>)");
	public static final Pattern CSS_IMG_PATTERN = Pattern.compile("(url\\([\",'])(.*?)([\",']\\))");
	public static final Pattern JSTL_URL_PATTERN = Pattern.compile("(<c:url.*?value=\")(/{1}.*?)(\".*?>)");
	public static final Pattern DOLLAR_SIGN = Pattern.compile("\\$");

	/**
	 * target directory
	 */
	@Parameter(defaultValue = "${project.build.directory}/optimized-webapp", required = true)
	private File targetDirectory;

	/**
	 * Webapp directory
	 */
	@Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
	private File sourceDirectory;

	/**
	 * Exclude resources
	 */
	@Parameter
	private List<String> excludeResources;

	@Parameter
	private List<String> extensionsToFilter;

	@Parameter
	private boolean minifyHtml = true;

	@Parameter
	private Set<String> htmlExtensions;

	@Parameter
	private boolean minifyJs = true;

	@Parameter
	private boolean minifyCss = true;

	@Parameter(defaultValue = "[hash][name].[ext]")
	private String namePattern;

	/**
	 * CDN url
	 */
	@Parameter
	private String cdn;

	private final Set<String> processedFiles = new HashSet<>();
	private final Map<String, String> sourceToFingerprintedTarget = new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException {
		if (!sourceDirectory.isDirectory()) {
			throw new MojoExecutionException("source directory is not a directory: " + sourceDirectory.getAbsolutePath());
		}
		if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
			throw new MojoExecutionException("unable to create outputdirectory: " + targetDirectory.getAbsolutePath());
		}
		if (!targetDirectory.isDirectory()) {
			throw new MojoExecutionException("output directory is not a directory: " + targetDirectory.getAbsolutePath());
		}
		if (extensionsToFilter.isEmpty()) {
			getLog().info("no files to optimize found");
			return;
		}
		List<File> filesToOptimize = new ArrayList<>();
		findFilesToOptimize(filesToOptimize, sourceDirectory);
		if (filesToOptimize.isEmpty()) {
			getLog().info("no files to optimize were found");
			return;
		}

		mkDirs(sourceDirectory, targetDirectory);

		for (File cur : filesToOptimize) {
			try {
				process(cur);
				processedFiles.add(cur.getAbsolutePath());
			} catch (Exception e) {
				getLog().error("unable to process: " + cur.getAbsolutePath(), e);
				throw new MojoExecutionException("unable to process: " + cur.getAbsolutePath(), e);
			}
		}

		copyDeepFiles(sourceDirectory, targetDirectory);

		for (Entry<String, String> cur : sourceToFingerprintedTarget.entrySet()) {
			File toMove = new File(targetDirectory, cur.getKey());
			File dest = new File(targetDirectory, cur.getValue());
			if (toMove.exists() && !toMove.renameTo(dest)) {
				throw new MojoExecutionException("unable to move src: " + toMove.getAbsolutePath() + " dst: " + dest.getAbsolutePath());
			}
		}
	}

	private void process(File sourceFile) throws MojoExecutionException {
		if (getLog().isDebugEnabled()) {
			getLog().debug("processing file: " + sourceFile.getAbsolutePath());
		}
		String data = readFile(sourceFile);
		StringBuffer outputFileData = new StringBuffer(data);
		outputFileData = processPattern(LINK_PATTERN, outputFileData.toString(), sourceFile.getAbsolutePath());
		outputFileData = processPattern(SCRIPT_PATTERN, outputFileData.toString(), sourceFile.getAbsolutePath());
		outputFileData = processPattern(IMG_PATTERN, outputFileData.toString(), sourceFile.getAbsolutePath());
		outputFileData = processPattern(CSS_IMG_PATTERN, outputFileData.toString(), sourceFile.getAbsolutePath());
		outputFileData = processPattern(JSTL_URL_PATTERN, outputFileData.toString(), sourceFile.getAbsolutePath());
		String processedData = null;
		if (htmlExtensions != null && !htmlExtensions.isEmpty()) {
			String extension = getExtension(sourceFile.getName());
			if (extension != null && htmlExtensions.contains(extension) && minifyHtml) {
				getLog().info("minifying html: " + sourceFile.getAbsolutePath());
				processedData = HtmlMinifier.minify(outputFileData.toString());
			}
		} else if (sourceFile.getName().contains(".min.")) {
			getLog().info("ignoring already minified resource: " + sourceFile.getAbsolutePath());
		} else if (sourceFile.getName().endsWith(".js") && minifyJs) {
			processedData = outputFileData.toString();
			getLog().info("minifying javascript: " + sourceFile.getAbsolutePath());
			processedData = Compressor.compressJavaScript(new StringReader(processedData), getLog());
		} else if (sourceFile.getName().endsWith(".css") && minifyCss) {
			processedData = outputFileData.toString();
			getLog().info("minifying css: " + sourceFile.getAbsolutePath());
			processedData = Compressor.compressCSS(new StringReader(processedData), getLog());
		}

		if (processedData == null) {
			processedData = outputFileData.toString();
		}

		File targetFile = new File(targetDirectory, stripSourceDirectory(sourceDirectory, sourceFile));
		try (FileWriter w = new FileWriter(targetFile)) {
			IOUtils.write(processedData, w);
		} catch (IOException e) {
			throw new MojoExecutionException("unable to file: " + targetFile.getAbsolutePath(), e);
		}
	}

	private StringBuffer processPattern(Pattern p, String data, String sourceOfData) throws MojoExecutionException {
		StringBuffer outputFileData = new StringBuffer();
		Matcher m = p.matcher(data);
		while (m.find()) {
			String curLink = m.group(2);
			if (getLog().isDebugEnabled()) {
				for (int i = 0; i < m.groupCount(); ++i) {
					getLog().debug("group " + i + ": " + m.group(i));
				}
			}
			if (isExcluded(curLink)) {
				getLog().info("resource excluded: " + curLink);
				m.appendReplacement(outputFileData, "$1" + curLink + "$3");
				continue;
			}
			int queryIndex = curLink.indexOf('?');
			String query = "";
			if (queryIndex != -1) {
				query = curLink.substring(queryIndex);
				curLink = curLink.substring(0, queryIndex);
			} else {
				queryIndex = curLink.indexOf('#');
				if (queryIndex != -1) {
					query = curLink.substring(queryIndex);
					curLink = curLink.substring(0, queryIndex);
				}
			}

			String targetPath = sourceToFingerprintedTarget.get(curLink);
			if (targetPath == null) {
				File curLinkFile = new File(sourceDirectory, curLink);
				if (!curLinkFile.exists()) {
					getLog().warn("resource file doesn't exist: " + curLink + " found in: " + sourceOfData);
					// escape dollar sign in result output
					curLink = DOLLAR_SIGN.matcher(curLink).replaceAll("\\\\\\$");
					m.appendReplacement(outputFileData, "$1" + curLink + "$3");
					continue;
				}
				logIfRelativePath(curLink);
				targetPath = generateTargetResourceFilename(curLinkFile, curLink, namePattern);
				logIfRelativePath(targetPath);

				sourceToFingerprintedTarget.put(curLink, targetPath);
			}

			String targetURL;
			if (cdn == null) {
				targetURL = "$1" + targetPath + query + "$3";
			} else {
				targetURL = "$1" + cdn + targetPath + query + "$3";
			}

			m.appendReplacement(outputFileData, targetURL);
		}
		m.appendTail(outputFileData);
		return outputFileData;
	}

	private void logIfRelativePath(String path) {
		if (path.length() != 0 && path.charAt(0) != '/') {
			getLog().warn("relative path detected: " + path);
		}
	}

	private boolean isExcluded(String path) {
		if (excludeResources == null) {
			return false;
		}
		for (String curExclude : excludeResources) {
			if (path.contains(curExclude)) {
				return true;
			}
		}
		return false;
	}

	static String generateTargetResourceFilename(File sourceFile, String sourceFilename, String namePattern) throws MojoExecutionException {
		String fingerprint;
		try (FileInputStream fis = new FileInputStream(sourceFile)) {
			fingerprint = DigestUtils.md5Hex(fis);
		} catch (Exception e1) {
			throw new MojoExecutionException("unable to calculate md5 for file: " + sourceFile.getAbsolutePath(), e1);
		}
		String filename = FilenameUtils.getBaseName(sourceFilename);
		String extension = FilenameUtils.getExtension(sourceFilename);

		Map<String, String> values = new HashMap<>();
		values.put("name", filename);
		values.put("hash", fingerprint);
		values.put("ext", extension);

		StringSubstitutor sub = new StringSubstitutor(values, "[", "]");
		return sub.replace(namePattern);
	}

	static String stripSourceDirectory(File sourceDirectory, File file) {
		return file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
	}

	private void mkDirs(File curSrcDirectory, File curDestDirectory) {
		if (!curSrcDirectory.isDirectory()) {
			return;
		}
		File[] subFiles = curSrcDirectory.listFiles();
		for (File curFile : subFiles) {
			if (!curFile.isDirectory()) {
				continue;
			}
			File newDir = new File(curDestDirectory, curFile.getName());
			if (!newDir.exists() && !newDir.mkdirs()) {
				getLog().warn("unable to create directory in outputDirectory: " + newDir);
				continue;
			}
			mkDirs(curFile, newDir);
		}
	}

	private void copyDeepFiles(File srcDir, File dstDir) throws MojoExecutionException {
		File[] srcFiles = srcDir.listFiles();
		for (File curFile : srcFiles) {
			if (curFile.isDirectory()) {
				copyDeepFiles(curFile, new File(dstDir, curFile.getName()));
				continue;
			}

			if (processedFiles.contains(curFile.getAbsolutePath())) {
				continue;
			}

			try (FileInputStream fis = new FileInputStream(curFile); FileOutputStream fos = new FileOutputStream(new File(dstDir, curFile.getName()))) {
				IOUtils.copy(fis, fos);
			} catch (IOException e) {
				throw new MojoExecutionException("unable to copy", e);
			}
		}
	}

	private static String readFile(File file) throws MojoExecutionException {
		try (FileReader r = new FileReader(file)) {
			return IOUtils.toString(r);
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read file: " + file.getAbsolutePath(), e);
		}
	}

	private void findFilesToOptimize(List<File> output, File source) {
		if (!source.isDirectory()) {
			return;
		}
		File[] subFiles = source.listFiles();
		for (File curFile : subFiles) {
			if (curFile.isDirectory()) {
				findFilesToOptimize(output, curFile);
				continue;
			}

			if (!curFile.isFile()) {
				continue;
			}

			String extension = getExtension(curFile.getName());
			if (extension == null) {
				continue;
			}

			if (extensionsToFilter.contains(extension)) {
				output.add(curFile);
				continue;
			}
		}
	}

	private static String getExtension(String filename) {
		int extensionIndex = filename.lastIndexOf('.');
		if (extensionIndex == -1) {
			return null;
		}
		return filename.substring(extensionIndex + 1);
	}

}
