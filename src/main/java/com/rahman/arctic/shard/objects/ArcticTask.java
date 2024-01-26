package com.rahman.arctic.shard.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import lombok.Getter;
import lombok.Setter;

/**
 * Thread task to be ran to create objects on OpenStack
 * @author SGT Rahman
 *
 * @param <R> Object to be obtained/created
 */
public abstract class ArcticTask<R> implements Runnable {
	
	@Getter
	private final int priority;
	
	@Getter
	private List<ArcticTask<?>> taskDependencies = new ArrayList<>();
	
	@Getter
	private List<ArcticTask<?>> children = new ArrayList<>();
	
	/**
	 * Resource to be obtained/created from Type Generics
	 */
	@Getter @Setter
	private R resource;
	
//	@Getter
//	private boolean completed = false;
	
	/**
	 * Used for Thread creation in ArcticCreator to ensure threads do not run out of order
	 */
	private final CountDownLatch cdl;
	
	/**
	 * Creates an Arctic Task
	 * @param c OpenStack Client
	 * @param priority Priority for queuing in ArcticCreator
	 */
	public ArcticTask(int priority) {
		this.priority = priority;
		cdl = new CountDownLatch(0);
	}
	
	/**
	 * Creates an Arctic Task with Dependencies
	 * @param c OpenStack Client
	 * @param priority Priority for queuing in ArcticCreator
	 * @param depends List of ArcticTasks this threads needs to be finished first
	 */
	public ArcticTask(int priority, List<ArcticTask<?>> depends) {
		this.priority = priority;
		taskDependencies = depends;
		cdl = new CountDownLatch(depends.size());
		registerChildParent();
	}

	/**
	 * Adds this thread as a child to all of its dependent tasks
	 */
	private void registerChildParent() {
		taskDependencies.forEach(e -> {
			e.getChildren().add(this);
		});
	}
	
	/**
	 * Used to countdown the CountDownLatch (cdl)
	 */
	private void notifyChildren() {
		children.forEach(e -> {
			e.cdl.countDown();
		});
	}
	
	/**
	 * Action for the thread to run
	 */
	public abstract void action();
	
	public void run() {
		// Thread sleeping for smaller tasks or when a task is dependent on another task
		try {
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		action();
		notifyChildren();
	}
	
}