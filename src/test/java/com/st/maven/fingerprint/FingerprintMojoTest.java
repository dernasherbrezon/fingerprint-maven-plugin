package com.st.maven.fingerprint;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FingerprintMojoTest {

	@Rule
	public MojoRule mrule = new MojoRule();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testPattern() throws Exception {
		Pattern linkPattern = FingerprintMojo.LINK_PATTERN;
		String linkUrl = "<link rel=\"stylesheet\" href=\"${pageContext.request.contextPath}/resources/css/style.css\" />";
		Matcher linkMatcher = linkPattern.matcher(linkUrl);
		assertTrue(linkMatcher.find());

		String multilineLinkUrl = "<link\n    rel=\"stylesheet\"\n    href=\"${pageContext.request.contextPath}/resources/css/style.css\" />";
		Matcher multilineLinkMatcher = linkPattern.matcher(multilineLinkUrl);
		assertTrue(multilineLinkMatcher.find());

		Pattern scriptPattern = FingerprintMojo.SCRIPT_PATTERN;
		String scriptUrl = "<script src=\"${pageContext.request.contextPath}/resources/js/vendor/zepto.js\">";
		Matcher scriptMatcher = scriptPattern.matcher(scriptUrl);
		assertTrue(scriptMatcher.find());

		Pattern imgPattern = FingerprintMojo.IMG_PATTERN;
		String imageUrl1 = "<img src=\"${pageContext.request.contextPath}/images/favicon-whatever.ico\" />";
		Matcher imgMatcher1 = imgPattern.matcher(imageUrl1);
		assertFalse(imgMatcher1.find());

		String imageUrl2 = "<img src=\"/images/photo.jpg\" />";
		Matcher imgMatcher2 = imgPattern.matcher(imageUrl2);
		assertTrue(imgMatcher2.find());

		String imageUrl3 = "<img\n    src=\"/images/photo.jpg\"\n    />";
		Matcher imgMatcher3 = imgPattern.matcher(imageUrl3);
		assertTrue(imgMatcher3.find());

		// Tests for the CSS image references
		Pattern cssPattern = FingerprintMojo.CSS_URL_PATTERN;
		// Double-quoted url, absolute location
		String cssUrl1 = "url( \"/images/navigation-s66728e073e.png\" )";
		Matcher cssMatcher1 = cssPattern.matcher(cssUrl1);
		assertTrue(cssMatcher1.find());

		// Single-quoted url, absolute location
		String cssUrl2 = "url( '/images/navigation-s66728e073e.png' )";
		Matcher cssMatcher2 = cssPattern.matcher(cssUrl2);
		assertTrue(cssMatcher2.find());

		// Unquoted url, absolute location
		String cssUrl3 = "url( /images/navigation-s66728e073e.png )";
		Matcher cssMatcher3 = cssPattern.matcher(cssUrl3);
		assertTrue(cssMatcher3.find());

		// Double-quoted url, relative location
		String cssUrl4 = "url( \"../images/navigation-s66728e073e.png\" )";
		Matcher cssMatcher4 = cssPattern.matcher(cssUrl4);
		assertTrue(cssMatcher4.find());

		// Single-quoted url, relative location
		String cssUrl5 = "url( '../images/navigation-s66728e073e.png' )";
		Matcher cssMatcher5 = cssPattern.matcher(cssUrl5);
		assertTrue(cssMatcher5.find());

		// Unquoted url, relative location
		String cssUrl6 = "url( ../images/navigation-s66728e073e.png )";
		Matcher cssMatcher6 = cssPattern.matcher(cssUrl6);
		assertTrue(cssMatcher6.find());

		// JSTL url, absolute
		Pattern jstlUrlPattern = FingerprintMojo.JSTL_URL_PATTERN;
		String jstlUrl1 = "<c:url value=\"/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
		Matcher jstlUrlMatcher1 = jstlUrlPattern.matcher(jstlUrl1);
		assertTrue(jstlUrlMatcher1.find());

		// JSTL url, with context root
		String jstlUrl2 = "<c:url value=\"${pageContext.request.contextPath}/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
		Matcher jstlUrlMatcher2 = jstlUrlPattern.matcher(jstlUrl2);
		assertFalse(jstlUrlMatcher2.find());

		// JSTL url, href
		String jstlUrl3 = "<c:url value=\"http://www.fedex.com/Tracking?ascend_header=1&amp;clienttype=dotcom&amp;cntry_code=us&amp;language=english&amp;tracknumbers=${shipment.trackingNumber}\" var=\"fedexUrl\"/>";
		Matcher jstlUrlMatcher3 = jstlUrlPattern.matcher(jstlUrl3);
		assertFalse(jstlUrlMatcher3.find());

		// Multiline JSTL url, absolute
		String jstlUrl4 = "<c:url\n    value=\"/resources/images/favicon.ico\"\n    var=\"faviconUrl\"\n    />";
		Matcher jstlUrlMatcher4 = jstlUrlPattern.matcher(jstlUrl4);
		assertTrue(jstlUrlMatcher4.find());

	}

	@Test
	public void testGenerateTargetFilename() throws Exception {
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		File sourceDirectory = new File("src/test/resources/");
		String targetHtmlFilename = FingerprintMojo.stripSourceDirectory(sourceDirectory, file);
		assertEquals(String.format("%sdummy-file-for-testing.txt", File.separator), targetHtmlFilename);
	}

	@Test
	public void testGenerateFilenameWithDefaultPattern() throws Exception {
		String defaultPattern = "[hash][name].[ext]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = FingerprintMojo.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", defaultPattern);
		assertEquals(resultFilename, "331afe01c54815562adc514c6b5eb561dummy-file-for-testing.txt");
	}

	@Test
	public void testGenerateFilenameWithCustomPattern() throws Exception {
		String namePattern = "[hash].[name].[ext]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = FingerprintMojo.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals(resultFilename, "331afe01c54815562adc514c6b5eb561.dummy-file-for-testing.txt");
	}

	@Test
	public void testGenerateFilenameWithAnotherCustomPattern() throws Exception {
		String namePattern = "[name].[ext]?hash=[hash]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = FingerprintMojo.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals(resultFilename, "dummy-file-for-testing.txt?hash=331afe01c54815562adc514c6b5eb561");
	}

	@Test
	public void testGenerateFilenameWithUnsupportedParameterInCustomPattern() throws Exception {
		String namePattern = "[name].[hash].[ext][sv]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = FingerprintMojo.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals(resultFilename, "dummy-file-for-testing.331afe01c54815562adc514c6b5eb561.txt[sv]");
	}

	@Test
	public void testSuccess() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "generate");
		mm.execute();
		assertFiles(new File("src/test/resources/expectedSuccess"), new File(folder.getRoot(), "optimized-webapp"));
	}

	private static void assertFiles(File expected, File actual) {
		if (expected.isFile()) {
			assertBinaryFilesEqual(expected, actual);
		}
		if (expected.isDirectory()) {
			assertDirectory(expected, actual);
		}
	}

	private static void assertBinaryFilesEqual(File expected, File actual) {
		try (InputStream is = new BufferedInputStream(new FileInputStream(expected)); InputStream ais = new BufferedInputStream(new FileInputStream(actual))) {
			byte[] expectedBody = IOUtils.toByteArray(is);
			byte[] actualBody = IOUtils.toByteArray(ais);
			assertArrayEquals(expectedBody, actualBody);
		} catch (IOException e) {
			fail("unable to read file: " + e.getMessage());
		}
	}

	private static void assertDirectory(File expected, File actual) {
		assertEquals(expected.isDirectory(), actual.isDirectory());
		File[] expectedFiles = expected.listFiles();
		File[] actualFiles = actual.listFiles();
		assertEquals(expectedFiles.length, actualFiles.length);
		for (int i = 0; i < expectedFiles.length; i++) {
			assertFiles(expectedFiles[i], actualFiles[i]);
		}
	}

	private MavenProject loadSuccessProject() throws Exception {
		File basedir = new File("src/test/resources/success");
		MavenProject mavenProject = mrule.readMavenProject(basedir);
		mavenProject.getBuild().setDirectory(folder.getRoot().getAbsolutePath());
		return mavenProject;
	}
}
