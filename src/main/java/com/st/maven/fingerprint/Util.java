package com.st.maven.fingerprint;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;

class Util {

	static String getExtension(String filename) {
		int extensionIndex = filename.lastIndexOf('.');
		if (extensionIndex == -1) {
			return null;
		}
		return filename.substring(extensionIndex + 1);
	}
	
	static String stripSourceDirectory(File sourceDirectory, File file) {
		return file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
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
		return FilenameUtils.getFullPath(sourceFilename) + sub.replace(namePattern);
	}
}
