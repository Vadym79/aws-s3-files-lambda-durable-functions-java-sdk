package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpcomingTalksExtractor implements RequestHandler<Author, UpcomingTalks>, BaseContentExtractor {
	
   private static final Logger LOGGER = LoggerFactory.getLogger(UpcomingTalksExtractor.class);
   
	@Override
	public UpcomingTalks handleRequest(Author author, Context context) {
		LOGGER.info("invoked UpcomingTalksExtractor Lambda function with author "+author);
		try {
			return this.search(author, "upcoming talks", 3, UpcomingTalks.class);
		} catch (Exception e) {
			LOGGER.error("error occured "+e.getMessage());
		}
		return null;
	}
}