package com.rahman.arctic.shard.exceptions;

/**
 * More defined error for when a resource times out when trying to make
 * @author SGT Rahman
 *
 */
public class ResourceTimeoutException extends Exception {

	private static final long serialVersionUID = -5090840106025554312L;

	public ResourceTimeoutException(String message) {
		super(message);
	}
	
}