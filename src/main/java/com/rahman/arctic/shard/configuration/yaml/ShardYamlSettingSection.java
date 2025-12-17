package com.rahman.arctic.shard.configuration.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShardYamlSettingSection {

	private String key;
	private String type;
	private boolean required;
	
}