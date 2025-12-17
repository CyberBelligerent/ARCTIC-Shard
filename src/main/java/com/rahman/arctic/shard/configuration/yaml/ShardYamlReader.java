package com.rahman.arctic.shard.configuration.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class ShardYamlReader {

	@Getter
	public String className;

	@Getter
	public String version;

	@Getter
	public List<ShardYamlSettingSection> configSections = new ArrayList<>();

	public ShardYamlReader(String name, Map<String, Object> inProperties) throws Exception {
		if (!inProperties.containsKey("class")) {
			throw new Exception("[" + name + "] - Required key: \'class\' missing from shard.yml");
		}

		if (!inProperties.containsKey("version")) {
			throw new Exception("[" + name + "] - Required key: \'version\' missing from shard.yml");
		}
		
		className = inProperties.get("class").toString();
		version = inProperties.get("version").toString();

		if (!inProperties.containsKey("config_settings")) {
			return;
		}

		Object rawConfigSettings = inProperties.get("config_settings");

		if (!(rawConfigSettings instanceof Map)) {
			throw new IllegalStateException("config_settings must be a map of key -> definition");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> configSettings = (Map<String, Object>) rawConfigSettings;

		for (String configKey : configSettings.keySet()) {

			Object rawConfigSection = configSettings.get(configKey);
			if (!(rawConfigSection instanceof Map)) {
				throw new IllegalStateException("\'" + rawConfigSection + "\' must be a map of key -> definition");
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> configSection = (Map<String, Object>) rawConfigSection;

			configSections.add(new ShardYamlSettingSection(configKey, configSection.getOrDefault("type", "STRING").toString().toUpperCase(), (boolean)configSection.getOrDefault("required", false)));
		}
	}

}