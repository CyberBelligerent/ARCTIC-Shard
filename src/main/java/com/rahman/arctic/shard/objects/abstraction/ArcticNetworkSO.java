package com.rahman.arctic.shard.objects.abstraction;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ArcticNetworkSO {
	private String name;
	private String providerId;
	private String rangeId;
	private String ipRangeStart;
	private String ipRangeEnd;
	private String ipGateway;
	private String ipCidr;
	private Map<String, String> extraVariables = new HashMap<>();
}