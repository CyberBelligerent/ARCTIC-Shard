package com.rahman.arctic.shard.objects;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class ArcticHost {
	private String ip;
	private String name;
	private String flavor;
	private Set<String> networks = new HashSet<>();
	private Set<String> volumes = new HashSet<>();
	private String rangeId;
}