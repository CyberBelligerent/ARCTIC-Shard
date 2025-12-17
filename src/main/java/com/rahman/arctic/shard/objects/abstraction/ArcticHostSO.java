package com.rahman.arctic.shard.objects.abstraction;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class ArcticHostSO {
	private String ip;
	private String name;
	private String defaultUser;
	private String defaultPassword;
	private String flavor;
	private String rangeId;
	private String imageId;
	private String osType;
	private Set<String> wantedIPs = new HashSet<>();
	private Set<String> networks = new HashSet<>();
	private Set<String> volumes = new HashSet<>();
}