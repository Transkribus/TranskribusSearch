package eu.transkribus.solrSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.enums.SearchType;
import eu.transkribus.core.model.beans.searchresult.KeywordHit;
import eu.transkribus.core.model.beans.searchresult.KeywordSearchResult;

public class KeywordSearcher {
	
	private final String serverUrl;
	private static SolrClient server;
	private static final Logger LOGGER = LoggerFactory.getLogger(TrpSearcher.class);
	private static final String BAD_SYMBOLS = "{,},(,[,+,-,:,=,],),#";
	
	public KeywordSearcher(final String serverUrl) {
		if (serverUrl == null || serverUrl.isEmpty())
			throw new IllegalArgumentException("ServerUrl must not be empty!");
		// serverUrl = solrConstants.getString("solrUrl"); //Get server url from
		// properties file
		this.serverUrl = serverUrl;
		server = getSolrClient();
		LOGGER.info("Instance of TrpSearcher was created.");

	}
	
	
	public KeywordSearchResult searchKeyword(String keyword, float probLow, float probHigh, List<Integer> colIds,
			List<String> filters, int start, int rows){
		
		QueryResponse response = new QueryResponse();

		SolrQuery query = buildQuery(keyword, probLow, probHigh, colIds, filters, start, rows);
		
		
		
		if(server == null) {
			server = getSolrClient();
		}		
		try {
			response = server.query(query);
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		return generateKWSearchResult(response);
	}
	
	
	private KeywordSearchResult generateKWSearchResult(QueryResponse response) {
		
		KeywordSearchResult result = new KeywordSearchResult();
		result.setParams(response.getHeader().get("params").toString());		
		result.setNumResults(response.getResults().getNumFound());	
		result.setKeywordHits(generateKeywordHits(response));
		
		
		return result;
	}


	private ArrayList<KeywordHit> generateKeywordHits(QueryResponse response) {
	
		ArrayList<KeywordHit> kwHits = new ArrayList<>();
		
		
//		System.out.println(response.getResults().get(0).getChildDocuments());
		
		
		for(SolrDocument solrDoc: response.getResults()){

			KeywordHit kwHit = new KeywordHit();
			kwHit.setId(solrDoc.getFieldValue("id").toString());
			kwHit.setDocTitle(solrDoc.getFieldValue("title").toString());
			String word = solrDoc.getChildDocuments().get(0).getFieldValue("word").toString();
			float probability = (float) solrDoc.getChildDocuments().get(0).getFieldValue("probability");
			kwHit.setWord(word);
			kwHit.setProbability(probability);
			kwHits.add(kwHit);
		}
		
		return kwHits;
	}


	private SolrQuery buildQuery(String keyword, float probLow, float probHigh, List<Integer> colIds,
			List<String> filters, int start, int rows) {
		
		
		SolrQuery query = new SolrQuery();
		
		String userRightsFilter = "";
		int counter = 0;
		if (colIds == null) {
			userRightsFilter = "";
		} else {
			for (int i : colIds) {
				userRightsFilter += "collectionId:" + i + " ";
				counter++;
				if (counter < colIds.size())
					userRightsFilter += "OR ";
			}
		}
		
		query.setFilterQueries(userRightsFilter);
		
		String queryString = "";
		queryString += "{!parent which=type_s:parent}";
		queryString += String.format("(word:%s AND probability:[%s TO %s])", keyword, probLow, probHigh);		
		query.set("q", queryString);	
		
		String flString = "";
		flString += String.format(" *,[child parentFilter=type_s:parent childFilter='word:%s AND type_s:child AND probability:[%s TO %s]' ] ", keyword, probLow, probHigh);
		
		
		query.set("fl", flString);
		
		return query;
		
	}
	
	
	
	
	// Set connection to solr
	private SolrClient getSolrClient() {

		// SolrClient solr = new ConcurrentUpdateSolrClient(serverUrl, 20, 3);
		SolrClient solr = new HttpSolrClient.Builder(serverUrl).build();
		return solr;
	}

}
