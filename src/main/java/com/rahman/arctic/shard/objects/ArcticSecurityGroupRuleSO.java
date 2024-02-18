package com.rahman.arctic.shard.objects;

import lombok.Data;

@Data
public class ArcticSecurityGroupRuleSO {
	private String name;
	private String description;
	private String secGroup;
	private String direction;
	private int startPortRange;
	private int endPortRange;
	private String protocol;
	private String eth;
	private String rangeId;
}