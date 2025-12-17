package com.rahman.arctic.shard.configuration.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class ShardProfile {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Getter
	private String id;
	
	@Getter @Setter
	private String username;
	
	@Getter @Setter
	private String profileName;
	
	@Getter @Setter
	private String domain;
	
}