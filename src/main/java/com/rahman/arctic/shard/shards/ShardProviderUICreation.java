package com.rahman.arctic.shard.shards;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.rahman.arctic.shard.ShardProviderTmpl;

public abstract class ShardProviderUICreation<T, ReturnedItem> {
	
	private ShardProviderTmpl<T> provider;
	
    public void setProvider(ShardProviderTmpl<T> provider) {
        this.provider = provider;
    }

    protected ShardProviderTmpl<T> getProvider() {
        return provider;
    }
    
	public abstract ReturnedItem returnResult(T t);
	
	public CompletableFuture<ReturnedItem> returnResults(T t, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			return returnResult(t);
		}, executor);
	}
	
}