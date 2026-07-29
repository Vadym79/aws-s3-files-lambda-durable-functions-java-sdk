package dev.vkazulkin.handler;

public class UpcomingTalksWebSearchExtractor extends AuthorContentWebSearchExtractor {

	public static final String PATH="/author/content/upcomingTalks";
	
	@Override
	protected String getPath() {
		return PATH;
	}
    
}
