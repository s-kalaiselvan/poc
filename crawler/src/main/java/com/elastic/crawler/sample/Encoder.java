package com.elastic.crawler.sample;

public class Encoder {
	private static final String ELASTICSEARCH_USERNAME = Messages.getString("crawler.elasticsearch.username");
	private static final String ELASTICSEARCH_PASSWORD = Messages.getString("crawler.elasticsearch.password");

	public static void main(String[] args) {
		String base64Auth = java.util.Base64.getEncoder().encodeToString((ELASTICSEARCH_USERNAME + ":" + ELASTICSEARCH_PASSWORD).getBytes());
		System.out.println("Basic " + base64Auth);
	}
}
