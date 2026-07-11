package dev.vkazulkin.entity;

import java.time.LocalDate;
import java.util.Set;

public record UpcomingTalks(Set<UpcomingTalk> upcomingTalks) {
	
	public static UpcomingTalks getDefaultUpcomingTalks() {
		var upcomingTalk1= new UpcomingTalk("Building AI Agents with Spring AI and Amazon Bedrock AgentCore",
			LocalDate.of(2026, 6, 13),"https://www.meetup.com/aws-user-group-dusseldorf/events/315327513");
		return new UpcomingTalks(Set.of(upcomingTalk1));
	}
}


