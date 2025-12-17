package com.rahman.arctic.shard.shards;

import lombok.Getter;

public class UIFieldCreation<T> {

	@Getter
	private String key;
	
	@Getter
	private String label;
	
	@Getter
	private ShardProviderUICreation<T, ?> uiTool;
	
	public UIFieldCreation(String k, String l, ShardProviderUICreation<T, ?> uT) {
		key = k;
		label = l;
		uiTool = uT;
	}
	
}