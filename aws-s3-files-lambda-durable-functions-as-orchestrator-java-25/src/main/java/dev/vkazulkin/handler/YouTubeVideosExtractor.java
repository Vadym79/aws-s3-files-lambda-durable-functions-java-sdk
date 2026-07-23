package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.Author;
import dev.vkazulkin.entity.YouTubeVideos;
import dev.vkazulkin.entity.YouTubeVideo;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class YouTubeVideosExtractor
		implements RequestHandler<Author, YouTubeVideos> {

	public static final Logger LOGGER = LoggerFactory.getLogger(YouTubeVideosExtractor.class);
	
	@Override
	public YouTubeVideos handleRequest(Author author, Context context) {
		LOGGER.info("invoked YouTubeVideosExtractor Lambda function with author "+author);
		return this.searchForYouTubeVideos();
	}

	private YouTubeVideos searchForYouTubeVideos() {
		var youtubeVideo1= new YouTubeVideo("Building AI Agents with Spring AI and Amazon Bedrock AgentCore",
				"https://www.youtube.com/watch?v=JQXfSjMOa1g");
		return new YouTubeVideos(Set.of(youtubeVideo1));
	}

}