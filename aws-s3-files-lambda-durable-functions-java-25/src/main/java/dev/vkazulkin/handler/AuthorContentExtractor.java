
package dev.vkazulkin.handler;

import java.time.Duration;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;
import software.amazon.lambda.durable.config.CallbackConfig;
import software.amazon.lambda.durable.config.WaitForCallbackConfig;


public class AuthorContentExtractor extends DurableHandler<Author, AuthorContent> implements AbstractAuthorContentExtractor {
	  
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		ctx.getLogger().info("author "+author);
		var config = this.getStepConfig();
		
		var upcomingTalks= ctx.step("searchForUpcomingTalks-step", UpcomingTalks.class, stepCtx -> this.searchForUpcomingTalks(), config);
		var	youtubeVideos= ctx.step("searchForYouTubeVideos-step", YouTubeVideos.class, stepCtx -> this.searchForYouTubeVideos(), config);
		
	    var authorContent = new AuthorContent(author, upcomingTalks, youtubeVideos);
		this.writeAuthorContentToFile(authorContent, ctx.getLogger());
		
		
		var waitCallback= WaitForCallbackConfig.builder()
	    .callbackConfig(CallbackConfig.builder().timeout(Duration.ofHours(1)).build()) // optional
	    .build();
	    
		this.waitForUpcomingTalksApproval(ctx, author, upcomingTalks);
	    return authorContent;
	}
		
}