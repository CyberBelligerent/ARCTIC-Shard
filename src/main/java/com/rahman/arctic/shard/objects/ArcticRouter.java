package com.rahman.arctic.shard.objects;

import java.util.Set;

import lombok.Data;

@Data
public class ArcticRouter {
	private String name;
	private String rangeId;
	private Set<String> connectedNetworkNames;
}