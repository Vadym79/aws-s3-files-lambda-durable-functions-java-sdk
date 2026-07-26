
package dev.vkazulkin.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;


public class AsyncAuthorContentExtractor extends DurableHandler<Author, AuthorContent> implements AbstractAuthorContentExtractor {
	  
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncAuthorContentExtractor.class);
	
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		LOGGER.info("author "+author);
		var config = this.getStepConfig();
		
		var upcomingTalksFuture= ctx.stepAsync("searchForUpcomingTalks-async-step", UpcomingTalks.class, stepCtx -> this.searchForUpcomingTalks(), config);
		var	youtubeVideosFuture= ctx.stepAsync("searchForYouTubeVideos-async-step", YouTubeVideos.class, stepCtx -> this.searchForYouTubeVideos(), config);
		
		var upcomingTalks=upcomingTalksFuture.get();
		var youtubeVideos= youtubeVideosFuture.get();
	    
	    var authorContent = new AuthorContent(author, upcomingTalks, youtubeVideos);
		
	    ctx.step("writeAuthorContentToFile-step", Void.class, stepCtx -> this.writeAuthorContentToFile(authorContent), config);	
	    return authorContent;
	}
}