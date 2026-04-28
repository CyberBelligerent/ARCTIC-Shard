package com.rahman.arctic.shard;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.rahman.arctic.shard.util.IpUtil;

import com.rahman.arctic.shard.configuration.ShardProfileSettingsReference;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlReader;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;
import com.rahman.arctic.shard.shards.ShardObjectType;
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
	private Map<ShardObjectType, LinkedHashSet<UIFieldCreation<T>>> obtainableFutures = new HashMap<>();
	
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
		
		Method m = Arrays.stream(uiTool.getClass().getMethods())
		        .filter(method -> method.getName().equals("returnResult"))
		        .filter(method -> method.getParameterCount() == 1)
		        .filter(method -> !method.isBridge() && !method.isSynthetic())
		        .findFirst()
		        .orElse(null);

		if (m == null) {
		    System.err.println("Unable to find required Method 'returnResult(T)'");
		    return;
		}
		
		if(!m.isAnnotationPresent(UIField.class)) {
			System.err.println("Method is missing required UIField");
			return;
		}
		
		uiTool.setProvider(this);
		
		UIField f = m.getAnnotation(UIField.class);
		
		if(!obtainableFutures.containsKey(f.object())) obtainableFutures.put(f.object(), new LinkedHashSet<>());
		
		System.out.println("[" + getDomain() + "]" + " adding key for " + f.object().toString());
		
		obtainableFutures.get(f.object()).add(new UIFieldCreation<T>(f.key(), f.label(), f.type(), f.keySource(), uiTool));
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

	/**
	 * Default fan-out for a host collection: builds one task per pre-cloned instance SO. Plugins
	 * with native bulk creation (e.g. OpenStack min/max_count) can override and ignore the cloned
	 * SOs in favor of a single bulk API call.
	 */
	protected Map<String, ArcticTask<T, ?>> buildHostCollection(ShardRunningContext<T> context,
			ArcticHostSO source, List<ArcticHostSO> instanceSos) {
		Map<String, ArcticTask<T, ?>> tasks = new LinkedHashMap<>();
		for (ArcticHostSO so : instanceSos) {
			tasks.put(so.getName(), buildHost(context, so));
		}
		return tasks;
	}

	public ArcticHostSO cloneSoForInstance(ArcticHostSO source, int index) {
		ArcticHostSO clone = new ArcticHostSO();
		clone.setName(source.getName() + "-" + (index + 1));
		clone.setRangeId(source.getRangeId());
		clone.setOsType(source.getOsType());
		clone.setNetworks(new HashSet<>(source.getNetworks()));
		clone.setVolumes(new HashSet<>(source.getVolumes()));
		clone.setCount(1);
		clone.setCollectionId(source.getCollectionId());
		clone.setPriorityOverride(source.getPriorityOverride());
		clone.setDestroyPriorityOverride(source.getDestroyPriorityOverride());

		Map<String, String> vars = new HashMap<>(source.getExtraVariables());
		if (vars.containsKey("network_ips"))
			vars.put("network_ips", incrementIpKvLines(vars.get("network_ips"), index));
		if (vars.containsKey("external_network_ips"))
			vars.put("external_network_ips", incrementIpKvLines(vars.get("external_network_ips"), index));
		clone.setExtraVariables(vars);
		return clone;
	}

	private static String incrementIpKvLines(String raw, int delta) {
		if (raw == null || raw.isBlank() || delta == 0) return raw;
		StringBuilder out = new StringBuilder();
		for (String line : raw.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) continue;
			int eq = trimmed.indexOf('=');
			if (eq <= 0) { out.append(line).append('\n'); continue; }
			String key = trimmed.substring(0, eq).trim();
			String value = trimmed.substring(eq + 1).trim();
			try {
				value = IpUtil.increment(value, delta);
			} catch (IllegalArgumentException ignored) {
				// not a parseable IPv4 — leave the value alone
			}
			out.append(key).append('=').append(value).append('\n');
		}
		return out.toString().stripTrailing();
	}

	protected abstract ArcticTask<T, ?> buildNetwork(ShardRunningContext<T> context, ArcticNetworkSO an);

	protected abstract ArcticTask<T, ?> buildSecurityGroup(ShardRunningContext<T> context, ArcticSecurityGroupSO asg);

	protected abstract ArcticTask<T, ?> buildSecurityGroupRule(ShardRunningContext<T> context, ArcticSecurityGroupRuleSO asgr);

	protected abstract ArcticTask<T, ?> buildRouter(ShardRunningContext<T> context, ArcticRouterSO ar);

	protected abstract ArcticTask<T, ?> buildVolume(ShardRunningContext<T> context, ArcticVolumeSO av);

	protected abstract ArcticTask<T, ?> destroyHost(ShardRunningContext<T> context, ArcticHostSO ah);

	protected abstract ArcticTask<T, ?> destroyNetwork(ShardRunningContext<T> context, ArcticNetworkSO an);

	protected abstract ArcticTask<T, ?> destroySecurityGroup(ShardRunningContext<T> context, ArcticSecurityGroupSO asg);

	protected abstract ArcticTask<T, ?> destroySecurityGroupRule(ShardRunningContext<T> context, ArcticSecurityGroupRuleSO asgr);

	protected abstract ArcticTask<T, ?> destroyRouter(ShardRunningContext<T> context, ArcticRouterSO ar);

	protected abstract ArcticTask<T, ?> destroyVolume(ShardRunningContext<T> context, ArcticVolumeSO av);

	protected abstract boolean templateExists(ShardRunningContext<T> context, String name);
	protected abstract String uploadTemplate(ShardRunningContext<T> context, Path filePath, String name);
}