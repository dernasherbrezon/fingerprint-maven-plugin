package com.st.maven.fingerprint;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class FingerprintMojo extends AbstractMojo {

	/*
	 * All resources should have absolute paths: Valid: <img
	 * src="/img/test.png"> . Invalid: <img src="test.png"> All resources should
	 * point to existing files without any pre-processing: Valid: <img
	 * src="/img/test.png"> . Invalid: <img
	 * src="<c:if test="${var}">/img/test.png</c:if>"
	 */
	public Pattern LINK_PATTERN = Pattern.compile("(<link.*?href=\")(.*?)(\".*?>)");
	public Pattern SCRIPT_PATTERN = Pattern.compile("(\")([^\\s]*?\\.js)(\")");
	public Pattern IMG_PATTERN = Pattern.compile("(<img.*?src=\")([^\\}\\{]*?)(\".*?>)");
	public Pattern CSS_IMG_PATTERN = Pattern.compile("(url\\([\",'])(.*?)([\",']\\))");
	public Pattern JSTL_URL_PATTERN = Pattern.compile("(<c:url.*?value=\")(/{1}.*?)(\".*?>)");

	/**
	 * Output directory
	 */
	@Parameter(defaultValue = "${project.build.directory}/optimized-webapp", required = true)
	private File outputDirectory;

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
	private Set<String> htmlExtensions;

	/**
	 * CDN url
	 */
	@Parameter
	private String cdn;

	private final Map<String, String> processedFiles = new HashMap<String, String>();

	@Override
	public void execute() throws MojoExecutionException {
		if (!sourceDirectory.isDirectory()) {
			throw new MojoExecutionException("source directory is not a directory: " + sourceDirectory.getAbsolutePath());
		}
		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new MojoExecutionException("unable to create outputdirectory: " + outputDirectory.getAbsolutePath());
			}
		}
		if (!outputDirectory.isDirectory()) {
			throw new MojoExecutionException("output directory is not a directory: " + outputDirectory.getAbsolutePath());
		}
		List<File> pagesToFilter = new ArrayList<File>();
		findPagesToFilter(pagesToFilter, sourceDirectory);
		if (pagesToFilter.isEmpty()) {
			return;
		}

		copyDirectories(sourceDirectory, outputDirectory);

		for (File curHTml : pagesToFilter) {
			try {
				processPage(curHTml);
			} catch (Exception e) {
				getLog().error("unable to process: " + curHTml.getAbsolutePath(), e);
				throw new MojoExecutionException("unable to process: " + curHTml.getAbsolutePath(), e);
			}
		}

		try {
			copyDeepFiles(sourceDirectory, outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("unable to deep copy files", e);
		}
	}

	private void processPage(File file) throws MojoExecutionException {
		File fileToProcess;
		if (processedFiles.containsKey(file.getAbsolutePath())) {
			fileToProcess = new File(processedFiles.get(file.getAbsolutePath()));
		} else {
			fileToProcess = file;
		}
		if (getLog().isDebugEnabled()) {
			getLog().debug("processing file: " + fileToProcess.getAbsolutePath());
		}
		String data = readFile(fileToProcess);
		StringBuffer outputFileData = new StringBuffer(data);
		outputFileData = processPattern(LINK_PATTERN, outputFileData.toString(), fileToProcess.getAbsolutePath());
		outputFileData = processPattern(SCRIPT_PATTERN, outputFileData.toString(), fileToProcess.getAbsolutePath());
		outputFileData = processPattern(IMG_PATTERN, outputFileData.toString(), fileToProcess.getAbsolutePath());
		outputFileData = processPattern(CSS_IMG_PATTERN, outputFileData.toString(), fileToProcess.getAbsolutePath());
		outputFileData = processPattern(JSTL_URL_PATTERN, outputFileData.toString(), fileToProcess.getAbsolutePath());
		String processedData = null;
		if (htmlExtensions != null && !htmlExtensions.isEmpty()) {
			String extension = getExtension(fileToProcess.getName());
			if (extension != null && htmlExtensions.contains(extension)) {
				getLog().info("minify html: " + fileToProcess.getAbsolutePath());
				processedData = HtmlMinifier.minify(outputFileData.toString());
			}
		} else if (fileToProcess.getName().contains(".min.")) {
			getLog().info("ignoring already minified resource: " + fileToProcess.getAbsolutePath());
		} else if (fileToProcess.getName().endsWith(".js")) {
			processedData = outputFileData.toString();
			getLog().info("minifying javascript: " + fileToProcess.getAbsolutePath());
			processedData = Compressor.compressJavaScript(new StringReader(processedData), getLog());
		} else if (fileToProcess.getName().endsWith(".css")) {
			processedData = outputFileData.toString();
			getLog().info("minifying css: " + fileToProcess.getAbsolutePath());
			processedData = Compressor.compressCSS(new StringReader(processedData), getLog());
		}

		if (processedData == null) {
			processedData = outputFileData.toString();
		}

		FileWriter w = null;
		File targetFile;
		if (!processedFiles.containsKey(file.getAbsolutePath())) {
			String targetHtmlFilename = generateTargetFilename(sourceDirectory, fileToProcess);
			targetFile = new File(outputDirectory, targetHtmlFilename);
		} else {
			targetFile = fileToProcess;
		}
		try {
			w = new FileWriter(targetFile);
			w.append(processedData);
			w.flush();
		} catch (IOException e) {
			throw new MojoExecutionException("unable to write html file: " + targetFile.getAbsolutePath(), e);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor", e);
				}
			}
		}
		if (!processedFiles.containsKey(file.getAbsolutePath())) {
			processedFiles.put(file.getAbsolutePath(), targetFile.getAbsolutePath());
		}
	}

	private StringBuffer processPattern(Pattern p, String data, String sourceOfData) throws MojoExecutionException {
		StringBuffer outputFileData = new StringBuffer();
		Matcher m = p.matcher(data);
		while (m.find()) {
			String curLink = m.group(2);
			for (int i = 0; i < m.groupCount(); ++i) {
				getLog().debug("group " + i + ": " + m.group(i));
			}
			if (isExcluded(curLink)) {
				getLog().info("resource excluded: " + curLink);
				m.appendReplacement(outputFileData, "$1" + curLink + "$3");
				continue;
			}
			int queryIndex = curLink.indexOf("?");
			String query = "";
			if (queryIndex != -1) {
				query = curLink.substring(queryIndex);
				curLink = curLink.substring(0, queryIndex);
			} else {
				queryIndex = curLink.indexOf("#");
				if (queryIndex != -1) {
					query = curLink.substring(queryIndex);
					curLink = curLink.substring(0, queryIndex);
				}
			}

			File linkFile = new File(sourceDirectory, curLink);
			if (!linkFile.exists()) {
				getLog().warn("resource file doesnt exist: " + curLink + " found in: " + sourceOfData);
				curLink = curLink.replaceAll("\\$", "\\\\\\$");
				m.appendReplacement(outputFileData, "$1" + curLink + "$3");
				continue;
			}
			if (curLink.length() > 0 && curLink.charAt(0) != '/') {
				getLog().warn("resource has relative path: " + curLink);
			}
			String fingerprint = generateFingerprint(readBinaryFile(linkFile));
			String targetPath = generateTargetResourceFilename(fingerprint, curLink);
			if (targetPath.length() != 0 && targetPath.charAt(0) != '/') {
				getLog().warn("relative path detected: " + curLink);
			}

			String targetURL;
			if (cdn == null) {
				targetURL = "$1" + targetPath + query + "$3";
			} else {
				targetURL = "$1" + cdn + targetPath + query + "$3";
			}

			m.appendReplacement(outputFileData, targetURL);
			File targetFilename = new File(outputDirectory, targetPath);
			if (targetFilename.exists()) {
				if (getLog().isDebugEnabled()) {
					getLog().debug("target file already moved: " + linkFile.getAbsolutePath());
				}
				continue;
			}
			getLog().info("move fingerprinted resource: " + linkFile.getAbsolutePath() + " to: " + targetFilename.getAbsolutePath());
			if (processedFiles.containsKey(linkFile.getAbsolutePath())) {
				String pathWithinSource = linkFile.getAbsolutePath();
				linkFile = new File(processedFiles.get(pathWithinSource));
				processedFiles.put(pathWithinSource, targetFilename.getAbsolutePath());
				try {
					copy(new FileInputStream(linkFile), new FileOutputStream(targetFilename), 2048);
				} catch (Exception e) {
					throw new MojoExecutionException("unable to copy resource file: " + linkFile + " to: " + targetFilename, e);
				}
				if (!linkFile.delete()) {
					getLog().warn("unable to move " + linkFile.getAbsolutePath());
				}
			} else {
				processedFiles.put(linkFile.getAbsolutePath(), targetFilename.getAbsolutePath());
				try {
					copy(new FileInputStream(linkFile), new FileOutputStream(targetFilename), 2048);
				} catch (Exception e) {
					throw new MojoExecutionException("unable to copy resource file: " + linkFile + " to: " + targetFilename, e);
				}
			}
		}
		m.appendTail(outputFileData);
		return outputFileData;
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

	private static String generateFingerprint(byte[] data) throws MojoExecutionException {
		MessageDigest md5Alg;
		try {
			md5Alg = MessageDigest.getInstance("MD5");
			md5Alg.reset();
			byte[] digest = md5Alg.digest(data);
			BigInteger result = new BigInteger(digest);
			String resultStr = null;
			if (result.signum() < 0) {
				resultStr = result.negate().toString(16);
			} else {
				resultStr = result.toString(16);
			}
			return resultStr;
		} catch (NoSuchAlgorithmException e) {
			throw new MojoExecutionException("unable to generate fingerprint", e);
		}
	}

	static String generateTargetResourceFilename(String fingerprint, String sourceFilename) {
		int index = sourceFilename.lastIndexOf("/");
		if (index == -1) {
			return fingerprint + sourceFilename;
		}
		String filename = sourceFilename.substring(index + 1);
		return sourceFilename.substring(0, index) + "/" + fingerprint + filename;
	}

	static String generateTargetFilename(File sourceDirectory, File file) {
		return file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
	}

	private void copyDirectories(File curSrcDirectory, File curDestDirectory) {
		if (!curSrcDirectory.isDirectory()) {
			return;
		}
		File[] subFiles = curSrcDirectory.listFiles();
		for (File curFile : subFiles) {
			if (!curFile.isDirectory()) {
				continue;
			}
			File newDir = new File(curDestDirectory, curFile.getName());
			if (!newDir.exists()) {
				if (!newDir.mkdirs()) {
					getLog().warn("unable to create directory in outputDirectory: " + newDir);
					continue;
				}
			}
			copyDirectories(curFile, newDir);
		}
	}

	private void copyDeepFiles(File srcDir, File dstDir) throws IOException {
		File[] srcFiles = srcDir.listFiles();
		for (File curFile : srcFiles) {
			if (curFile.isDirectory()) {
				copyDeepFiles(curFile, new File(dstDir, curFile.getName()));
				continue;
			}

			if (processedFiles.containsKey(curFile.getAbsolutePath())) {
				continue;
			}

			copy(new FileInputStream(curFile), new FileOutputStream(new File(dstDir, curFile.getName())), 2048);
		}
	}

	private String readFile(File file) throws MojoExecutionException {
		BufferedReader r = null;
		String curLine = null;
		StringBuilder builder = new StringBuilder();
		try {
			r = new BufferedReader(new FileReader(file));
			while ((curLine = r.readLine()) != null) {
				builder.append(curLine);
				builder.append("\n");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read file: " + file, e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor: " + file, e);
				}
			}
		}
		return builder.toString();
	}

	private byte[] readBinaryFile(File f) throws MojoExecutionException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			byte[] buf = new byte[2048];
			int bytesRead = -1;
			while ((bytesRead = fis.read(buf)) != -1) {
				baos.write(buf, 0, bytesRead);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read file: " + f, e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor: " + f, e);
				}
			}
		}
	}

	private static void copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		int n;
		while (-1 != (n = inputStream.read(buffer))) {
			outputStream.write(buffer, 0, n);
		}
		inputStream.close();
		outputStream.close();
	}

	public void findPagesToFilter(List<File> output, File source) {
		if (!source.isDirectory()) {
			return;
		}
		File[] subFiles = source.listFiles();
		for (File curFile : subFiles) {
			if (curFile.isDirectory()) {
				findPagesToFilter(output, curFile);
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

	public static String getExtension(String filename) {
		int extensionIndex = filename.lastIndexOf(".");
		if (extensionIndex == -1) {
			return null;
		}
		String extension = filename.substring(extensionIndex + 1);
		return extension;
	}

}
