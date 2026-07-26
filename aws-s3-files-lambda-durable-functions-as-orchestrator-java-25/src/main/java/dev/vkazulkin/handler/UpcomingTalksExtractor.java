package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.*;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.UpcomingTalk;
import dev.vkazulkin.entity.UpcomingTalks;


public class UpcomingTalksExtractor implements RequestHandler<Author, UpcomingTalks> {
	
   private static final Logger LOGGER = LoggerFactory.getLogger(UpcomingTalksExtractor.class);

	@Override
	public UpcomingTalks handleRequest(Author author, Context context) {
		LOGGER.info("invoked UpcomingTalksExtractor Lambda function with author "+author);
		return this.searchForUpcomingTalks();
	}

	private UpcomingTalks searchForUpcomingTalks() {
		var upcomingTalk1= new UpcomingTalk("Building AI Agents with Spring AI and Amazon Bedrock AgentCore",
				LocalDate.of(2026, 6, 13),"https://www.meetup.com/aws-user-group-dusseldorf/events/315327513");
		return new UpcomingTalks(Set.of(upcomingTalk1));
	}
}