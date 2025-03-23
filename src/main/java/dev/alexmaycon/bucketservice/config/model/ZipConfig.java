package dev.alexmaycon.bucketservice.config.model;

import javax.validation.constraints.Size;

public class ZipConfig {
	
	private boolean enabled;
	@Size(min = 8, max=200, message = "The password length must be between 8 and 200 characters.")
	private String password;
	
	public ZipConfig() {
	}

	public ZipConfig(boolean enabled, String password) {
		super();
		this.enabled = enabled;
		this.password = password;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
