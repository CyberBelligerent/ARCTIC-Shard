package com.rahman.arctic.shard.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.shard.objects.ArcticSecurityGroup;

@Repository
public interface ArcticSecurityGroupRepo extends JpaRepository<ArcticSecurityGroup, String>{}