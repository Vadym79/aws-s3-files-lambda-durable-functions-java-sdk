
package dev.vkazulkin.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableFuture;
import software.amazon.lambda.durable.DurableHandler;
import software.amazon.lambda.durable.config.CompletionConfig;
import software.amazon.lambda.durable.config.ParallelConfig;
import software.amazon.lambda.durable.model.ParallelResult;


public class AuthorContentExtractor extends DurableHandler<Author, AuthorContent> {

	private static final String UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN  = System.getenv("UpcomingTalksWebSearchExtractorFunctionArn");
	private static final String YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN  = System.getenv("YouTubeVideosWebSearchExtractorFunctionArn");
	private static final String WRITE_CONTENT_TO_FILE_FUNCTION_ARN  = System.getenv("WriteContentToFileFunctionArn");

	public static final Logger LOGGER = LoggerFactory.getLogger(AuthorContentExtractor.class);

	
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		LOGGER.info("author "+author);
		LOGGER.info("UpcomingTalksWebSearchExtractorFunctionArn "+UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("YouTubeVideosWebSearchExtractorFunctionArn "+YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("WriteContentToFileFunctionArn "+WRITE_CONTENT_TO_FILE_FUNCTION_ARN);

		var config = ParallelConfig.builder()
				.maxConcurrency(5)
				.completionConfig(CompletionConfig.allCompleted())
				.build();

	    var parallel = ctx.parallel("parallel-search", config);

		DurableFuture<UpcomingTalks> upcomingTalksFuture = parallel.branch("searchForUpcomingTalks-parallel-step",
				UpcomingTalks.class, branchCtx -> {
			return branchCtx.invoke("searchForUpcomingTalks-step",
					UPCOMING_TALKS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN, author, UpcomingTalks.class);
		});

		DurableFuture<YouTubeVideos> youtubeVideosFuture = parallel.branch("searchForYouTubeVideos-parallel-step",
				YouTubeVideos.class, branchCtx -> {
			return branchCtx.invoke("searchForYouTubeVideos-step",
					YOUTUBE_VIDEOS_WEB_SEARCH_EXTRACTOR_FUNCTION_ARN, author, YouTubeVideos.class);
		});

		ParallelResult result = parallel.get();
		LOGGER.info("result: "+result);
		
		var upcomingTalks=upcomingTalksFuture.get();
		var youtubeVideos= youtubeVideosFuture.get();
		
		LOGGER.info("upcoming talks: " +upcomingTalks+ " youtube videos: "+youtubeVideos);
	    
		var authorContent = new AuthorContent(author, upcomingTalks, youtubeVideos);

		LOGGER.info("invoke write content to file");
		var writeToFileResult= ctx.invoke("writeToFile-step",
				WRITE_CONTENT_TO_FILE_FUNCTION_ARN, authorContent, Void.class);
		
		LOGGER.info("finished orchestration");
	    return authorContent;
	}
}