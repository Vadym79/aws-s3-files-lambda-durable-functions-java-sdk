package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.Author;
import dev.vkazulkin.entity.YouTubeVideos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class YouTubeVideosExtractor implements RequestHandler<Author, YouTubeVideos> , BaseContentExtract {

	private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeVideosExtractor.class);
	
	@Override
	public YouTubeVideos handleRequest(Author author, Context context) {
		IO.println("invoked YouTubeVideosExtractor Lambda function with author "+author);
		try {
			return this.search(author, "YouTube videos", 3, YouTubeVideos.class);
		} catch (Exception e) {
			IO.println("error occured "+e.getMessage());
		}
		return null;
	}
}