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
		@UniqueConstraint(columnNames = {"config_domain", "config_key"})
})
public class ShardConfiguration {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;
	
	@Getter @Setter @Column(nullable = false, name = "config_domain")
	private String configDomain;
	
	@Getter @Setter @Column(nullable = false, name = "config_key")
	private String configKey;
	
	@Getter @Setter @Column(nullable = false, name = "config_required")
	private boolean configRequired;
	
	@Getter @Setter @Column(nullable = true, name = "config_type")
	private ShardConfigurationType configType = ShardConfigurationType.STRING;
	
}