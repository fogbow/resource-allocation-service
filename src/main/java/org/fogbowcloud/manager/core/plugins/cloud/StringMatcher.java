package org.fogbowcloud.manager.core.plugins.cloud;

import org.mockito.ArgumentMatcher;

public class StringMatcher extends ArgumentMatcher<Object>{
	
	private Object object;
	
	public StringMatcher(Object object) {
		this.object = object;
	}
	
	@Override
	public boolean matches(Object argument) {
		return object.equals(argument.toString());
	}

}
