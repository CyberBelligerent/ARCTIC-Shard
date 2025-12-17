package com.rahman.arctic.shard.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rahman.arctic.shard.configuration.persistence.ShardConfiguration;
import com.rahman.arctic.shard.configuration.persistence.ShardProfile;
import com.rahman.arctic.shard.configuration.persistence.ShardProfileSettings;
import com.rahman.arctic.shard.repos.ShardConfigurationRepo;
import com.rahman.arctic.shard.repos.ShardProfileRepo;
import com.rahman.arctic.shard.repos.ShardProfileSettingsRepo;

import jakarta.transaction.Transactional;

@Service
public class ShardConfigurationService {

	private final ShardConfigurationRepo shardConfigRepo;
	private final ShardProfileRepo profileRepo;
	private final ShardProfileSettingsRepo profileSettingsRepo;
	private final CryptoHelper crypto;
	
	public ShardConfigurationService(CryptoHelper ch, ShardConfigurationRepo scr, ShardProfileRepo spr, ShardProfileSettingsRepo spsr) {
		crypto = ch;
		shardConfigRepo = scr;
		profileRepo = spr;
		profileSettingsRepo = spsr;
	}
	
	public void addConfigurationDetail(ShardConfiguration sc) {		
		shardConfigRepo.save(sc);
	}
	
	public boolean hasConfigurationDetail(String domain, String key) {
		ShardConfiguration config = shardConfigRepo.findByConfigDomainAndConfigKey(domain, key).orElse(null);
		
		return (config == null) ? false : true;
	}
	
	public List<ShardConfiguration> getAllConfigurationOptions(String domain) {
		return shardConfigRepo.findAllByConfigDomain(domain).orElse(null);
	}
	
	public ShardProfileSettingsReference getAllConfigurationsForProfile(String profileId) {
		ShardProfile sp = profileRepo.findById(profileId).orElse(null);
		if(sp == null) {
			// TOOD: Throw an issue!
			return null;
		}
		
		List<ShardProfileSettings> profileSettings = profileSettingsRepo.findAllByProfileId(profileId).orElse(new ArrayList<>());
		if(profileSettings.isEmpty()) return null;
		
		Map<String, String> settings = new HashMap<>();
		for(ShardProfileSettings sps : profileSettings) {
			settings.put(sps.getProfileKey(), crypto.decryptValue(sps.getProfileValue()));
		}
		
		return new ShardProfileSettingsReference(settings);
	}
	
	@Transactional
	public void setConfiguration(String profileId, String key, String value) throws Exception {
		ShardProfile sp = profileRepo.findById(profileId).orElse(null);
		if(sp == null) {
			// TOOD: Throw an issue!
			return;
		}
		
		ShardProfileSettings sps = profileSettingsRepo.findByProfileIdAndProfileKey(profileId, key).orElse(new ShardProfileSettings());
		
		sps.setProfileId(profileId);
		sps.setProfileKey(key);
		sps.setProfileValue(crypto.encryptValue(value));
		
		profileSettingsRepo.save(sps);
	}
	
}