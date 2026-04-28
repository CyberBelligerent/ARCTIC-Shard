package com.rahman.arctic.shard.objects.abstraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;

@Data
public class ArcticHostSO {
	private String ip;
	private String providerId;
	private String name;
	private String rangeId;
	private String osType;
	private Set<String> networks = new HashSet<>();
	private Set<String> volumes = new HashSet<>();

	private int count = 1;
	private String collectionId;

	private Integer priorityOverride;
	private Integer destroyPriorityOverride;

	private Map<String, String> extraVariables = new HashMap<>();
}