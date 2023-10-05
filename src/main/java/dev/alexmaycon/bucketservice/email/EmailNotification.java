package dev.alexmaycon.bucketservice.email;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import dev.alexmaycon.bucketservice.config.model.EmailConfig;

@Component
public class EmailNotification {
	
	private static final Logger logger = LoggerFactory.getLogger(EmailNotification.class);
	private final ServiceConfiguration serviceConfiguration;
	
	public EmailNotification(ServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }
	
	public void send(HashMap<String, String> directoriesPerJobConfig, JobExecution jobExecution) {
		final EmailConfig emailConfig = serviceConfiguration.getService().getEmail();

        if (emailConfig == null || emailConfig.getSendgrid() == null || emailConfig.getSendgrid().getApiKey() == null)
            return;
        
        ExecutionContext context = jobExecution.getExecutionContext();
        
        if (context == null) {
        	return;
        }
        
        HashMap<String, String> files = (HashMap<String, String>) context.get("MapFiles");
        String details = directoriesPerJobConfig.get(jobExecution.getJobInstance().getJobName());
        String dir = (String) context.get("Dir");
        
        SendGrid sg = new SendGrid(emailConfig.getSendgrid().getApiKey());
    	
    	Email from = new Email(emailConfig.getSender());
    	
	    String subject = "Backup "+dir+" - "+LocalDateTime.now().format(DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm"));
	    
	    Email to = new Email(emailConfig.getRecipients().get(0));
	    
	    StringBuilder text = new StringBuilder();
	    text.append("<html><header></header><body>");
	    text.append("<p><b>Folder backup:</b> "+dir+"</p>");
	    text.append("<p><b>Details:</b> "+details+"</p>");
	    text.append("<p><b>Date/time:</b> "+LocalDateTime.now().format(DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss"))+"</p>");
	    text.append("<p><b>Access URLS:</b></p><ul>");
	    
	    if (files != null) {
		    files.forEach((name,url) -> {
		    	if (url == null) {
		    		text.append("<li>"+name+"</li>");
		    	} else {
		    		text.append("<li><a href=\""+url+"\" target=\"_blank\">"+name+"</li>");
		    	}
		    });
	    } else {
	    	text.append("<li>No new files found.</li>");
	    }
	    
	    text.append("</ul></body></html>");
	    
	    Content content = new Content("text/html", text.toString());
	    Mail mail = new Mail(from, subject, to, content);
	    
	    if (emailConfig.getRecipients().size() > 1) {
	    	emailConfig.getRecipients().stream().skip(1).forEach(r -> mail.getPersonalization().get(0).addCc(new Email(r)));
	    }
	    
        try {
          Request request = new Request();
          request.setMethod(Method.POST);
          request.setEndpoint("mail/send");
          request.setBody(mail.build());
          Response response = sg.api(request);
          if (response.getStatusCode() >= 400) {
        	  logger.error("Error on sending e-mail ("+response.getStatusCode()+"): "+response.getBody());
          } else {
        	  logger.info("Email successfully sent.");
          }
        } catch (IOException ex) {
          logger.error(ex.toString());
        }		
	}
}
