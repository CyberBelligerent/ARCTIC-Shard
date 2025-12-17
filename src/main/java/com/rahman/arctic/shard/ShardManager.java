package com.rahman.arctic.shard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.rahman.arctic.shard.configuration.ShardConfigurationService;
import com.rahman.arctic.shard.configuration.persistence.ShardConfiguration;
import com.rahman.arctic.shard.configuration.persistence.ShardConfigurationType;
import com.rahman.arctic.shard.configuration.persistence.ShardProfile;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlReader;
import com.rahman.arctic.shard.configuration.yaml.ShardYamlSettingSection;
import com.rahman.arctic.shard.objects.ShardConfigurationReference;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;

@Service
public class ShardManager {

	@Getter
	private Map<String, ShardProviderTmpl<?>> shards = new HashMap<>();

	@Getter
	private Map<ShardProfile, ShardRunningContext<?>> runningShardProfiles = new HashMap<>();
	
	private final ShardConfigurationService configurationService;

	public void createSession(ShardProfile profile) {
		ShardProviderTmpl<?> provider = shards.get(profile.getDomain());
		if(provider == null) throw new ResourceNotFoundException("Unable to load domain");
		ShardRunningContext<?> context = provider.createRunningContext(configurationService.getAllConfigurationsForProfile(profile.getId()));
		
		if(!context.validateConfiguration()) throw new ResourceNotFoundException("Configuration components missing");
		
		context.createClient();
		runningShardProfiles.put(profile, context);
	}
	
	public boolean performConnectionTest(ShardProfile profile) {
		ShardProviderTmpl<?> provider = shards.get(profile.getDomain());
		if(provider == null) return false;
		ShardRunningContext<?> context = provider.createRunningContext(configurationService.getAllConfigurationsForProfile(profile.getId()));
		
		if(!context.validateConfiguration()) return false;
		return context.performConnectionTest();
	}
	
	public ShardRunningContext<?> getSession(ShardProfile profile) {
		ShardRunningContext<?> context = runningShardProfiles.get(profile);
		if(context == null) throw new ResourceNotFoundException("Unable to load running shard context");
		return context;
	}
	
	public CompletableFuture<?> createOneOffSession(ShardProfile profile, String key) {
		ShardProviderTmpl<?> provider = shards.get(profile.getDomain());
		if(provider == null) throw new ResourceNotFoundException("Unable to load domain");
		ShardRunningContext<?> context = provider.createRunningContext(configurationService.getAllConfigurationsForProfile(profile.getId()));
		
		if(!context.validateConfiguration()) throw new ResourceNotFoundException("Configuration components missing");
		
		return context.runOneOffSession(key);
	}
	
	@PreDestroy
	public void destroyPluginLoaders() {
		for (ShardProviderTmpl<?> shardPlugin : getShards().values()) {
			try {
				System.out.println("[" + shardPlugin.getShardPluginName() + "] - Closing");
				try {
					shardPlugin.pluginDisabled();
				} catch (Exception e) {
					System.err.println("[" + shardPlugin.getShardPluginName() + "] - Error with ShardPlugins Disable method");
				}
				shardPlugin.getLoader().close();
			} catch (IOException e) {
				System.err.println("[" + shardPlugin.getShardPluginName() + "] Issue with closing plugin");
			}
		}
	}

	public ShardManager(ShardConfigurationService service) {
		configurationService = service;
	}

	@PostConstruct
	public void checkForPotentialShardPlugins() {
		File providersFolder = new File("providers");
		if (!providersFolder.exists()) {
			if (!providersFolder.mkdir()) {
				System.err.println("Failed to create providers directory");
				return;
			}
			System.out.println("ARCTIC Shard: Providers empty, will be unable to connect to hypervisor");
			return;
		}

		if (!providersFolder.isDirectory()) {
			System.err.println("'providers' exists but is not a directory");
			return;
		}

		System.out.println("Checking for ShardPlugins...");

		File[] potentialProviderPlugins = providersFolder.listFiles();

		if (potentialProviderPlugins == null || potentialProviderPlugins.length == 0) {
			System.out.println("ARCTIC Shard: Providers empty, will be unable to connect to hypervisor");
			return;
		}

		for (File potentialProviderPlugin : potentialProviderPlugins) {
			enablePotentialShardPlugin(potentialProviderPlugin);
		}
	}

