package dev.vkazulkin.entity;

import java.util.List;

public record YouTubeVideos(List<YouTubeVideo> youtubeVideos) {
	
	public static YouTubeVideos getDefaultYouTubeVideos() {
		var youtubeVideo1= new YouTubeVideo("Building AI Agents with Spring AI and Amazon Bedrock AgentCore", 
				"https://www.youtube.com/watch?v=JQXfSjMOa1g");
		var youtubeVideo2= new YouTubeVideo("Serverless Java applications on AWS Lambda with Micronaut, Quarkus & Spring Boot", 
				"https://www.youtube.com/watch?v=B8ko_q4g3qc");
		return new YouTubeVideos(List.of(youtubeVideo1, youtubeVideo2));
	}
}


