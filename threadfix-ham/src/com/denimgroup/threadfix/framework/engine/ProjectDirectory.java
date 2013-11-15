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
package com.denimgroup.threadfix.framework.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.denimgroup.threadfix.framework.util.FilePathUtils;
import com.denimgroup.threadfix.framework.util.SanitizedLogger;

// TODO make more error resistant
public class ProjectDirectory {
	
	private File directory;
	
	private final Map<String, Set<String>> fileMap;
	
	private final SanitizedLogger log = new SanitizedLogger("ProjectDirectory");
	
	public ProjectDirectory(File directory) {
		this.directory = directory;
		fileMap = buildMaps(directory);
	}
	
	public String getDirectoryPath() {
		return directory.getAbsolutePath();
	}
	
	private Map<String, Set<String>> buildMaps(File startingFile) {
		Map<String, Set<String>> returnMap = new HashMap<>();
		
		if (startingFile != null && startingFile.isDirectory()) {
			recurseMap(startingFile, returnMap);
		}
		
		return returnMap;
	}
	
	private void recurseMap(File currentDirectory, Map<String, Set<String>> map) {
		if (currentDirectory == null || !currentDirectory.isDirectory() || !currentDirectory.exists()) {
			return;
		}
		
    	List<File> directories = new ArrayList<>();
    	
    	for (File file : currentDirectory.listFiles()) {
    		// we want to skip all files / directories that start with .
    		if (file != null && file.getName().charAt(0) != '.') {
	    		if (file.isFile()) {
	    			addToMap(map, file);
	    		} else if (file.isDirectory()) {
	    			directories.add(file);
	    		}
    		}
    	}
    	
    	for (File directory : directories) {
    		recurseMap(directory, map);
    	}
	}
	
	private void addToMap(Map<String, Set<String>> map, File file) {
		if (!map.containsKey(file.getName())) {
			map.put(file.getName(), new HashSet<String>());
		}
		
		String canonicalPath = FilePathUtils.getRelativePath(file, getDirectoryPath());
		
		if (canonicalPath != null) {
			map.get(file.getName()).add(canonicalPath);
		} else {
			log.warn("Recursive algorithm in ProjectDirectory broke out of its starting directory.");
		}
	}
	
	// TODO we may be able to get better results with some more advanced logic here
	// maybe skip directories like "test", look in specific paths or at least check guesses
	// on the other hand I don't really see this being a bottleneck
	public File findWebXML() {
		return findFile("web.xml", "WEB-INF", "web.xml");
	}
	
	public List<File> findFiles(String pathWithStars) {
		List<File> files;
		
		if (pathWithStars.contains("*")) {
			// we have to do a wildcard match
			files = findFilesWithStar(pathWithStars);
		} else {
			// do normal add
			files = Arrays.asList(findFile(pathWithStars));
		}
		
		return files;
	}
	
	private List<File> findFilesWithStar(String path) {
		List<File> returnFile = null;
		String[] pathSegments = breakUpPath(path);
		
		if (pathSegments != null && pathSegments.length > 0) {
			returnFile = findFilesWithStar(pathSegments[pathSegments.length - 1], pathSegments);
		}
		
		return returnFile;
	}
	
	private List<File> findFilesWithStar(String fileName, String... pathSegments) {
		List<File> returnFile = new ArrayList<>();
		
		if (fileName.contains("*")) {
		
			List<String> possibleEntries = new ArrayList<String>();
			
			String[] segments = fileName.split("\\*");
			if (fileName.endsWith("*")) {
				List<String> list = new ArrayList<>(Arrays.asList(segments));
				list.add("");
				segments = list.toArray(new String[list.size()]);
			}
			
			for (String key : fileMap.keySet()) {
				if (matches(key, segments)) {
					possibleEntries.add(key);
				}
			}
			
			for (String key : possibleEntries) {
				String extension = calculateBestOption(pathSegments, fileMap.get(key));
				if (extension != null) {
					File testFile = new File(getDirectoryPath() + extension);
					if (testFile.exists()) {
						returnFile.add(testFile);
					}
				}
			}
		}
	
		return returnFile;
	}
	
