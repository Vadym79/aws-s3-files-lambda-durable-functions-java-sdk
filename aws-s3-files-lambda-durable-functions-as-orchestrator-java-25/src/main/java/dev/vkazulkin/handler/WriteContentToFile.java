package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;


public class WriteContentToFile implements RequestHandler<AuthorContent, Void> {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final String WORKSPACE_MOUNT  = System.getenv("WORKSPACE_MOUNT");
	public static final Logger LOGGER = LoggerFactory.getLogger(WriteContentToFile.class);

	@Override
	public Void handleRequest(AuthorContent authorContent, Context context) {
	    LOGGER.info("invoked WriteContentToFile Lambda function with author "+authorContent);
	    LOGGER.info("author content: "+authorContent);
		var authorContentAsJson = objectMapper.writeValueAsString(authorContent);
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

}