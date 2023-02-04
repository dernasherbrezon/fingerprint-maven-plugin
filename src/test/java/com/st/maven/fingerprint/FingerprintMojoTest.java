package com.st.maven.fingerprint;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
