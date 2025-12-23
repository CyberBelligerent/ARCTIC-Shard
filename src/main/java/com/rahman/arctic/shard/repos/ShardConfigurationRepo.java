package com.rahman.arctic.shard.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.shard.configuration.persistence.ShardConfiguration;

@Repository
public interface ShardConfigurationRepo extends JpaRepository<ShardConfiguration, String> {
	public Optional<ShardConfiguration> findByConfigDomainAndConfigKey(String configDomain, String configKey);
	public Optional<List<ShardConfiguration>> findAllByConfigDomain(String configDomain);
}