package dev.vkazulkin.entity;

import java.util.Set;

public record YouTubeVideos(Set<YouTubeVideo> youtubeVideos) {
	
	public static YouTubeVideos getDefaultYouTubeVideos() {
		var youtubeVideo1= new YouTubeVideo("Building AI Agents with Spring AI and Amazon Bedrock AgentCore", 
				"https://www.youtube.com/watch?v=JQXfSjMOa1g");
		return new YouTubeVideos(Set.of(youtubeVideo1));
	}
}


