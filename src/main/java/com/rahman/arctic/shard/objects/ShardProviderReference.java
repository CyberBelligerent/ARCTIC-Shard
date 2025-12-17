package com.rahman.arctic.shard.objects;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShardProviderReference {

	private String name;
	private boolean loaded;
	private boolean enabled;
	private boolean errored;
	private String errorMessage;
	
	private List<ShardConfigurationReference> settings;
	private List<String> missingConfigurationKeys;
	
}