package org.fogbowcloud.manager.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class ManagerScheduledExecutorService {
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;
	
	private static final Logger LOGGER = Logger.getLogger(ManagerScheduledExecutorService.class);

	public ManagerScheduledExecutorService(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	public void scheduleAtFixedRate(final Runnable task, long delay, long period) {
		this.future = this.executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} catch (Throwable e) {
					LOGGER.error("Failed while executing timer task", e);
				}
			}
		}, delay, period, TimeUnit.MILLISECONDS);
	}

	public void cancel() {
		if (this.future != null) {
			this.future.cancel(false);
		}
		this.future = null;
	}

	public boolean isScheduled() {
		return this.future != null && !this.future.isCancelled();
	}
}
