package com.rahman.arctic.shard.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class ProfileConfigReader {
	
	public static Map<String, Map<String, String>> run(Scanner scanner) {
		 Map<String, Map<String, String>> profileProperties = new HashMap<String, Map<String, String>>();
		
		String profileName = null;
		
		try {
			int lineNumber = 0;
			while(scanner.hasNextLine()) {
				++lineNumber;
				String line = scanner.nextLine().trim();
				
				if(line.isEmpty() || line.startsWith("#")) continue;
				String newProfileName = parseProfileName(line);
				
				if(newProfileName != null) {
					profileName = newProfileName;
					profileProperties.put(profileName, new HashMap<String, String>());
				} else {
					Entry<String, String> property = parsePropertyLine(line, lineNumber);
					
					if(property == null) {
						// TODO: Arctic Console Error with reading key = value pair
					}
					
					Map<String, String> props = profileProperties.get(profileName);
					
					if(props.containsKey(property.getKey())) {
						// TODO: Arctic Console Error with duplicate key readings
					}
					
					props.put(property.getKey(), property.getValue());
				}
				
			}
		} finally {
			scanner.close();
		}
		
		return profileProperties;
	}
	
	private static String parseProfileName(String line) {
		if(line.startsWith("[") && line.endsWith("]")) {
			String profileName = line.substring(1, line.length() - 1);
			return profileName.trim();
		}
		return null;
	}
	
	private static Entry<String, String> parsePropertyLine(String line, int lineNumber) {
		String[] pair = line.split("=", 2);
		if(pair.length != 2) {
			return null;
		}
		String propertyKey = pair[0].trim();
		String propertyValue = pair[1].trim();
		
		return new AbstractMap.SimpleImmutableEntry<String, String>(propertyKey, propertyValue);
	}

}