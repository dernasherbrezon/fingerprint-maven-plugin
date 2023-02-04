package com.st.maven.fingerprint;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileComparator implements Comparator<File> {

	private final Map<String, Integer> extensionToPriority = new HashMap<>();
	private static final int UNKNOWN_EXTENSION_PRIORITY = 255;

	public FileComparator(Set<String> htmlExtensions) {
		for (String cur : htmlExtensions) {
			extensionToPriority.put(cur, 0);
		}
		extensionToPriority.put("css", 1);
		extensionToPriority.put("js", 2);
	}

	@Override
	public int compare(File o1, File o2) {
		String ext1 = Util.getExtension(o1.getName());
		String ext2 = Util.getExtension(o2.getName());
		Integer ext1Priority = extensionToPriority.get(ext1);
		if (ext1Priority == null) {
			ext1Priority = UNKNOWN_EXTENSION_PRIORITY;
		}
		Integer ext2Priority = extensionToPriority.get(ext2);
		if (ext2Priority == null) {
			ext2Priority = UNKNOWN_EXTENSION_PRIORITY;
		}
		return ext2Priority.compareTo(ext1Priority);
	}
}
