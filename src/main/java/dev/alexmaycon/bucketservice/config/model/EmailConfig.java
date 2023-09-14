package dev.alexmaycon.bucketservice.config.model;

import java.util.List;

import javax.validation.constraints.Pattern;

public class EmailConfig {
	
	@Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$")
    private String sender;
	
	private List<String> recipients;
	
	private SendgridConfig sendgrid;
	
	public EmailConfig() {
	}
	
	public EmailConfig(@Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$") String sender,
			List<String> recipients, SendgridConfig sendgrid) {
		super();
		this.sender = sender;
		this.recipients = recipients;
		this.sendgrid = sendgrid;
	}
	
	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
	}

	public SendgridConfig getSendgrid() {
		return sendgrid;
	}

	public void setSendgrid(SendgridConfig sendgrid) {
		this.sendgrid = sendgrid;
	}

}
