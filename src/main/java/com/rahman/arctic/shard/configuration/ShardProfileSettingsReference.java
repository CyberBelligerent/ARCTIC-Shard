package com.rahman.arctic.shard.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class ShardProfileSettingsReference {

	@Getter
	private final String username;
	private final Map<String, String> settings;
	
	public ShardProfileSettingsReference(String u, Map<String, String> settingsSnapshot) {
		username = u;
		settings = Collections.unmodifiableMap(new HashMap<>(settingsSnapshot));
	}
	
	public void printAllSettings() {
		for(String key : settings.keySet()) {
			System.out.println(key + ":" + settings.get(key));
		}
	}
	
	public String getConfiguration(String key) {
		if(!settings.containsKey(key)) return null;
		return settings.get(key);
	}
	
	public String getConfigurationNonNullable(String key) throws Exception {
		String config_value = getConfiguration(key);
		if(config_value == null || config_value.isBlank()) throw new Exception("Make this an actual exception later");
		return config_value;
	}
	
	public String getConfigurationOrDefault(String key, String defaultValue) {
		String value = getConfiguration(key);
		return (value == null || value.isBlank()) ? defaultValue : value;
	}
	
	public boolean hasConfiguration(String key) {
		return settings.containsKey(key);
	}
	
}