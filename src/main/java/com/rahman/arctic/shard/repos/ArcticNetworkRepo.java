package com.rahman.arctic.shard.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.shard.objects.ArcticNetwork;

@Repository
public interface ArcticNetworkRepo extends JpaRepository<ArcticNetwork, String>{}