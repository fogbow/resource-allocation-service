package org.fogbowcloud.manager.core.plugins;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

public class PluginHelper {

	public static final int PORT_ENDPOINT = getAvailablePort();
	public static final String CIRROS_IMAGE_TERM = "cadf2e29-7216-4a5e-9364-cf6513d5f1fd";
	public static final String LINUX_X86_TERM = "linuxx86";
	public static final String COMPUTE_OCCI_URL = "http://localhost:" + PORT_ENDPOINT;
	public static final String COMPUTE_NOVAV2_URL = "http://localhost:" + PORT_ENDPOINT;
	public static final String NETWORK_NOVAV2_URL = "http://localhost:" + PORT_ENDPOINT;

	public static final String ACCESS_ID = "HgfugGJHgJgHJGjGJgJg-857GHGYHjhHjH";
	public static final String TENANT_ID = "fc394f2ab2df4114bde39905f800dc57";
	public static final String TENANT_NAME = "admin";

	public static final String USER_ID = "user_id";
	public static final String USERNAME = "admin";
	public static final String USER_PASS = "reverse";

	public static final String FED_USERNAME = "federation_user";
	public static final String FED_USER_PASS = "federation_user_pass";
	
	/**
	 * Getting a available port on range 60000:61000
	 * @return
	 */
	public static int getAvailablePort() {		
		int initialP = 60000;
		int finalP = 61000;
		for (int i = initialP; i < finalP; i++) {
			int port = new Random().nextInt(finalP - initialP) + initialP;
			ServerSocket ss = null;
			DatagramSocket ds = null;
			try {
				ss = new ServerSocket(port);
				ss.setReuseAddress(true);
				ds = new DatagramSocket(port);
				ds.setReuseAddress(true);
				return port;
			} catch (IOException e) {
			} finally {
				if (ds != null) {
					ds.close();
				}
				if (ss != null) {
					try {
						ss.close();
					} catch (IOException e) {
						/* should not be thrown */
					}
				}
			}
		}		
		return -1;
	}

}