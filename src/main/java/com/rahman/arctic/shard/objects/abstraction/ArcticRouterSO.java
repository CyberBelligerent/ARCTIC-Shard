package com.rahman.arctic.shard.objects.abstraction;

import java.util.Set;

import lombok.Data;

@Data
public class ArcticRouterSO {
	private String name;
	private String rangeId;
	private Set<String> connectedNetworkNames;
}