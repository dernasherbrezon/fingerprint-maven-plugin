package com.st.maven.fingerprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class FingerprintMojoTest {

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
		Pattern cssPattern = FingerprintMojo.CSS_IMG_PATTERN;
		// Double quotes url, absolute location
		String cssUrl1 = "url(\"/images/navigation-s66728e073e.png\")";
		Matcher cssMatcher1 = cssPattern.matcher(cssUrl1);
		assertTrue(cssMatcher1.find());

		// Single quotes url, absolute location
		String cssUrl2 = "url('/images/navigation-s66728e073e.png')";
		Matcher cssMatcher2 = cssPattern.matcher(cssUrl2);
		assertTrue(cssMatcher2.find());

		// Double quotes url, relative location
		String cssUrl3 = "url(\"../images/navigation-s66728e073e.png\")";
		Matcher cssMatcher3 = cssPattern.matcher(cssUrl3);
		assertTrue(cssMatcher3.find());

		// Double quotes url, relative location
		String cssUrl4 = "url('../images/navigation-s66728e073e.png')";
		Matcher cssMatcher4 = cssPattern.matcher(cssUrl4);
		assertTrue(cssMatcher4.find());

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
	}

	@Test
	public void testGenerateTargetFilename() throws Exception {
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		File sourceDirectory = new File("src/test/resources/");
		String targetHtmlFilename = FingerprintMojo.stripSourceDirectory(sourceDirectory, file);
		assertEquals(String.format("%sdummy-file-for-testing.txt", File.separator) , targetHtmlFilename);
	}

	@Test
	public void testGenerateFilenameWithDefaultPattern() throws Exception {
		String defaultPattern ="[hash][name].[ext]";
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
}
