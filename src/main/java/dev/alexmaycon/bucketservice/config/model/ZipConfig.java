package dev.alexmaycon.bucketservice.config.model;

public class ZipConfig {
	
	private boolean enabled;
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
