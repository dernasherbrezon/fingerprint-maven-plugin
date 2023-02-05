package com.st.maven.fingerprint;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class UtilTest {

	@Test
	public void testGetExtension() {
		assertEquals("css", Util.getExtension("file.css"));
	}

	@Test
	public void testGenerateTargetFilename() throws Exception {
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		File sourceDirectory = new File("src/test/resources/");
		String targetHtmlFilename = Util.stripSourceDirectory(sourceDirectory, file);
		assertEquals(String.format("%sdummy-file-for-testing.txt", File.separator), targetHtmlFilename);
	}

	@Test
	public void testGenerateFilenameWithDefaultPattern() throws Exception {
		String defaultPattern = "[hash][name].[ext]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = Util.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", defaultPattern);
		assertEquals("331afe01c54815562adc514c6b5eb561dummy-file-for-testing.txt", resultFilename);
	}

	@Test
	public void testGenerateFilenameWithCustomPattern() throws Exception {
		String namePattern = "[hash].[name].[ext]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = Util.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals("331afe01c54815562adc514c6b5eb561.dummy-file-for-testing.txt", resultFilename);
	}

	@Test
	public void testGenerateFilenameWithAnotherCustomPattern() throws Exception {
		String namePattern = "[name].[ext]?hash=[hash]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = Util.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals("dummy-file-for-testing.txt?hash=331afe01c54815562adc514c6b5eb561", resultFilename);
	}

	@Test
	public void testGenerateFilenameWithUnsupportedParameterInCustomPattern() throws Exception {
		String namePattern = "[name].[hash].[ext][sv]";
		File file = new File("src/test/resources/dummy-file-for-testing.txt");
		String resultFilename = Util.generateTargetResourceFilename(file, "dummy-file-for-testing.txt", namePattern);
		assertEquals("dummy-file-for-testing.331afe01c54815562adc514c6b5eb561.txt[sv]", resultFilename);
	}
}
