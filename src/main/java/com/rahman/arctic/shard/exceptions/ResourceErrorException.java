package com.rahman.arctic.shard.exceptions;

/**
 * More defined error for creating a resource from OpenStack
 * @author SGT Rahman
 *
 */
public class ResourceErrorException extends Exception {

	private static final long serialVersionUID = -5705689080746914999L;

	public ResourceErrorException(String message) {
		super(message);
	}
	
}