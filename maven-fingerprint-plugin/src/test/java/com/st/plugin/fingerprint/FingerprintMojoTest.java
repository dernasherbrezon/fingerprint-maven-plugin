package com.st.plugin.fingerprint;

import org.junit.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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
		String linkUrl = "<link rel=\"stylesheet\" href=\"${pageContext.request.contextPath}/resources/css/style.css\" />";
		Matcher linkMatcher = linkPattern.matcher(linkUrl);
		assertTrue(linkMatcher.find());

		Pattern scriptPattern = fingerprintMojo.SCRIPT_PATTERN;
		String scriptUrl = "<script src=\"${pageContext.request.contextPath}/resources/js/vendor/zepto.js\">";
		Matcher scriptMatcher = scriptPattern.matcher(scriptUrl);
		assertTrue(scriptMatcher.find());

		Pattern imgPattern = fingerprintMojo.IMG_PATTERN;
		String imageUrl = "<img src=\"${pageContext.request.contextPath}/images/favicon-whatever.ico\" />";
		Matcher imgMatcher = imgPattern.matcher(imageUrl);
		assertTrue(imgMatcher.find());

		// Tests for the CSS image references
		Pattern cssPattern = fingerprintMojo.CSS_IMG_PATTERN;
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
		Pattern jstlUrlPattern = fingerprintMojo.JSTL_URL_PATTERN;
		String jstlUrl1 = "<c:url value=\"/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
		Matcher jstlUrlMatcher1 = jstlUrlPattern.matcher(jstlUrl1);
		assertTrue(jstlUrlMatcher1.find());

		// JSTL url, with context root
		String jstlUrl2 = "<c:url value=\"${pageContext.request.contextPath}/resources/images/favicon.ico\" var=\"faviconUrl\"/>";
		Matcher jstlUrlMatcher2 = jstlUrlPattern.matcher(jstlUrl2);
		assertTrue(jstlUrlMatcher2.find());

		// JSTL url, href
		String jstlUrl3 = "<c:url value=\"http://www.fedex.com/Tracking?ascend_header=1&amp;clienttype=dotcom&amp;cntry_code=us&amp;language=english&amp;tracknumbers=${shipment.trackingNumber}\" var=\"fedexUrl\"/>";
		Matcher jstlUrlMatcher3 = jstlUrlPattern.matcher(jstlUrl3);
		assertFalse(jstlUrlMatcher3.find());
	}
}
