package eu.transkribus.solrSearch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class solrConstants {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(solrConstants.class);
	private static final String PROPERTIES_FILENAME = "solr.properties";
	private static Properties props = null;
	
	
	private static void getProperties(){
		
		
		try {
			InputStream propertiesAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILENAME);
			if(propertiesAsStream == null){				
				propertiesAsStream = new FileInputStream(PROPERTIES_FILENAME);
			}
			props = new Properties();
			props.load(propertiesAsStream);

		} 	
		catch (InvalidPropertiesFormatException e) {
			LOGGER.error("Invalid properties file: "+ PROPERTIES_FILENAME +"\n"+e.getMessage());
			e.printStackTrace();
		}		
		catch (FileNotFoundException e) {	
			LOGGER.error("Properties File not found");
			e.printStackTrace();
		}
		catch (IOException e) {
			LOGGER.error("Could not load: "+ PROPERTIES_FILENAME +"\n"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public static String getString(String name) {
		if(props==null) {
			getProperties();
		}
		return props.getProperty(name);
	}
	

}
