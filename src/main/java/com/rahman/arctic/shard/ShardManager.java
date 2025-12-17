package com.rahman.arctic.shard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.springframework.stereotype.Service;

import com.rahman.arctic.shard.shards.ShardProviderTmpl;
import com.rahman.arctic.shard.util.ProfileConfigReader;
import com.rahman.arctic.shard.util.ProfileProperties;

import lombok.Getter;

@Service
public class ShardManager {

	@Getter
	private Map<String, ProfileProperties> shardProperties = new HashMap<>();
	
	@Getter
	private Map<String, ShardProviderTmpl<?>> shards = new HashMap<>();
	
	public ShardProviderTmpl<?> getPrimaryShard() {
		// TODO: Allow this to be switchable
		return shards.get("openstack");
	}
	
	/**
	 * Creates Shard configurations for acceptable Shard Interfaces through Cloud Environments
	 * @param f File of which to pull
	 */
	public ShardManager(String f) {
		File configFile = new File(f);
		if(configFile != null) {
			if(!configFile.exists() || !configFile.isFile()) {
				System.out.println("Config file not found. Please Create file: " + f + " next to the jar file");
				System.exit(1);
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
	
		}
		
	}
	
	public void registerShard(String name, ShardProviderTmpl<?> shard) {
		System.out.println("Shard: " + name + " Registered");
		shards.put(name, shard);
	}

}