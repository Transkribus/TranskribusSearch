package eu.transkribus.solrSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class SearchResult {
	private Map<String,ArrayList<String>> wordHighlights = new HashMap<String,ArrayList<String>>();
	private Map<String,ArrayList<String>> lineHighlights = new HashMap<String,ArrayList<String>>();
	private Map<String,ArrayList<String>> wordHighlightsLc = new HashMap<String,ArrayList<String>>();
	private Map<String,ArrayList<String>> lineHighlightsLc = new HashMap<String,ArrayList<String>>();
	private Map<String,String> authors = new HashMap<String,String>();
	private Map<String,String> parentDocs = new HashMap<String,String>();
	private Map<String,String> pageUrls = new HashMap<String,String>();
	private Map<String,ArrayList<Object>> wordCoords = new HashMap<String,ArrayList<Object>>();
	private Map<String,Long> collFacets = new HashMap<String,Long>();
	
	public SearchResult(){
		
	}
	
	public void getFromSolrResponse(QueryResponse response){
		Map<String, Map<String, List<String>>> obj = response.getHighlighting();
		for(String key : obj.keySet()){
			wordHighlights.put(key, (ArrayList<String>) obj.get(key).get("fullTextFromWords"));	
			lineHighlights.put(key, (ArrayList<String>) obj.get(key).get("fullTextFromLines"));
			wordHighlightsLc.put(key, (ArrayList<String>) obj.get(key).get("fullTextFromWordsLc"));	
			lineHighlightsLc.put(key, (ArrayList<String>) obj.get(key).get("fullTextFromLinesLc"));
		}
		
		for(SolrDocument result : response.getResults()){
			if(result.getFieldValue("author") != null){
				authors.put(result.getFieldValue("id").toString(), result.getFieldValue("author").toString());
			}
			if(result.getFieldValue("docId") != null){
				parentDocs.put(result.getFieldValue("id").toString(), result.getFieldValue("docId").toString());
			}		
		}
		
		for(SolrDocument result : response.getResults()){
			if(result.getFieldValue("pageUrl") != null){
				pageUrls.put(result.getFieldValue("id").toString(), result.getFieldValue("pageUrl").toString());
			}
		}
		
		for(SolrDocument result : response.getResults()){
			if(result.getFieldValue("wordCoords") != null){
				wordCoords.put(result.getFieldValue("id").toString(), (ArrayList<Object>) result.getFieldValues("wordCoords"));
			}
		}
		
		for (Count count : response.getFacetField("collectionId").getValues()){
			collFacets.put(count.getName(), count.getCount());
		}
		
	}
	
	
	public SearchResult(QueryResponse response){		
		
	}
	
	public Map<String,ArrayList<String>> getWordsLcHighlights(){
		return wordHighlightsLc;
	}
	
	public Map<String,ArrayList<String>> getLinesLcHighlights(){
		return lineHighlightsLc;
	}
	
	public Map<String,ArrayList<String>> getWordsHighlights(){
		return wordHighlights;
	}
	
	public Map<String,ArrayList<String>> getLinesHighlights(){
		return lineHighlights;
	}
	
	public Map<String,String> getAuthors(){
		return authors;
	}
	
	public Map<String,String> getParentDocs(){
		return parentDocs;
	}
	
	public Map<String,String> getPageUrls(){
		return pageUrls;
	}
	
	public Map<String,ArrayList<Object>> getWordCoords(){
		return wordCoords;
	}
	
	public Map<String,Long> getCollFacets(){
		return collFacets;
	}
	
	

}
