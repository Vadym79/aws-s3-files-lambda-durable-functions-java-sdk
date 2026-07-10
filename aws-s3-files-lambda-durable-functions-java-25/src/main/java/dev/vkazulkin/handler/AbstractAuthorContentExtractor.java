package dev.vkazulkin.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import dev.vkazulkin.entity.AuthorContent;
import dev.vkazulkin.entity.UpcomingTalk;
import dev.vkazulkin.entity.UpcomingTalks;
import dev.vkazulkin.entity.YouTubeVideo;
import dev.vkazulkin.entity.YouTubeVideos;
import software.amazon.lambda.durable.config.StepConfig;
import software.amazon.lambda.durable.logging.DurableLogger;
import software.amazon.lambda.durable.retry.JitterStrategy;
import software.amazon.lambda.durable.retry.RetryStrategies;
import tools.jackson.databind.ObjectMapper;

public interface AbstractAuthorContentExtractor {
	
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public static final String WORKSPACE_MOUNT  = System.getenv("WORKSPACE_MOUNT");
	
	public default StepConfig getStepConfig() {
		return StepConfig.builder()
				.retryStrategy(RetryStrategies.exponentialBackoff(
						3,                        // max attempts
						Duration.ofSeconds(2),    // initial delay  
						Duration.ofSeconds(30),   // max delay
						2.0,                      // backoff multiplier
						JitterStrategy.FULL))
				 .build();
		
	}
	public default YouTubeVideos searchForYouTubeVideos() {
		var youtubeVideo1= new YouTubeVideo("Building AI Agents with Spring AI and Amazon Bedrock AgentCore", 
				"https://www.youtube.com/watch?v=JQXfSjMOa1g");
		return new YouTubeVideos(Set.of(youtubeVideo1));
	}
	
	public default UpcomingTalks searchForUpcomingTalks() {
		var upcomingTalk1= new UpcomingTalk("Building AI Agents with Spring AI and Amazon Bedrock AgentCore",
			LocalDate.of(2026, 6, 13),"https://www.meetup.com/aws-user-group-dusseldorf/events/315327513");
		return new UpcomingTalks(Set.of(upcomingTalk1));
	}
	
	public default void writeAuthorContentToFile(AuthorContent authorContent, DurableLogger logger) {
		var authorContentAsJson = OBJECT_MAPPER.writeValueAsString(authorContent);
        var fileName= authorContent.author().firstName()+"-"+authorContent.author().lastName()+".json";
		Path path = Paths.get(WORKSPACE_MOUNT, fileName);
		byte[] strToBytes = authorContentAsJson.getBytes();
		logger.info("saving result to: "+path);

		try {
			Files.write(path, strToBytes);
		} catch (IOException ex) {
			logger.error("error wrting to the file", ex);
		}
	}
}