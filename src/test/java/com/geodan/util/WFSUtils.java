/**
 * Solr DataImportHandler for WFS services
 * 
 * Copyright 2013 Jan Boonen (jan.boonen@geodan.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
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
