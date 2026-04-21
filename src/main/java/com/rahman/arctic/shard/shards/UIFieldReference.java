package com.rahman.arctic.shard.shards;

import lombok.Data;

@Data
public class UIFieldReference {
	private String key;
	private String label;
	private FieldType type;
	private String keySource;
	private Object returnValue;
}