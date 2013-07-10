package com.st.plugin.fingerprint;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

	@Test
	public void testPattern() throws Exception {
		System.out.println("Patterns Test");

		String imageUrl = "<img src=\"/images/favicon-whatever.ico\" />";

		Pattern pattern = fingerprintMojo.IMG_PATTERN;
		Matcher matcher = pattern.matcher(imageUrl);

		assertTrue(matcher.find());
	}
}
