
package dev.vkazulkin.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;

public class AuthorContentExtractor extends DurableHandler<Author, AuthorContent> implements AbstractAuthorContentExtractor {
	  
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorContentExtractor.class);
	
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {
		LOGGER.info("author "+author);
		var config = this.getStepConfig();
		
		var upcomingTalks= ctx.step("searchForUpcomingTalks-step", UpcomingTalks.class, stepCtx -> this.searchForUpcomingTalks(), config);
		var	youtubeVideos= ctx.step("searchForYouTubeVideos-step", YouTubeVideos.class, stepCtx -> this.searchForYouTubeVideos(), config);
			    	
		var upcomingTalkApprovalStatus = this.waitForUpcomingTalksApproval(ctx, author, upcomingTalks);
		
	    var authorContent = new AuthorContent(author, upcomingTalkApprovalStatus, youtubeVideos);
		
	    ctx.step("writeAuthorContentToFile-step", Void.class, stepCtx -> this.writeAuthorContentToFile(authorContent), config);
		
	    return authorContent;
	}
}