package com.st.maven.fingerprint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilTest {

	@Test
	public void testGetExtension() {
		assertEquals("css", Util.getExtension("file.css"));
	}

}
