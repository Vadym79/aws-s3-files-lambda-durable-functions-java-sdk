package dev.vkazulkin.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.vkazulkin.entity.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;


public class WriteAuthorContentToFile implements RequestHandler<AuthorContent, Void> {
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String WORKSPACE_MOUNT  = System.getenv("WORKSPACE_MOUNT");
	private static final Logger LOGGER = LoggerFactory.getLogger(WriteAuthorContentToFile.class);

	@Override
	public Void handleRequest(AuthorContent authorContent, Context context) {
	    LOGGER.info("invoked WriteContentToFile Lambda function with author "+authorContent);
	    LOGGER.info("author content: "+authorContent);
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

}