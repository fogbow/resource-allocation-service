package org.fogbowcloud.manager.core.utils;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public class SshClientPool {
	
	// from the old class ManagerController in fogbow-manager
	private static final int DEFAULT_MAX_POOL = 200;
	
	protected final long TIMEOUT = 20000; // 20 seconds
	private final long DEFAULT_SCHEDULER_PERIOD = 300000; // 5 minutes
	
	private Map<String, SSHConnection> pool = new HashMap<String, SSHConnection>();
	private final ManagerTimer sshConnectionSchedulerTimer;
	private DateUtils dateUtils;
	private Semaphore semaphore;
	private SSHClientFactory clientFactory = new SSHClientFactory();
	
	public SshClientPool() {
		this(new DateUtils());
	}

	public SshClientPool(DateUtils dateUtils) {
		this.sshConnectionSchedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
		this.semaphore = new Semaphore(DEFAULT_MAX_POOL);
		this.dateUtils = dateUtils;
	}
	
	public SSHClient getClient(String address, String sshUser, String sshPrivateKeyPath) throws Exception {
		SSHConnection sshConnection = pool.get(address);
		if (sshConnection == null) {
			semaphore.acquire(1);
			SSHClient client = clientFactory.createSshClient();
			sshConnection = new SSHConnection(client, dateUtils.currentTimeMillis());
			pool.put(address, sshConnection);
		}
		
		SSHClient client = sshConnection.getSshClient();
		if (!client.isConnected()) {
			String[] sshAddressAndPort = address.split(":");
			client.connect(sshAddressAndPort[0], Integer.parseInt(sshAddressAndPort[1]));
		}
		if (!client.isAuthenticated()) {
			client.authPublickey(sshUser, sshPrivateKeyPath);
		}
		if (!sshConnectionSchedulerTimer.isScheduled()) {
			triggerSshConnectionPoolScheduler();
		}	
		return client;
	}
	
	protected void triggerSshConnectionPoolScheduler() {
		sshConnectionSchedulerTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				removeTimedoutSSHConnection();
			}
		}, 0, DEFAULT_SCHEDULER_PERIOD);
	}
	
	protected void removeTimedoutSSHConnection() {
		if (pool == null) {
			return;
		}
		
		Set<String> keySet = new HashSet<String>(pool.keySet());
		for (String key : keySet) {
			if (pool.get(key).getTimestamp() + TIMEOUT < dateUtils.currentTimeMillis()) {
				pool.remove(key);
				semaphore.release();
			}
		}
		
		if (pool.isEmpty()) {
			sshConnectionSchedulerTimer.cancel();
		}
	}
	
	protected void setClientFactory(SSHClientFactory sshClientFactory) {
		this.clientFactory = sshClientFactory;
	}
	
	protected void setSemaphore(Semaphore semaphore) {
		this.semaphore = semaphore;
	}
	
	protected void setPool(Map<String, SSHConnection> pool) {
		this.pool = pool;
	}
	
	protected Map<String, SSHConnection> getPool() {
		return pool;
	}
	
	protected void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}
	
	public ManagerTimer getSshConnectionSchedulerTimer() {
		return sshConnectionSchedulerTimer;
	}
	
	protected class SSHClientFactory {
		
		public SSHClient createSshClient() {
			SSHClient client = new SSHClient();
			client.setConnectTimeout(10);
			addBlankHostKeyVerifier(client);
			return client;
		}
		
		private void addBlankHostKeyVerifier(SSHClient ssh) {		
	        ssh.addHostKeyVerifier(new HostKeyVerifier() {
	            @Override
	            public boolean verify(String arg0, int arg1, PublicKey arg2) {
	                return true;
	            }
	        });
	    }
	}
	
	protected class SSHConnection {
		
		private SSHClient sshClient;
		private long timestamp;
		
		public SSHConnection(SSHClient sshClient, long timestamp) {
			this.sshClient = sshClient;
			this.timestamp = timestamp;
		}

		public SSHClient getSshClient() {
			return sshClient;
		}

		public long getTimestamp() {
			return timestamp;
		}		
	}
}