	private void enablePotentialShardPlugin(File potentialProviderPlugin) {
		if (!potentialProviderPlugin.getName().endsWith(".jar"))
			return;

		try {
			URL jarUrl = potentialProviderPlugin.toURI().toURL();
			URLClassLoader pluginLoader = new URLClassLoader(new URL[] { jarUrl },
					Thread.currentThread().getContextClassLoader());

			Yaml yaml = new Yaml();
			Map<String, Object> yamlSettings;
			try (InputStream stream = pluginLoader.getResourceAsStream("shard.yml")) {
				if (stream == null) {
					System.err.println("[" + potentialProviderPlugin.getName() + "] - ShardPlugin missing shard.yml");
					pluginLoader.close();
					return;
				}
				yamlSettings = yaml.load(stream);
			}
			
			ShardYamlReader syr = null;
			try {
				syr = new ShardYamlReader(potentialProviderPlugin.getName(), yamlSettings);
			} catch (Exception e) {
				pluginLoader.close();
				return;
			}

			String pluginClassName = syr.getClassName();
			Class<?> pluginClass = null;
			try {
				pluginClass = pluginLoader.loadClass(pluginClassName);
			} catch (ClassNotFoundException e) {
				System.err.println(
						"[" + potentialProviderPlugin.getName() + "] - Unable to find class: " + pluginClassName);
				pluginLoader.close();
				return;
			}

			if (!ShardProviderTmpl.class.isAssignableFrom(pluginClass)) {
				System.err.println("[" + potentialProviderPlugin.getName()
						+ "] - Provider is not of instance ShardProviderTmpl.class");
				pluginLoader.close();
				return;
			}

			Object instantiatedPluginClass = pluginClass.getDeclaredConstructor().newInstance();
			ShardProviderTmpl<?> shardPlugin = (ShardProviderTmpl<?>) instantiatedPluginClass;

			if (shardPlugin.getDomain() == null || shardPlugin.getDomain().isBlank()) {
				System.err.println(
						"[" + shardPlugin.getDomain() + "] - Provider Domain is not set using getDomain()");
				pluginLoader.close();
				return;
			}

			if (configurationService == null) {
				System.err
						.println("[" + shardPlugin.getDomain() + "] - Unable to load configuration Service");
				pluginLoader.close();
				return;
			}

			shardPlugin.setShardPluginName(potentialProviderPlugin.getName());
			shardPlugin.setLoader(pluginLoader);
			populateDatabase(shardPlugin.getDomain(), syr);
			
			if (!shardPlugin.isInitialized()) {
				System.err.println(
						"[" + shardPlugin.getDomain() + "] - Provider was unable to hook injected variables");
				pluginLoader.close();
				return;
			}
			
			System.out.println("[" + shardPlugin.getDomain() + "] - ShardPlugin registered");
			registerShard(shardPlugin.getDomain(), shardPlugin);
			shardPlugin.setYamlReader(syr);
			shardPlugin.setEnabled(true);
			shardPlugin.runPlugin();
		} catch (MalformedURLException e) {
			System.err.println("Unable to parse file path: " + potentialProviderPlugin.getName());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private List<ShardConfigurationReference> populateDatabase(String domain, ShardYamlReader syr) {
		if(syr.getConfigSections().isEmpty()) return null;
		
		List<ShardConfigurationReference> objects = new ArrayList<>();
		for(ShardYamlSettingSection syss : syr.getConfigSections()) {
			if(!configurationService.hasConfigurationDetail(domain, syss.getKey())) {
				ShardConfiguration sc = new ShardConfiguration();
				sc.setConfigDomain(domain);
				sc.setConfigKey(syss.getKey());
				sc.setConfigType(ShardConfigurationType.valueOf(syss.getType()));
				sc.setConfigRequired(syss.isRequired());
				configurationService.addConfigurationDetail(sc);
				System.out.println("[" + domain + "] - Enabled configuration setting: " + syss.getKey());
			}
		}
		
		return objects;
	}
	
//	public ShardProviderReference acquireShardProviderInformation(String name) {
//		if(!shards.containsKey(name)) return new ShardProviderReference(name, false, false, true, "Unable to load", null, null);
//		ShardProviderTmpl<?> shardPlugin = shards.get(name);
//		
//		List<ShardConfiguration> configOptions = configurationService.getAllConfigurationOptions(name);
//		List<ShardConfigurationReference> configReference = new ArrayList<>();
//		List<String> missingConfiguration = new ArrayList<>();
//		if(configOptions != null) {
//			for(ShardConfiguration sc : configOptions) {
//				if(sc.isConfigRequired() && (sc.getConfigValue() == null || sc.getConfigValue().isBlank())) {
//					missingConfiguration.add(sc.getConfigKey());
//				}
//				
//				String domain = sc.getConfigDomain();
//				String key = sc.getConfigKey();
//				String value = "";
//				String configType = sc.getConfigType().toString().toLowerCase();
//				boolean isRequired = sc.isConfigRequired();
//				if(sc.getConfigType().equals(ShardConfigurationType.PASSWORD)) {
//					value = "********************";
//				} else {
//					value = configurationService.getConfiguration(domain, key);
//				}
//				
//				configReference.add(new ShardConfigurationReference(key, value, configType, isRequired));
//			}
//		}
//		
//		return new ShardProviderReference(name, true, shardPlugin.isEnabled(), shardPlugin.isError(), shardPlugin.getErrorMessage(), configReference, missingConfiguration);
//	}

	public void registerShard(String name, ShardProviderTmpl<?> shard) {
		System.out.println("Shard: " + name + " Registered");
		shards.put(name, shard);
	}

}