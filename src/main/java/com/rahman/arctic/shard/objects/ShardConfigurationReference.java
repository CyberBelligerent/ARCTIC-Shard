package com.rahman.arctic.shard.objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ShardConfigurationReference {

	private String key;
	private String value;
	private String type;
	private boolean required;
	
}