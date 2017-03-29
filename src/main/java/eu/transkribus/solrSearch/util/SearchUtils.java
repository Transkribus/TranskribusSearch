package eu.transkribus.solrSearch.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.enums.SearchType;
import eu.transkribus.core.model.beans.searchresult.Facet;
import eu.transkribus.core.model.beans.searchresult.FulltextSearchResult;
import eu.transkribus.core.model.beans.searchresult.PageHit;

public class SearchUtils {

	private static final Logger logger = LoggerFactory.getLogger(SearchUtils.class);
	// find tagged stuff
	private static final Pattern TAG_REGEX = Pattern.compile("<em>(.+?)</em>"); //Solr tags for highlighting
	private static final String SPECIAL_SYMBOLS = "[@©«»„“”°■♦¶]";

	public static List<String> getTagValues(final String str) {
		List<String> tagValues = new ArrayList<String>();
		Matcher matcher = TAG_REGEX.matcher(str);
		while (matcher.find()) {
			tagValues.add(matcher.group(1));
		}
		return tagValues;
	}
	
	/**
	 * Generates FullText Results object containing solr results and necessary information
	 * @param response From solr server
	 * @param TYPE Words, WordsLc, Lines, LinesLc
	 * @return FulltextSearchResult object containing relevant information
	 */
	public static FulltextSearchResult generateSearchResult(QueryResponse response, SearchType TYPE){
		
		FulltextSearchResult results = new FulltextSearchResult();			
		results.setParams(response.getHeader().get("params").toString());		
		results.setNumResults(response.getResults().getNumFound());	
		results.setPageHits(generatePageHits(response, TYPE));
		
		ArrayList<Facet> facets = new ArrayList<Facet>();
		
		for(FacetField facetField : response.getFacetFields()){
			Facet facet = new Facet();
			facet.setName(facetField.getName());
			
			Map<String,Long> fMap = new HashMap<String,Long>();
			
			for(Count f : facetField.getValues()){
				fMap.put(f.getName().toString(), f.getCount());
			}
			facet.setFacetMap(fMap);
			facets.add(facet);
		}
		results.setFacets(facets);
		

		return results;
	}

	private static ArrayList<PageHit> generatePageHits(QueryResponse response, SearchType TYPE) {
		
		Map<String, Map<String, List<String>>> hlObj = response.getHighlighting();		
		ArrayList<PageHit> pageHits = new ArrayList<>();
		
		for(Object key : hlObj.keySet()){
			PageHit hit = new PageHit();
			
			switch(TYPE){
			case Words: hit.setHighlights((ArrayList<String>) hlObj.get(key).get("fullTextFromWords"));
						break;
			case WordsLc: hit.setHighlights((ArrayList<String>) hlObj.get(key).get("fullTextFromWordsLc"));
						break;
			case Lines: hit.setHighlights((ArrayList<String>) hlObj.get(key).get("fullTextFromLines"));
						break;
			case LinesLc: hit.setHighlights((ArrayList<String>) hlObj.get(key).get("fullTextFromLinesLc"));
						break;			
			}			
			
			for(SolrDocument result : response.getResults()){
				if(result.getFieldValue("id").toString().equals(key.toString())){
					hit.setPageNr(Long.parseLong(result.getFieldValue("pageNr").toString()));
					hit.setDocId(Long.parseLong(result.getFieldValue("docId").toString()));
					hit.setPageUrl(result.getFieldValue("pageUrl").toString());
					if(result.getFieldValue("f_title") != null){
						hit.setDocTitle(result.getFieldValue("f_title").toString());
					}else{
						hit.setDocTitle("BAD_TITLE");
					}
					
					ArrayList<Integer> collIds = new ArrayList<>();
					Collection<Object> collIdObjects = result.getFieldValues("collectionId");
					//FIXME this is null when searching for "test" on test server!?
					logger.debug("collIdObjects = " + collIdObjects);
					for(Object o : collIdObjects){
						collIds.add(Integer.parseInt(o.toString()));
					}
					hit.setCollectionIds(collIds);
					
					ArrayList<String> hlWords = new ArrayList<>();
					
					for (String hl : hit.getHighlights()){
						for(String hlWord: SearchUtils.getTagValues(hl)){
							hlWords.add(hlWord);
						}							
					}
					
					ArrayList<String> wordCoords = new ArrayList<>();
					
					for(String hlWord: hlWords){
						
						List<Object> wordCoordList = (ArrayList<Object>) result.getFieldValues("wordCoords");
						
						if(wordCoordList != null) {
							for(Object wordCoord : wordCoordList){
								if(wordCoord.toString().split(":")[0].replaceAll(SPECIAL_SYMBOLS, "").equals(hlWord) && !wordCoords.contains(wordCoord.toString())){
									wordCoords.add(wordCoord.toString());	
								}
							}
						}
					}
					hit.setWordCoords(wordCoords);
				}
				
			}
			
			pageHits.add(hit);
		}
		
		return pageHits;
		
		
	}
	
	
}
