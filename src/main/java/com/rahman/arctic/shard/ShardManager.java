package com.rahman.arctic.shard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
			} catch(IOException e) {
				System.out.println("Unable to read config file, please delete and recreate.");
				System.exit(1);
				return;
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
			Map<String, String> props = entry.getValue();
			profiles.put(profileName, new ProfileProperties(profileName, props));
		}
		
		shardProperties = profiles;
	}
	
	public void registerShard(String name, ShardProviderTmpl<?> shard) {
		shards.put(name, shard);
	}
	
}