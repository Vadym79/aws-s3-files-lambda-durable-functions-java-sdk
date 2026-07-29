package dev.vkazulkin.handler;

public class YouTubeVideosWebSearchExtractor extends AuthorContentWebSearchExtractor {

	public static final String PATH="/author/content/youtubeVideos";
	
	@Override
	protected String getPath() {
		return PATH;
	}
    
}
