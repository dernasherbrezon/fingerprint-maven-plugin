package com.st.maven.fingerprint;

public class Util {

	public static String getExtension(String filename) {
		int extensionIndex = filename.lastIndexOf('.');
		if (extensionIndex == -1) {
			return null;
		}
		return filename.substring(extensionIndex + 1);
	}

}
