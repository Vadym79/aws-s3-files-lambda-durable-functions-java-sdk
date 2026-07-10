
package dev.vkazulkin.handler;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;


public class AsyncAuthorContentExtractor extends DurableHandler<Author, AuthorContent> implements AbstractAuthorContentExtractor {
	  
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		ctx.getLogger().info("author "+author);
		var config = this.getStepConfig();
		
		var upcomingTalksFuture= ctx.stepAsync("searchForUpcomingTalks-async-step", UpcomingTalks.class, stepCtx -> this.searchForUpcomingTalks(), config);
		var	youtubeVideosFuture= ctx.stepAsync("searchForYouTubeVideos-async-step", YouTubeVideos.class, stepCtx -> this.searchForYouTubeVideos(), config);
		
	    var authorContent = new AuthorContent(author, upcomingTalksFuture.get(), youtubeVideosFuture.get());
		this.writeAuthorContentToFile(authorContent, ctx.getLogger());
	    return authorContent;
	}

}