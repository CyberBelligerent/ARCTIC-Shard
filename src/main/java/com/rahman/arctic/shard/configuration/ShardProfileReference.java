package com.rahman.arctic.shard.configuration;

import java.util.Map;

import lombok.Data;

@Data
public class ShardProfileReference {
	private String name;
	private String domain;
	private String status;
	
	private Map<String, String> values;
}