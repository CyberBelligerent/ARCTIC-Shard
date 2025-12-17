package com.rahman.arctic.shard.objects.abstraction;

import lombok.Data;

@Data
public class ArcticNetworkSO {
	private String name;
	private String rangeId;
	private String ipRangeStart;
	private String ipRangeEnd;
	private String ipGateway;
	private String ipCidr;
}