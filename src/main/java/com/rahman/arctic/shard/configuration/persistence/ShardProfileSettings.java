package com.rahman.arctic.shard.configuration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames = {"profile_id", "profile_key"})
})
public class ShardProfileSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;
	
	@Getter @Setter
	@Column(name = "profile_id")
	private String profileId;
	
	@Getter @Setter @Column(name = "profile_key")
	private String profileKey;
	
	@Getter @Setter @Column(name = "profile_value")
	private String profileValue;
	
	@Getter @Setter @Column(name = "profile_required")
	private boolean profileRequired;
}