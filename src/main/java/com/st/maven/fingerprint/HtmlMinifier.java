package com.st.maven.fingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlMinifier {

	private final static Pattern newLineSymbols = Pattern.compile("[\n\t\r]");
	private final static Pattern tagWhiteSpaces = Pattern.compile(">(\\s+)<");
	private final static String PRE_START_TAG = "<pre";
	private final static String PRE_END_TAG = "</pre>";
	private final static Pattern DEFAULT_INPUT_TYPE = Pattern.compile("(<input.*?) type=\"text\"");

	static String minify(String page) {
		Matcher m = DEFAULT_INPUT_TYPE.matcher(page);
		
		StringBuffer sb = new StringBuffer();
		while( m.find() ) {
			m.appendReplacement(sb, "$1");
		}
		m.appendTail(sb);
		page = sb.toString();
		if (!page.contains(PRE_START_TAG)) {
			return fix(page);
		}
		
		StringBuilder b = new StringBuilder();
		List<String> parts = getParts(0, page);
		for (String curPart : parts) {
			if (curPart.contains(PRE_START_TAG)) {
				b.append(curPart);
			} else {
				b.append(fix(curPart));
			}
		}
		return b.toString();
	}

	private static String fix(String original) {
		String page = newLineSymbols.matcher(original).replaceAll("");
		String result = tagWhiteSpaces.matcher(page).replaceAll("><");
		return result.trim();
	}

	private static List<String> getParts(int curStart, String original) {
		List<String> result = new ArrayList<String>();
		int start = original.indexOf(PRE_START_TAG, curStart);
		if (start == -1) {
			result.add(original.substring(curStart));
			return result;
		}
		int end = original.indexOf(PRE_END_TAG, start);
		int newStart = end + PRE_END_TAG.length();
		result.add(original.substring(curStart, start));
		result.add(original.substring(start, newStart));
		List<String> subs = getParts(newStart, original);
		result.addAll(subs);
		return result;
	}

}
