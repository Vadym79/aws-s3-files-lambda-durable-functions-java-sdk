
package dev.vkazulkin.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;
import software.amazon.lambda.durable.config.CompletionConfig;
import software.amazon.lambda.durable.config.InvokeConfig;
import software.amazon.lambda.durable.config.NestingType;
import software.amazon.lambda.durable.config.ParallelConfig;


public class AuthorContentExtractor extends DurableHandler<Author, AuthorContent> {

	private static final String UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN  = System.getenv("UpcomingTalksWebSearchExtractorFunctionArn");
	private static final String YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN  = System.getenv("YouTubeVideosWebSearchExtractorFunctionArn");
	private static final String WRITE_AUTHOR_CONTENT_TO_FILE_FUNCTION_ARN  = System.getenv("WriteAuthorContentToFileFunctionArn");

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorContentExtractor.class);

	
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		LOGGER.info("author "+author);
		LOGGER.info("UpcomingTalksWebSearchExtractorFunctionArn "+UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("YouTubeVideosWebSearchExtractorFunctionArn "+YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("WriteAuthorContentToFileFunctionArn "+WRITE_AUTHOR_CONTENT_TO_FILE_FUNCTION_ARN);

		var config = ParallelConfig.builder()
				.maxConcurrency(5)
				.nestingType(NestingType.NESTED)
				.completionConfig(CompletionConfig.allCompleted())
				.build();
		
		var invokeConfig=InvokeConfig.builder().build();

	    var parallel = ctx.parallel("parallel-web-search", config);

		var upcomingTalksFuture = parallel.branch("webSearchForUpcomingTalks-parallel-step",
				UpcomingTalks.class, branchCtx -> {
			return branchCtx.invoke("webSearchForUpcomingTalks-step",
					UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN, author, 
					UpcomingTalks.class, invokeConfig);
		});

		var youtubeVideosFuture = parallel.branch("webSearchForYouTubeVideos-parallel-step",
				YouTubeVideos.class, branchCtx -> {
			return branchCtx.invoke("webSearchForYouTubeVideos-step",
					YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN, author, 
					YouTubeVideos.class, invokeConfig);
		});

		var result = parallel.get();
		LOGGER.info("result: "+result);
		
		var upcomingTalks=upcomingTalksFuture.get();
		var youtubeVideos= youtubeVideosFuture.get();
		
		LOGGER.info("upcoming talks: " +upcomingTalks+ " youtube videos: "+youtubeVideos);
	    
		var authorContent = new AuthorContent(author, upcomingTalks, youtubeVideos);

		LOGGER.info("invoke write author content to file");
		ctx.invoke("writeAuthorContentToFile-step",
				WRITE_AUTHOR_CONTENT_TO_FILE_FUNCTION_ARN, authorContent, 
				Void.class, invokeConfig);
		
		LOGGER.info("finished orchestration");
	    return authorContent;
	}
}