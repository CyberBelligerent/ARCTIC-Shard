package com.rahman.arctic.shard;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Map;

import com.rahman.arctic.shard.configuration.ShardProfileSettingsReference;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlReader;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;
import com.rahman.arctic.shard.shards.ShardProviderUICreation;
import com.rahman.arctic.shard.shards.UIField;
import com.rahman.arctic.shard.shards.UIFieldCreation;

import lombok.Getter;
import lombok.Setter;

public abstract class ShardProviderTmpl<T> {

	@Getter
	@Setter
	private String shardPluginName;
	
	@Getter
	private LinkedHashSet<UIFieldCreation<T>> uiCreationTools = new LinkedHashSet<>();
	
	@Getter
	@Setter
	private URLClassLoader loader;
	
	@Getter @Setter
	private boolean enabled = false;
	
	@Getter
	private boolean error = false;
	
	@Getter @Setter
	private ShardYamlReader yamlReader;
	
	@Getter
	private String errorMessage = "";

	public abstract String getDomain();

	public abstract T createClient(ShardProfileSettingsReference config);

	public void pluginEnabled() {}
	public void pluginDisabled() {}

	final void runPlugin() {
//		this.client = createClient();
		pluginEnabled();
	}
	
	public boolean isInitialized() {
		String name = getDomain();
		
		return ((name != null && !name.isBlank()));
	}

	public <R> void registerUICreation(ShardProviderUICreation<T, R> uiTool) {
		
		Method m = null;
		try {
			m = uiTool.getClass().getMethod("returnResult");
		} catch (NoSuchMethodException | SecurityException e) {
			System.err.println("Unable to find required Method 'returnResult'");
			return;
		}
		
		if(!m.isAnnotationPresent(UIField.class)) {
			System.err.println("Method is missing required UIField");
			return;
		}
		
		uiTool.setProvider(this);
		
		UIField f = m.getAnnotation(UIField.class);
		uiCreationTools.add(new UIFieldCreation<T>(f.key(), f.label(), uiTool));
	}
	
	@SuppressWarnings("unchecked")
	public final <R> ArcticTask<T, R> getTypedTask(Map<String, ArcticTask<T, ?>> map, String name) {
		return (ArcticTask<T, R>) map.get(name);
	}
	
	public void fail() {
		failWithMessage("");
	}
	
	public void failWithMessage(String message) {
		enabled = false;
		error = true;
		errorMessage = message;
		System.err.println("[" + getDomain() + "] - " + message);
	}
	
	protected ShardRunningContext<T> createRunningContext(ShardProfileSettingsReference config) {
		return new ShardRunningContext<T>(this, config);
	}

	protected abstract ArcticTask<T, ?> buildHost(ShardRunningContext<T> context, ArcticHostSO ah);

	protected abstract ArcticTask<T, ?> buildNetwork(ShardRunningContext<T> context, ArcticNetworkSO an);

	protected abstract ArcticTask<T, ?> buildSecurityGroup(ShardRunningContext<T> context, ArcticSecurityGroupSO asg);

	protected abstract ArcticTask<T, ?> buildSecurityGroupRule(ShardRunningContext<T> context, ArcticSecurityGroupRuleSO asgr);

	protected abstract ArcticTask<T, ?> buildRouter(ShardRunningContext<T> context, ArcticRouterSO ar);

	protected abstract ArcticTask<T, ?> buildVolume(ShardRunningContext<T> context, ArcticVolumeSO av);
}