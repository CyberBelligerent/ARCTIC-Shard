package com.rahman.arctic.shard.objects;

import com.rahman.arctic.shard.exceptions.ResourceErrorException;
import com.rahman.arctic.shard.exceptions.ResourceTimeoutException;

/**
 * Interface to create the method for waiting on OpenStack resources to be created
 * @author SGT Rahman
 * 
 * @param <T> Type of client
 * @param <R> Type to be waiting on
 */
public interface Waiter<T, R> {
	/**
	 * Will stop the current thread until the resource becomes available or errors out
	 * @param client OpenStack client to obtain information about the resource
	 * @param re RangeExercise ID for sending information to
	 * @param resource Resource to be waited on
	 * @param timeInSeconds TimeOut in seconds
	 * @param pollingTimeInSeconds Time between checking on status of resource
	 * @return If the wait was successful or not
	 * @throws ResourceTimeoutException If the resource was not able to be created in time
	 * @throws ResourceErrorException If the resource had an error from OpenStack
	 */
	boolean waitUntilReady(T client, String re, R resource, int timeInSeconds, int pollingTimeInSeconds) throws ResourceTimeoutException, ResourceErrorException;
}