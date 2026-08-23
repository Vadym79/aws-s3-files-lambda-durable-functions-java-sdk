package dev.vkazulkin.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.Author;
import dev.vkazulkin.entity.AuthorContent;
import dev.vkazulkin.entity.UpcomingTalkApprovalStatus;
import dev.vkazulkin.entity.UpcomingTalks;
import dev.vkazulkin.entity.YouTubeVideos;
import software.amazon.lambda.durable.DurableContext;
import software.amazon.lambda.durable.config.*;
import software.amazon.lambda.durable.model.WaitForConditionResult;
import software.amazon.lambda.durable.retry.JitterStrategy;
import software.amazon.lambda.durable.retry.RetryStrategies;
import software.amazon.lambda.durable.retry.WaitForConditionWaitStrategy;
import software.amazon.lambda.durable.retry.WaitStrategies;
import tools.jackson.databind.ObjectMapper;

public interface AbstractAuthorContentExtractor {
	
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public static final String WORKSPACE_MOUNT  = System.getenv("WORKSPACE_MOUNT");
	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthorContentExtractor.class);
	public static final Random RANDOM= new Random();

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
	
	public default UpcomingTalkApprovalStatus waitForUpcomingTalksApproval(DurableContext ctx, Author author, UpcomingTalks upcomingTalks) {
		LOGGER.info("invoked waitForUpcomingTalksApproval");

		var config = WaitForConditionConfig.<UpcomingTalkApprovalStatus>builder()
				.waitStrategy(WaitStrategies.defaultStrategy())
				.initialState(new UpcomingTalkApprovalStatus(upcomingTalks, UpcomingTalkApprovalStatus.Status.PENDING))
				.build();

		return ctx.waitForCondition(
				"wait-for-approval",
				UpcomingTalkApprovalStatus.class,
				(currentStatus, stepCtx) -> {
					var latest = this.getUpcomingTalkApprovalStatus(author,upcomingTalks);
					return UpcomingTalkApprovalStatus.Status.APPROVED.equals(latest.status())
							? WaitForConditionResult.stopPolling(latest)
							: WaitForConditionResult.continuePolling(latest);
				},
				config);

	   	}

	/**
	 * Simulates polling: 75% cases the talks are not approved, in 25% -approved  
	 * 
	 * @param author
	 * @param upcomingTalks
	 * @return
	 */
	private UpcomingTalkApprovalStatus getUpcomingTalkApprovalStatus(Author author, UpcomingTalks upcomingTalks) {
		// Generate random integers in range 0 to 3
		int r = RANDOM.nextInt(4);
		LOGGER.info("generate random number: "+r);
		return r < 3? new UpcomingTalkApprovalStatus(upcomingTalks, UpcomingTalkApprovalStatus.Status.NOT_APPROVED) :
				new UpcomingTalkApprovalStatus(upcomingTalks, UpcomingTalkApprovalStatus.Status.APPROVED);
	}
}