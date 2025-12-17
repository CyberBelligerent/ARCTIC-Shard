package com.rahman.arctic.shard.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.shard.configuration.persistence.ShardProfileSettings;

@Repository
public interface ShardProfileSettingsRepo extends JpaRepository<ShardProfileSettings, String> {

	Optional<ShardProfileSettings> findByProfileIdAndProfileKey(String id, String key);
	Optional<List<ShardProfileSettings>> findAllByProfileId(String id);
	
}