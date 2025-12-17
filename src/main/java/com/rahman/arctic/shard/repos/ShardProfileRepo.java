package com.rahman.arctic.shard.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.shard.configuration.persistence.ShardProfile;

@Repository
public interface ShardProfileRepo extends JpaRepository<ShardProfile, String> {
	Optional<ShardProfile> findByUsernameAndDomain(String user, String domain);
	List<ShardProfile> findAllByUsername(String username);
}