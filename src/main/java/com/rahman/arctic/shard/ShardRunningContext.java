package com.rahman.arctic.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import com.rahman.arctic.shard.configuration.ShardProfileSettingsReference;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlSettingSection;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;
import com.rahman.arctic.shard.shards.UIFieldCreation;

import lombok.Getter;

public class ShardRunningContext<T> {

	@Getter
	private T client;
	private ShardProviderTmpl<T> provider;
	private ShardProfileSettingsReference config;
	
	public ShardRunningContext(ShardProviderTmpl<T> shardProvider, ShardProfileSettingsReference configSnapshot) {
		provider = shardProvider;
		config = configSnapshot;
	}
	
	public boolean validateConfiguration() {
		if(provider.getYamlReader().getConfigSections().isEmpty()) return true;
		
		boolean canLoad = true;
		
		for(ShardYamlSettingSection syss : provider.getYamlReader().getConfigSections()) {
			if(syss.isRequired()) {
				if(!config.hasConfiguration(syss.getKey())) canLoad = false;
				String value = config.getConfiguration(syss.getKey());
				if(value == null || value.isBlank()) canLoad = false;
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
	
	public CompletableFuture<?> runOneOffSession(String key) {
		UIFieldCreation<T> uiToRun = null;
		for(UIFieldCreation<T> ui : provider.getUiCreationTools()) {
			if(ui.getKey().equals(key)) {
				uiToRun = ui;
				break;
			}
		}
		
		if(uiToRun == null) throw new ResourceNotFoundException("Provider does not have UIField with name: " + key);
		
		uiToRun.getUiTool().initialize(getClient());
		return uiToRun.getUiTool().returnResult();
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
	
}