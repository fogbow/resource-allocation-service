package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputePlugin;

public class OpenNebulaComputePlugin implements ComputePlugin<Token>{

	@Override
	public String requestInstance(ComputeOrder computeOrder, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ComputeInstance getInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
	}

}
