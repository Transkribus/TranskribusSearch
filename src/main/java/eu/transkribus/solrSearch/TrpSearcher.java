package eu.transkribus.solrSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.enums.SearchType;
import eu.transkribus.core.model.beans.searchresult.FulltextSearchResult;
import eu.transkribus.core.model.beans.searchresult.PageHit;
import eu.transkribus.solrSearch.util.SearchUtils;

public class TrpSearcher {

	private final String serverUrl;
	private static SolrClient server;
	private static final Logger LOGGER = LoggerFactory.getLogger(TrpSearcher.class);

	// public final static String WORDS = "Words";
	// public final static String WORDSLC = "WordsLc";
	// public final static String LINES = "Lines";
	// public final static String LINESLC = "LinesLc";

	public TrpSearcher(final String serverUrl) {
		if (serverUrl == null || serverUrl.isEmpty())
			throw new IllegalArgumentException("ServerUrl must not be empty!");
		// serverUrl = solrConstants.getString("solrUrl"); //Get server url from
		// properties file
		this.serverUrl = serverUrl;
		server = getSolrClient();
		LOGGER.info("Instance of TrpSearcher was created.");

	}

	/**
	 * FullText search without additional filters
	 * 
	 * @param searchText
	 *            Text to search for
	 * @param TYPE
	 *            WORDS or WORDSLC or LINES or LINESLC
	 * @param colIds
	 *            Filters by colIds
	 * @return Solr QueryResponse
	 */
	public FulltextSearchResult searchFullText(String searchText, SearchType TYPE, ArrayList<Integer> colIds, int start, int rows) {
		return searchFullText(searchText, TYPE, colIds, null, start, rows);
	}

	/**
	 * FullText Search
	 * 
	 * @param searchText
	 *            text to search for
	 * @param TYPE
	 * @param colIds
	 * @param filters
	 * @return
	 */
	public FulltextSearchResult searchFullText(String searchText, SearchType TYPE, List<Integer> colIds,
			List<String> filters, int start, int rows) {

		QueryResponse result = new QueryResponse();

		SolrQuery query = buildQuery(searchText, TYPE, colIds, filters, start, rows);

		LOGGER.debug("Query: " + query.toString());
		
		try {
			result = server.query(query);
		} catch (SolrServerException | IOException e) {

			e.printStackTrace();
		}		
		return SearchUtils.generateSearchResult(result, TYPE);
	}

	public SolrQuery buildQuery(String searchText, SearchType TYPE, List<Integer> colIds,
			List<String> filters, int start, int rows) {
		SolrQuery query = new SolrQuery();

		String userRightsFilter = "";
		int counter = 0;
		if (colIds == null) {
//			userRightsFilter = "collectionId:*";
			userRightsFilter = "";
		} else {
			for (int i : colIds) {
				userRightsFilter += "collectionId:" + i + " ";
				counter++;
				if (counter < colIds.size())
					userRightsFilter += "OR ";
			}
		}

//		if (filters != null) {
//			for (String filter : filters) {
//				filterString += "AND " + filter + " ";
//			}
//
//		}

		String queryString = "";
		searchText = searchText.trim().replace(" ", "\\ ");

		switch (TYPE) {
		case Words:
			queryString += "fullTextFromWords:" + searchText;
			break;
		case WordsLc:
			queryString += "fullTextFromWordsLc:" + searchText;
			break;
		case Lines:
			queryString += "fullTextFromLines:" + searchText;
			break;
		case LinesLc:
			queryString += "fullTextFromLinesLc:" + searchText;
			break;
		default:
			queryString += "fullTextFromWordsLc:" + searchText;
			break;

		}

				
		if (filters != null) {
			for (String filter : filters) {
				queryString += " AND " + filter;
			}

		}
		if(!userRightsFilter.equals("")){
			queryString += " AND ("+userRightsFilter+")";
		}
		
		

		query.set("q", queryString);
		//query.set("fq", filterString);

		query.setStart(start);
		query.setRows(rows);
		
		boolean highlighting = true;

		if (highlighting) {
			query.set("hl", "on");
			query.set("hl.fl", "fullText*");
			query.set("hl.snippets", "20");
		}
		
		query.setFacet(true);
		query.setFacetMinCount(1);
		query.addFacetField("f_author");
		query.addFacetField("f_uploader");
		query.addFacetField("f_title");
		query.addFacetField("f_collectionName");
		query.addFacetField("scriptType");

		// System.out.println(queryString);

		LOGGER.info("q: " + queryString + ", fq: " + userRightsFilter);
		return query;
	}

	// Set connection to solr
	private SolrClient getSolrClient() {

		// SolrClient solr = new ConcurrentUpdateSolrClient(serverUrl, 20, 3);
		SolrClient solr = new HttpSolrClient.Builder(serverUrl).build();
		return solr;
	}


	


}
