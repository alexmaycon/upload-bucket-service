package dev.alexmaycon.bucketservice.config.model;

public class SendgridConfig {
	
	private String apiKey;
	
	public SendgridConfig() {
	}

	public SendgridConfig(String apiKey) {
		super();
		this.apiKey = apiKey;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}
