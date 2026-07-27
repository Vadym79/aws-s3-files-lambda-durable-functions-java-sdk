package dev.vkazulkin.handler;

public class UpcomingTalksWebSearchExtractor extends AuthorContentWebSearchExtractor {

	@Override
	protected String getPath() {
		return "/author/content/upcomingTalks";
	}
    
}
