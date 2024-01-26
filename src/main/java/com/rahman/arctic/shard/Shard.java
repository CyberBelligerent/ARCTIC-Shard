package com.rahman.arctic.shard;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.rahman.arctic.shard.repos"})
@EntityScan("com.rahman.arctic.shard.objects")
public class Shard {

	public Shard() {
		new ShardManager("./.providers");
		System.out.println("Enabling Service: Shard");
	}
	
}