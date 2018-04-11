package org.fogbowcloud.manager.core.models;

public class Credential {
	private String name;
	private boolean required;
	private String valueDefault;
	
	public Credential(String name, boolean required, String valueDefault) {
		this.name = name;
		this.required = required;
		this.valueDefault = valueDefault;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getValueDefault() {
		return valueDefault;
	}
	
	public void setValueDefault(String valueDefault) {
		this.valueDefault = valueDefault;
	}

	public boolean isRequired() {
		return required;
	}
}
