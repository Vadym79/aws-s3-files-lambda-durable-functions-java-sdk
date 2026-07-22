package dev.vkazulkin.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.Author;
import dev.vkazulkin.entity.AuthorContent;
import dev.vkazulkin.entity.UpcomingTalkApprovalStatus;
import dev.vkazulkin.entity.UpcomingTalks;
import dev.vkazulkin.entity.YouTubeVideos;
import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.config.CallbackConfig;
import software.amazon.lambda.durable.config.StepConfig;
import software.amazon.lambda.durable.config.StepSemantics;
import software.amazon.lambda.durable.config.WaitForCallbackConfig;
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
		Path path = Paths.get(WORKSPACE_MOUNT, fileName);
		byte[] strToBytes = authorContentAsJson.getBytes();
		LOGGER.info("saving result to: "+path);

		try {
			Files.write(path, strToBytes);
		} catch (IOException ex) {
			LOGGER.error("error wrting to the file", ex);
		}
		return null;
	}
	
	public default UpcomingTalkApprovalStatus waitForUpcomingTalksApproval(DurableContext ctx, Author author, UpcomingTalks upcomingTalks) {
		LOGGER.info("invoked waitForUpcomingTalksApproval");
	    var waitCallbackConfig= WaitForCallbackConfig.builder()	
	    	  .callbackConfig(CallbackConfig.builder().timeout(Duration.ofHours(1))
	    	  .build())
			  .build();
			    
		var response= ctx.waitForCallback(
	                "wait-for-approval",
	                UpcomingTalkApprovalStatus.class,
	                (callbackId, stepCtx) -> this.sendApprovalRequest(callbackId, author, upcomingTalks),
	                waitCallbackConfig);
         
		LOGGER.info("received callback response "+response);
		
		return response;

	}
	
	private void sendApprovalRequest(String callbackId, Author author, UpcomingTalks upcomingTalks) {
		LOGGER.info("get approval for the talk of  "+ author+ " for the talks "+upcomingTalks+
				" with the callback id "+callbackId);
	}
}