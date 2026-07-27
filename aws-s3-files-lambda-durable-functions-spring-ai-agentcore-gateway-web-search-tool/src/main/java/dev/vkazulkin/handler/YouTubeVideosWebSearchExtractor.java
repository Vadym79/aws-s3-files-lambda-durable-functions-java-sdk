package dev.vkazulkin.handler;

public class YouTubeVideosWebSearchExtractor extends AuthorContentWebSearchExtractor {

	@Override
	protected String getPath() {
		return "/author/content/youtubeVideos";
	}
    
}
