package com.rahman.arctic.shard.shards;

import lombok.Getter;

public class UIFieldCreation<T> {

	@Getter
	private String key;

	@Getter
	private String label;

	@Getter
	private FieldType type;

	@Getter
	private String keySource;

	@Getter
	private ShardProviderUICreation<T, ?> uiTool;

	public UIFieldCreation(String k, String l, FieldType t, String ks, ShardProviderUICreation<T, ?> uT) {
		key = k;
		label = l;
		type = t;
		keySource = ks;
		uiTool = uT;
	}

}