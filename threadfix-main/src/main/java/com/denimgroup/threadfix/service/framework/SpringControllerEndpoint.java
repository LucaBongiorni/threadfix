////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.service.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SpringControllerEndpoint implements Comparable<SpringControllerEndpoint>, EndpointGenerator {
	
	public static final String GENERIC_INT_SEGMENT = "{id}";
	private static final String requestMappingStart = "RequestMethod.";
	
	private final String rawFilePath, rawUrlPath;
	private final Set<String> methods, parameters;
	private final int startLineNumber, endLineNumber;
	
	private String cleanedFilePath = null, cleanedUrlPath = null;
	
	private String fileRoot;
	
	public SpringControllerEndpoint(String filePath, String urlPath,
			Collection<String> methods, Collection<String> parameters,
			int startLineNumber, int endLineNumber) {
		this.rawFilePath     = filePath;
		this.rawUrlPath      = urlPath;
		this.startLineNumber = startLineNumber;
		this.endLineNumber   = endLineNumber;
		
		this.parameters = new HashSet<>(parameters);
		this.methods    = getCleanedSet(methods);
	}
	
	private Set<String> getCleanedSet(Collection<String> methods) {
		Set<String> returnSet = new HashSet<>();
		for (String method : methods) {
			if (method.startsWith(requestMappingStart)) {
				returnSet.add(method.substring(requestMappingStart.length()));
			} else {
				returnSet.add(method);
			}
		}
		return returnSet;
	}
	
	public String getRawFilePath() {
		return rawFilePath;
	}

	public String getRawUrlPath() {
		return rawUrlPath;
	}
	
	public Set<String> getParameters() {
		return parameters;
	}

	public String getCleanedFilePath() {
		if (cleanedFilePath == null && fileRoot != null &&
				rawFilePath != null && rawFilePath.contains(fileRoot)) {
			cleanedFilePath = rawFilePath.substring(fileRoot.length());
		}
		
		return cleanedFilePath;
	}
	
	public void setFileRoot(String fileRoot) {
		this.fileRoot = fileRoot;
	}

	public String getCleanedUrlPath() {
		if (cleanedUrlPath == null) {
			cleanedUrlPath = cleanUrlPathStatic(rawUrlPath);
		}
		
		return cleanedUrlPath;
	}
	
	public static String cleanUrlPathStatic(String rawUrlPath) {
		if (rawUrlPath == null) {
			return null;
		} else {
			return rawUrlPath
					.replaceAll("/\\*/", "/" + GENERIC_INT_SEGMENT + "/")
					.replaceAll("\\{[^\\}]+\\}", GENERIC_INT_SEGMENT);
		}
	}
	
	public static String cleanUrlPathDynamic(String rawUrlPath) {
		if (rawUrlPath == null) {
			return null;
		} else {
			return rawUrlPath
					.replaceAll("/[0-9]+/", "/" + GENERIC_INT_SEGMENT + "/")
					.replaceAll("\\.html", "")
					.replaceAll("/[0-9]+$", "/" + GENERIC_INT_SEGMENT);
		}
	}
	
	public int getStartLineNumber() {
		return startLineNumber;
	}
	
	public boolean matchesLineNumber(int lineNumber) {
		return lineNumber < endLineNumber && lineNumber > startLineNumber;
	}
	
	public boolean matchesMethod(String method) {
		return method != null && methods != null && methods.contains(method.toUpperCase());
	}

	@Override
	public String toString() {
		return "[" + getCleanedFilePath() +
				":" + startLineNumber +
				"-" + endLineNumber +
				" -> " + getMethod() +
				" " + getCleanedUrlPath() +
				" " + getParameters() +
				"]";
	}

	public Set<String> getMethod() {
		return methods;
	}

	@Override
	public int compareTo(SpringControllerEndpoint arg0) {
		int returnValue = 0;
		
		if (arg0 != null) {
			returnValue -= 2 * arg0.rawFilePath.compareTo(rawFilePath);
			if (startLineNumber < arg0.startLineNumber) {
				returnValue -= 1;
			} else {
				returnValue += 1;
			}
		}
		
		return returnValue;
	}

	@Override
	public List<Endpoint> generateEndpoints() {
		Iterable<String> methodsToGenerate;
		
		if (methods == null || methods.isEmpty()) {
			methodsToGenerate = Arrays.asList("GET");
		} else {
			methodsToGenerate = new TreeSet<>(methods);
		}
		
		List<Endpoint> endpoints = new ArrayList<>();
		
		for (String method : methodsToGenerate) {
			endpoints.add(new DefaultEndpoint(cleanedUrlPath, parameters, method));
		}
		
		return endpoints;
	}
	
}
