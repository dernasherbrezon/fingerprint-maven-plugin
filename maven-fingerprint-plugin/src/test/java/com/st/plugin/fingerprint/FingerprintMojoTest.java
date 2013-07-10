package com.st.plugin.fingerprint;

import org.junit.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * @author Richard Scott Smith <scott.smith@isostech.com>
 */
public class FingerprintMojoTest {
	FingerprintMojo fingerprintMojo;

	@BeforeClass
	public static void setUpClass() throws Exception {

	}

	@AfterClass
	public static void tearDownClass() throws Exception {

	}

	@Before
	public void setUp() throws Exception {
		fingerprintMojo = new FingerprintMojo();
	}

	@After
	public void tearDown() throws Exception {

	}

	/**
	 * Testing of the various regex patterns.
	 * @throws Exception
	 */
	@Test
	public void testPattern() throws Exception {
		System.out.println("Patterns Test");

		Pattern linkPattern = fingerprintMojo.LINK_PATTERN;
		String linkUrl = "<link rel=\"stylesheet\" href=\"${basePath}/resources/css/style.css\" />";
		Matcher linkMatcher = linkPattern.matcher(linkUrl);
		assertTrue(linkMatcher.find());

		Pattern scriptPattern = fingerprintMojo.SCRIPT_PATTERN;
		String scriptUrl = "<script src=\"${basePath}/resources/js/vendor/zepto.js\">";
		Matcher scriptMatcher = scriptPattern.matcher(scriptUrl);
		assertTrue(scriptMatcher.find());

		Pattern imgPattern = fingerprintMojo.IMG_PATTERN;
		String imageUrl = "<img src=\"/images/favicon-whatever.ico\" />";
		Matcher imgMatcher = imgPattern.matcher(imageUrl);
		assertTrue(imgMatcher.find());

		Pattern cssPattern = fingerprintMojo.CSS_IMG_PATTERN;
		String cssUrl1 = "url(\"/images/navigation-s66728e073e.png\")";
		Matcher cssMatcher1 = cssPattern.matcher(cssUrl1);
		assertTrue(cssMatcher1.find());
		String cssUrl2 = "url('/images/navigation-s66728e073e.png')";
		Matcher cssMatcher2 = cssPattern.matcher(cssUrl2);
		assertTrue(cssMatcher2.find());

	}
}