	private boolean matches(String item, String[] segments) {
		int size = segments.length;
		
		boolean result = false;
		
		if (size >= 2) {
			boolean first = item.startsWith(segments[0]);
			boolean last = item.endsWith(segments[size - 1]);
			
			if (first && last) {
				String progress = item;
				int misses = 0;
				
				for (String string : segments) {
					int index = progress.indexOf(string);
					if (index != -1) {
						progress = progress.substring(index);
					} else {
						misses = 1;
						break;
					}
				}
				
				result = misses == 0;
			}
		} else if (size == 1) {
			result = item.equals(segments[0]);
		}
		
		
		return result;
	}
	
	/**
	 * Find the file on the file system given a static or dynamic file path.
	 * This will find the file on the file system with the same name and longest common
	 * @param path
	 * @return
	 */
	public File findFile(String path) {
		File returnFile = null;
		String[] pathSegments = breakUpPath(path);
		
		if (pathSegments != null && pathSegments.length > 0) {
			returnFile = findFile(pathSegments[pathSegments.length - 1], pathSegments);
		}
		
		return returnFile;
	}
	
	private File findFile(String fileName, String... pathSegments) {
		File returnFile = null;
		
		if (fileMap.containsKey(fileName) && !fileMap.get(fileName).isEmpty()) {
			String extension = calculateBestOption(pathSegments, fileMap.get(fileName));
			if (extension != null) {
				File testFile = new File(getDirectoryPath() + extension);
				if (testFile.exists()) {
					returnFile = testFile;
				}
			}
		}
		
		return returnFile;
	}
	
	public String findCanonicalFilePath(String path, String root) {
		String returnString = null;
		String[] pathSegments = breakUpPath(path);
		
		if (pathSegments != null && pathSegments.length > 0) {
			returnString = findFilePath(pathSegments[pathSegments.length - 1], pathSegments);
			
			if (returnString != null) {
				returnString = returnString.replace('\\', '/');
			}
		}
		
		if (returnString != null && root != null && returnString.startsWith(root)) {
			returnString = returnString.substring(root.length());
		}
		
		return returnString;
	}
	
	public String findCanonicalFilePath(String path) {
		String returnString = null;
		String[] pathSegments = breakUpPath(path);
		
		if (pathSegments != null && pathSegments.length > 0) {
			returnString = findFilePath(pathSegments[pathSegments.length - 1], pathSegments);
			
			if (returnString != null) {
				returnString = returnString.replace('\\', '/');
			}
		}
		
		if (returnString != null && directory != null && returnString.startsWith(directory.toString())) {
			returnString = returnString.substring(directory.toString().length());
		}
		
		return returnString;
	}
	
	private String findFilePath(String name, String... pathSegments) {
		String returnPath = null;
		
		if (fileMap.containsKey(name) && !fileMap.get(name).isEmpty()) {
			returnPath = calculateBestOption(pathSegments, fileMap.get(name));
		}
		
		return returnPath;
	}

	// score all the items in the set of choices against the given path segments
	private String calculateBestOption(String[] pathSegments, Set<String> choices) {
		String returnOption = null;
		
		int highestScore = -1;
		
		for (String choice : choices) {
			if (choice != null) {
				int choiceScore = calculateScore(breakUpPath(choice), pathSegments);
				if (choiceScore > highestScore) {
					returnOption = choice;
					highestScore = choiceScore;
					if (choiceScore == pathSegments.length) {
						break;
					}
				}
			}
		}
		
		return returnOption;
	}
	
	// split along / or \ or just return the whole path
	private String[] breakUpPath(String choice) {
		String[] results;
		
		if (choice.indexOf('/') != -1) {
			results = choice.split("/");
		} else if (choice.indexOf('\\') != -1) {
			results = choice.split("\\\\");
		} else {
			results = new String[] { choice };
		}

		return results;
	}
	
	// calculates the length of the common end elements
	private int calculateScore(String[] option, String[] path) {
		int score = 0;
		
		if (option != null && option.length != 0 &&
				path != null && path.length != 0) {
			int optionIndex = option.length - 1, pathIndex = path.length - 1;
			
			while (optionIndex >= 0 && pathIndex >= 0) {
				if (option[optionIndex].equals(path[pathIndex])) {
					score += 1;
					optionIndex -= 1;
					pathIndex -= 1;
				} else {
					break;
				}
			}
		}
		
		return score;
	}
}
