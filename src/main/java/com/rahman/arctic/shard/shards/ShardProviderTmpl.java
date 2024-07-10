package com.rahman.arctic.shard.shards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.rahman.arctic.shard.ShardManager;
import com.rahman.arctic.shard.objects.ArcticHostSO;
import com.rahman.arctic.shard.objects.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.ArcticRouterSO;
import com.rahman.arctic.shard.objects.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.ArcticVolumeSO;
import com.rahman.arctic.shard.objects.providers.ProviderFlavor;
import com.rahman.arctic.shard.objects.providers.ProviderImage;
import com.rahman.arctic.shard.util.ProfileProperties;

import lombok.Getter;

public abstract class ShardProviderTmpl<T> {

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
	private Map<String, ArcticTask<T, ?>> securityGroupRuleTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> routerTasks = new HashMap<>();
	
	@Getter
	private Map<String, ArcticTask<T,?>> volumeTasks = new HashMap<>();
	
	/**
	 * Creates a new Shard as a connector for the Cloud Environment wanted
	 * @param domain
	 * @param manager
	 */
	public ShardProviderTmpl(String domain, ShardManager manager) {
		sManager = manager;
		
		properties = loadSettings(domain);
		sManager.registerShard(domain, this);
		client = createClient();
	}
	
	public abstract T createClient();
	
	public void createHost(ArcticHostSO ah) {
		instanceTasks.put(ah.getName(), buildHost(ah));
	}
	
	public void createNetwork(ArcticNetworkSO an) {
		networkTasks.put(an.getName(), buildNetwork(an));
	}
	
	public void createSecurityGroup(ArcticSecurityGroupSO asg) {
		securityGroupTasks.put(asg.getName(), buildSecurityGroup(asg));
	}
	
	public void createSecurityGroupRule(ArcticSecurityGroupRuleSO asgr) {
		securityGroupRuleTasks.put(asgr.getName(), buildSecurityGroupRule(asgr));
	}
	
	public void createRouter(ArcticRouterSO ar) {
		routerTasks.put(ar.getName(), buildRouter(ar));
	}
	
	public void createVolume(ArcticVolumeSO av) {
		volumeTasks.put(av.getName(), buildVolume(av));
	}
	
	protected abstract ArcticTask<T,?> buildHost(ArcticHostSO ah);
	protected abstract ArcticTask<T,?> buildNetwork(ArcticNetworkSO an);
	protected abstract ArcticTask<T,?> buildSecurityGroup(ArcticSecurityGroupSO asg);
	protected abstract ArcticTask<T,?> buildSecurityGroupRule(ArcticSecurityGroupRuleSO asgr);
	protected abstract ArcticTask<T,?> buildRouter(ArcticRouterSO ar);
	protected abstract ArcticTask<T,?> buildVolume(ArcticVolumeSO av);
	public abstract CompletableFuture<List<ProviderImage>> obtainOS();
	public abstract CompletableFuture<List<ProviderFlavor>> obtainFlavors();
	
	private ProfileProperties loadSettings(String domain) {
		return sManager.getShardProperties().get(domain);
	}
}