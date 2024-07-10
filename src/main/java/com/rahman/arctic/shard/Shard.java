package com.rahman.arctic.shard;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import lombok.Getter;

@Configuration
@EnableJpaRepositories(basePackages = {"com.rahman.arctic.shard.repos"})
@EntityScan("com.rahman.arctic.shard.objects")
public class Shard {

	@Getter
	private ShardManager shardManager;
	
	public Shard() {
		shardManager = new ShardManager("./.providers");
		System.out.println("Enabling Service: Shard");
	}
	
	@Bean
    public ShardManager shardManager() {
        return this.shardManager;
    }
	
}