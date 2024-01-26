package com.rahman.arctic.shard.shards;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.rahman.arctic.shard.ShardManager;
import com.rahman.arctic.shard.objects.ArcticHost;
import com.rahman.arctic.shard.objects.ArcticNetwork;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.util.ProfileProperties;

import lombok.Getter;

public abstract class ShardProviderTmpl<T> {
	
	@Autowired
	private ShardManager sManager;
	
	@Getter
	private T client;
	
	@Getter
	private ProfileProperties properties;
	
	@Getter
	private Map<String, ArcticTask<?>> networkTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<?>> instanceTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<?>> securityGroupTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<?>> routerTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<?>> volumeTasks = new HashMap<>();
	
	public ShardProviderTmpl(String domain) {
		properties = loadSettings(domain);
	}
	
	public abstract T createClient();
	
	public void createHost(ArcticHost ah) {
		instanceTasks.put(ah.getName(), buildHost(ah));
	}
	
	public void createNetwork(ArcticNetwork an) {
		networkTasks.put(an.getName(), buildNetwork(an));
	}
	
	protected abstract ArcticTask<?> buildHost(ArcticHost ah);
	protected abstract ArcticTask<?> buildNetwork(ArcticNetwork an);
	
	private ProfileProperties loadSettings(String domain) {
		return sManager.getShardProperties().get(domain);
	}
}