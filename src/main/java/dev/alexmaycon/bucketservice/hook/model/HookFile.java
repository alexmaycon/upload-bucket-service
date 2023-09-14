package dev.alexmaycon.bucketservice.hook.model;

public class HookFile {
	
	private String fileName;
	private String url;
	
	public HookFile() {
	}
	
	public HookFile(String fileName, String url) {
		super();
		this.fileName = fileName;
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
}
