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
				return;
			}
			
			FileInputStream fis = null;
			
			try {
				fis = new FileInputStream(configFile);
				Map<String, Map<String, String>> properties = ProfileConfigReader.run(new Scanner(fis));
				convertPropertiesToObject(properties);
				
				for(String key : properties.keySet()) {
					Map<String, String> mainProps = properties.get(key);
					if(!mainProps.containsKey("class")) {
						System.out.println("Unable to Locate Cloud Environment Shard Loader for: " + key + " ...");
						continue;
					}
					
					Class<?> clazz = Class.forName(mainProps.get("class"));
					Constructor<?> constructor = clazz.getDeclaredConstructor(ShardManager.class);
					
					Object instance = constructor.newInstance(this);
					System.out.println("Successfully created instance: " + instance);
				}
			} catch(IOException | ClassNotFoundException e) {
				System.out.println("Unable to read config file, please delete and recreate.");
				System.exit(1);
				return;
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if(fis != null) {
					try {
						fis.close();
					} catch (IOException e) {}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param properties Domain with Key-Value properties to load into memory
	 */
	private void convertPropertiesToObject(Map<String, Map<String, String>> properties) {
		Map<String, ProfileProperties> profiles = new HashMap<>();
		for(Entry<String, Map<String, String>> entry : properties.entrySet()) {
			String profileName = entry.getKey();
			
			System.out.println("Loading Profile: " + profileName);
			
			Map<String, String> props = entry.getValue();
			profiles.put(profileName, new ProfileProperties(profileName, props));
		}
		
		shardProperties = profiles;
	}
	
	public void registerShard(String name, ShardProviderTmpl<?> shard) {
		System.out.println("Shard: " + name + " Registered");
		shards.put(name, shard);
	}
	
}