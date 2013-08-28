package com.geodan.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public final class WFSUtils {

	private WFSUtils() {
		// Hide constructor
	}

	public static final String convertParams(Map<String, Object> params) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> entry : params.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append("&");
		}

		return sb.toString();
	}

	public static final String loadResponseFromClasspath(String path) {
		String result = "";
		InputStream is = null;
		Scanner scanner = null;
		try {
			is = WFSUtils.class.getResourceAsStream(path);
			scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
			if (scanner.hasNext()) {
				result = scanner.next();
			}
		} finally {
			scanner.close();
			try {
				is.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
		return result;
	}

}
