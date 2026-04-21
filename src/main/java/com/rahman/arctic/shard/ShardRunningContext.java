package com.rahman.arctic.shard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rahman.arctic.shard.configuration.ShardProfileSettingsReference;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlSettingSection;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;
import com.rahman.arctic.shard.shards.FieldType;
import com.rahman.arctic.shard.shards.ShardObjectType;
import com.rahman.arctic.shard.shards.UIFieldCreation;
import com.rahman.arctic.shard.shards.UIFieldReference;

import lombok.Getter;

public class ShardRunningContext<T> {

	@Getter
	private T client;
	private ShardProviderTmpl<T> provider;
	private ShardProfileSettingsReference config;
	ExecutorService executorService = Executors.newFixedThreadPool(10);

	public ShardRunningContext(ShardProviderTmpl<T> shardProvider, ShardProfileSettingsReference configSnapshot) {
		provider = shardProvider;
		config = configSnapshot;
	}

	public boolean validateConfiguration() {
		if (provider.getYamlReader().getConfigSections().isEmpty())
			return true;

		boolean canLoad = true;

		for (ShardYamlSettingSection syss : provider.getYamlReader().getConfigSections()) {
			if (syss.isRequired()) {
				if (!config.hasConfiguration(syss.getKey()))
					canLoad = false;
				String value = config.getConfiguration(syss.getKey());
				if (value == null || value.isBlank())
					canLoad = false;
			}
		}

		return canLoad;
	}

	public void createClient() {
		client = provider.createClient(config);
	}

	public boolean performConnectionTest() {
		client = provider.createClient(config);
		return (client != null);
	}

	public List<CompletableFuture<UIFieldReference>> runFields(ShardObjectType type) {
		createClient();
		
		if (!provider.getObtainableFutures().containsKey(type)) {
			System.err.println("Provider Context does not contain Object Type: " + type.toString());
			return new ArrayList<>();
		}

		LinkedHashSet<UIFieldCreation<T>> fields = provider.getObtainableFutures().get(type);
		List<CompletableFuture<UIFieldReference>> references = new ArrayList<>();
		
		fields.forEach(e -> {
			CompletableFuture<?> future = e.getUiTool().returnResults(client, executorService);
			CompletableFuture<UIFieldReference> rf = future.thenApply(result -> {
				UIFieldReference ufr = new UIFieldReference();
				ufr.setKey(e.getKey());
				ufr.setLabel(e.getLabel());
				ufr.setType(e.getType());
				ufr.setKeySource(e.getKeySource());
				ufr.setReturnValue(result);
				return ufr;
			}).exceptionally(ex -> {
				if(ex.getCause() != null) {
					System.out.println(ex.getCause().getMessage());
				} else {
					System.out.println(ex.getMessage());
				}
				UIFieldReference ufr = new UIFieldReference();
				ufr.setKey(e.getKey());
				ufr.setLabel(e.getLabel());
				ufr.setType(e.getType());
				ufr.setKeySource(e.getKeySource());
				ufr.setReturnValue("Error");
				return ufr;
			});
			
			references.add(rf);
		});
		
		return references;
	}

	@Getter
	private Map<String, ArcticTask<T, ?>> networkTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> instanceTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> securityGroupTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> securityGroupRuleTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> routerTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> volumeTasks = new HashMap<>();

	public void createHost(ArcticHostSO ah) {
		instanceTasks.put(ah.getName(), provider.buildHost(this, ah));
	}

	public void createNetwork(ArcticNetworkSO an) {
		networkTasks.put(an.getName(), provider.buildNetwork(this, an));
	}

	public void createSecurityGroup(ArcticSecurityGroupSO asg) {
		securityGroupTasks.put(asg.getName(), provider.buildSecurityGroup(this, asg));
	}

	public void createSecurityGroupRule(ArcticSecurityGroupRuleSO asgr) {
		securityGroupRuleTasks.put(asgr.getName(), provider.buildSecurityGroupRule(this, asgr));
	}

	public void createRouter(ArcticRouterSO ar) {
		routerTasks.put(ar.getName(), provider.buildRouter(this, ar));
	}

	public void createVolume(ArcticVolumeSO av) {
		volumeTasks.put(av.getName(), provider.buildVolume(this, av));
	}

	@Getter
	private Map<String, ArcticTask<T, ?>> destroyInstanceTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> destroyNetworkTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> destroySecurityGroupTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> destroySecurityGroupRuleTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> destroyRouterTasks = new HashMap<>();

	@Getter
	private Map<String, ArcticTask<T, ?>> destroyVolumeTasks = new HashMap<>();

	public void destroyHost(ArcticHostSO ah) {
		destroyInstanceTasks.put(ah.getName(), provider.destroyHost(this, ah));
	}

	public void destroyNetwork(ArcticNetworkSO an) {
		destroyNetworkTasks.put(an.getName(), provider.destroyNetwork(this, an));
	}

	public void destroySecurityGroup(ArcticSecurityGroupSO asg) {
		destroySecurityGroupTasks.put(asg.getName(), provider.destroySecurityGroup(this, asg));
	}

	public void destroySecurityGroupRule(ArcticSecurityGroupRuleSO asgr) {
		destroySecurityGroupRuleTasks.put(asgr.getName(), provider.destroySecurityGroupRule(this, asgr));
	}

	public void destroyRouter(ArcticRouterSO ar) {
		destroyRouterTasks.put(ar.getName(), provider.destroyRouter(this, ar));
	}

	public void destroyVolume(ArcticVolumeSO av) {
		destroyVolumeTasks.put(av.getName(), provider.destroyVolume(this, av));
	}

}