
package dev.vkazulkin.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.*;

import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.DurableHandler;
import software.amazon.lambda.durable.config.CompletionConfig;
import software.amazon.lambda.durable.config.ParallelConfig;


public class AuthorContentExtractor extends DurableHandler<Author, AuthorContent> {

	private static final String UPCOMING_TALKS_EXTRACTOR_FUNCTION_ARN  = System.getenv("UpcomingTalksExtractorFunctionArn");
	private static final String YOUTUBE_VIDEOS_EXTRACTOR_FUNCTION_ARN  = System.getenv("YouTubeVideosExtractorFunctionArn");
	private static final String WRITE_CONTENT_TO_FILE_FUNCTION_ARN  = System.getenv("WriteContentToFileFunctionArn");

	public static final Logger LOGGER = LoggerFactory.getLogger(AuthorContentExtractor.class);

	
	@Override
	public AuthorContent handleRequest(Author author, DurableContext ctx) {

		LOGGER.info("author "+author);
		LOGGER.info("UpcomingTalksExtractorFunctionArn "+UPCOMING_TALKS_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("YouTubeVideosExtractorFunctionArn "+YOUTUBE_VIDEOS_EXTRACTOR_FUNCTION_ARN);
		LOGGER.info("WriteContentToFileFunctionArn "+WRITE_CONTENT_TO_FILE_FUNCTION_ARN);

		var config = ParallelConfig.builder()
				.maxConcurrency(5)
				.completionConfig(CompletionConfig.allCompleted())
				.build();

	    var parallel = ctx.parallel("parallel-search", config);

		var upcomingTalksFuture = parallel.branch("searchForUpcomingTalks-parallel-step",
				UpcomingTalks.class, branchCtx -> {
			return branchCtx.invoke("searchForUpcomingTalks-step",
					UPCOMING_TALKS_EXTRACTOR_FUNCTION_ARN, author, UpcomingTalks.class);
		});

		var youtubeVideosFuture = parallel.branch("searchForYouTubeVideos-parallel-step",
				YouTubeVideos.class, branchCtx -> {
			return branchCtx.invoke("searchForYouTubeVideos-step",
					YOUTUBE_VIDEOS_EXTRACTOR_FUNCTION_ARN, author, YouTubeVideos.class);
		});

		var result = parallel.get();
		LOGGER.info("result: "+result);
		
		var upcomingTalks=upcomingTalksFuture.get();
		var youtubeVideos= youtubeVideosFuture.get();
		
		LOGGER.info("upcoming talks: " +upcomingTalks+ " youtube videos: "+youtubeVideos);
	    
		var authorContent = new AuthorContent(author, upcomingTalks, youtubeVideos);

		LOGGER.info("invoke write content to file");
		ctx.invoke("writeToFile-step",
				WRITE_CONTENT_TO_FILE_FUNCTION_ARN, authorContent, Void.class);
		
		LOGGER.info("finished orchestration");
	    return authorContent;
	}
}