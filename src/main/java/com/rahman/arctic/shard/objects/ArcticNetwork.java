package com.rahman.arctic.shard.objects;

import lombok.Data;

@Data
public class ArcticNetwork {
	private String name;
	private String rangeId;
	private String ipRangeStart;
	private String ipRangeEnd;
	private String ipGateway;
	private String ipCidr;
}