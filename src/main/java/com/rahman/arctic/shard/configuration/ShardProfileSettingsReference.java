package com.rahman.arctic.shard.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ShardProfileSettingsReference {

	private final Map<String, String> settings;
	
	public ShardProfileSettingsReference(Map<String, String> settingsSnapshot) {
		settings = Collections.unmodifiableMap(new HashMap<>(settingsSnapshot));
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