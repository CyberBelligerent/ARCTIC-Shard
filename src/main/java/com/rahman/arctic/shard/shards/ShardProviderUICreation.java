package com.rahman.arctic.shard.shards;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.rahman.arctic.shard.ShardProviderTmpl;

import lombok.Getter;

public abstract class ShardProviderUICreation<T, ReturnedItem> {
	
	@Getter
	private T client;
	private ShardProviderTmpl<?> provider;

    public void setProvider(ShardProviderTmpl<?> provider) {
        this.provider = provider;
    }

    protected ShardProviderTmpl<?> getProvider() {
        return provider;
    }
	
    public <c> CompletableFuture<List<ReturnedItem>> initialize(T t) {
    	client = t;
    	return returnResult();
    }
    
	public abstract CompletableFuture<List<ReturnedItem>> returnResult();
	
}