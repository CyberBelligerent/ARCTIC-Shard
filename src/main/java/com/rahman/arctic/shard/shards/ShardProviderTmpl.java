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
	private Map<String, ArcticTask<T,?>> networkTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> instanceTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> securityGroupTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> routerTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> volumeTasks = new HashMap<>();
	
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
	
	protected abstract ArcticTask<T,?> buildHost(ArcticHost ah);
	protected abstract ArcticTask<T,?> buildNetwork(ArcticNetwork an);
	
	private ProfileProperties loadSettings(String domain) {
		return sManager.getShardProperties().get(domain);
	}
}