package dev.vkazulkin.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.Author;
import dev.vkazulkin.entity.AuthorContent;
import dev.vkazulkin.entity.UpcomingTalks;
import dev.vkazulkin.entity.YouTubeVideo;
import dev.vkazulkin.entity.YouTubeVideos;
import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.config.CompletionConfig;
import software.amazon.lambda.durable.config.InvokeConfig;
import software.amazon.lambda.durable.config.MapConfig;
import software.amazon.lambda.durable.config.NestingType;
import software.amazon.lambda.durable.config.StepConfig;
import software.amazon.lambda.durable.config.StepSemantics;
import software.amazon.lambda.durable.retry.JitterStrategy;
import software.amazon.lambda.durable.retry.RetryStrategies;
import tools.jackson.databind.ObjectMapper;

public interface AbstractAuthorContentExtractor {
	
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public static final String WORKSPACE_MOUNT  = System.getenv("WORKSPACE_MOUNT");
	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthorContentExtractor.class);
	
	public default StepConfig getStepConfig() {
		return StepConfig.builder()
				.semanticsPerRetry(StepSemantics.AT_LEAST_ONCE_PER_RETRY)
				.retryStrategy(RetryStrategies.exponentialBackoff(
						3,                        // max attempts
						Duration.ofSeconds(2),    // initial delay  
						Duration.ofSeconds(30),   // max delay
						2.0,                      // backoff multiplier
						JitterStrategy.FULL))
				 .build();
		
	}
	public default YouTubeVideos searchForYouTubeVideos() {
		LOGGER.info("invoked searchForYouTubeVideos");
		return YouTubeVideos.getDefaultYouTubeVideos();
	}
	
	public default UpcomingTalks searchForUpcomingTalks() {
		LOGGER.info("invoked searchForUpcomingTalks");
		return UpcomingTalks.getDefaultUpcomingTalks();
	}
	
	public default Void writeAuthorContentToFile(AuthorContent authorContent) {
		LOGGER.info("invoked writeAuthorContentToFile");
		var authorContentAsJson = OBJECT_MAPPER.writeValueAsString(authorContent);
        var fileName= authorContent.author().firstName()+"-"+authorContent.author().lastName()+".json";
		var path = Paths.get(WORKSPACE_MOUNT, fileName);
		var strToBytes = authorContentAsJson.getBytes();
		LOGGER.info("saving result to: "+path);

		try {
			Files.write(path, strToBytes);
		} catch (IOException ex) {
			LOGGER.error("error wrting to the file", ex);
		}
		return null;
	}
	
	
	public default void saveYouTubeVideosToPlayList (DurableContext ctx, Author author, YouTubeVideos youtubeVideos)  {
		LOGGER.info("invoked saveYouTubeVideosToPlayList");
		
		var youtubePlayList = findOrCreateYouTubePlayListForTheAuthor(author);
		
		var mapConfig = MapConfig.builder()
				.nestingType(NestingType.NESTED)
			    .maxConcurrency(5)                                    
			    .completionConfig(CompletionConfig.allCompleted())    
			    .build();
		
		var mapResult = ctx.map("saveYouTubeVideoToPlayList-step", youtubeVideos.youtubeVideos(), 
				Boolean.class, (youtubeVideo, index, childCtx) -> {		
			return childCtx.step("saveYouTubeVideoToPlayList-step-"+index, Boolean.class, 
					        stepCtx -> saveYouTubeVideoToPlayList(youtubeVideo, youtubePlayList)
					        , this.getStepConfig());
				}, mapConfig);
		
		LOGGER.info("map overall result: "+mapResult);
		LOGGER.info("map results: "+mapResult.results());
		LOGGER.info("map result all succeeded? : "+mapResult.allSucceeded());
	}
	
	
	private static int findOrCreateYouTubePlayListForTheAuthor(Author author) {
		// Use YouTube API to log in to YouTube Account and search for the author's playlist
		// and return it if it already exists or create a new one
		// move this logic into a separate Lambda function and invoke it as a durable step
		return 1;
	}
	
	private static Boolean saveYouTubeVideoToPlayList(YouTubeVideo youtubeVideo, int playListId) {
	   LOGGER.info("invoked saveYouTubeVideoToPlayList function with youtube video:  " +youtubeVideo+ " and play list id" +playListId);
	   // Use YouTube API to save the video url to the play ist. 
	   LOGGER.info("successfully saved");
       return true;
	}
}