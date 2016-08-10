package eu.transkribus.solrSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.WordType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.solrSearch.util.IndexTextUtils;

public class TrpSearcher {
	
	private final String serverUrl; 
	private static SolrClient server;
	private static final Logger LOGGER = LoggerFactory.getLogger(TrpSearcher.class);
	
	public TrpSearcher(final String serverUrl) {
		if(serverUrl == null || serverUrl.isEmpty())
			throw new IllegalArgumentException("ServerUrl must not be empty!");
		//serverUrl = solrConstants.getString("solrUrl"); //Get server url from properties file
		this.serverUrl = serverUrl;
		server = getSolrClient();
		LOGGER.info("Instance of TrpSearcher was created.");		
	}	
	
	public QueryResponse search(Map<String,String> searchMap, ArrayList<Integer> colIds){
		
		QueryResponse result = new QueryResponse();
		SolrQuery query = new SolrQuery();
		
		String collectionString = "";
		int counter = 0;
		if(colIds == null){
			collectionString = "collectionId:*";
		} else {
			for(int i : colIds){
				collectionString += "collectionId:"+i+" ";
				counter++;
				if(counter<colIds.size())
					collectionString += "OR ";				
			}
		}
		
		String queryString = "";		
		boolean andCheck = false;
		
		for(String key : searchMap.keySet()){
			
			if(!searchMap.get(key).isEmpty() && andCheck == true){
				queryString += " AND " + key + ":" + searchMap.get(key);
			}
			
			if(!searchMap.get(key).isEmpty() && andCheck == false){
				queryString += key + ":" + searchMap.get(key);
				andCheck = true;
			}

		}	
		
		query.set("q", queryString);
		query.set("fq", collectionString);

		
		boolean highlighting = false;
		for(String key : searchMap.keySet()){
			if(key.contains("fullText")){
				highlighting = true;
			}
		}
		
		if(highlighting){
			query.set("hl", "on");
			
			query.set("hl.fl", "fullText*");
			query.set("hl.snippets", "20");
		}
		
		System.out.println(queryString);		
		
		LOGGER.info("q: "+queryString+ ", fq: "+collectionString);		
		
		try {
			result = server.query(query);
		} catch (SolrServerException | IOException e) {
			
			e.printStackTrace();
		}		
		
		return result;
	}
	
	
		
	//Set connection to solr
	private SolrClient getSolrClient(){
		
		//SolrClient solr = new ConcurrentUpdateSolrClient(serverUrl, 20, 3);	
		SolrClient solr = new HttpSolrClient.Builder(serverUrl).build();
		return solr;
	}
	
	
	//find tagged stuff
	private static final Pattern TAG_REGEX = Pattern.compile("<em>(.+?)</em>");

	public static List<String> getTagValues(final String str) {
	    final List<String> tagValues = new ArrayList<String>();
	    final Matcher matcher = TAG_REGEX.matcher(str);
	    while (matcher.find()) {
	        tagValues.add(matcher.group(1));
	    }
	    return tagValues;
	}	

}
