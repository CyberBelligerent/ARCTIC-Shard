package com.rahman.arctic.shard.util;

import java.util.Map;

import lombok.Getter;

public class ProfileProperties {

	@Getter
	private String profileName;
	
	@Getter
	private Map<String, String> profileProperties;
	
	public ProfileProperties(String name, Map<String, String> props) {
		profileName = name;
		profileProperties = props;
	}
	
	public String getPropertyValue(String name) {
		return getProfileProperties().get(name);
	}
	
}