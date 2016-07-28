import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.WordType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.persistence.logic.CollectionManager;
import eu.transkribus.persistence.logic.DocManager;


public class SearchTest {
	static String solrURL = "http://localhost:8983/solr/trp";
	public static void main(String[] args) throws Exception{
		
		
		
		
		
		DocManager dMan = new DocManager();
		CollectionManager cMan = new CollectionManager();
		
		TrpPage p = dMan.getDocById(89).getPages().get(139);
		PcGtsType pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
		for(TextRegionType tr : PageXmlUtils.getTextRegions(pc)){
			for(TextLineType tl : tr.getTextLine()){
				for(WordType tw: tl.getWord()){
					TrpWordType ttw = (TrpWordType) tw;
					String word = tw.getTextEquiv().getUnicode().replaceAll("[^\\p{L}]", "");
					//System.out.println(word + "\t\t" + tw.getCoords().getPoints());
					System.out.println(word + "\t\t" + p.getPageNr());
				}
			}
		}
		
			
		/*
		List<TrpCollection> colls = cMan.getCollectionsByUser(true, 43, 0, 0, null, null);
		for(TrpCollection c : colls){
			List<TrpDocMetadata> docs = dMan.getDocList(c.getColId());
			for(TrpDocMetadata md : docs){
				TrpDoc doc = dMan.getDocById(md.getDocId());
				for(TrpPage p : doc.getPages()){
					PcGtsType pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
					for(TextRegionType tr : PageXmlUtils.getTextRegions(pc)){
						for(TextLineType tl : tr.getTextLine()){
							TrpTextLineType ttl = (TrpTextLineType) tl;
							System.out.println(ttl.getUnicodeText()+ " " + ttl.getCoordinates());
						}
					}
				}
			}
		}
		*/
		
		

			

		
		
		
		

		/*
		List<TrpPage> pages = dMan.getDocById(558).getPages();
		for(TrpPage p : pages){
			System.out.println("Page: "+p.getPageNr());
			PcGtsType pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl()); 
			for(TextRegionType tr : PageXmlUtils.getTextRegions(pc)){
				System.out.println("\t: " + tr.getId());
				
				
				for(TextLineType tl : PageXmlUtils.getLinesInRegion(pc, tr.getId()) ){					
					TrpTextLineType ttl = (TrpTextLineType) tl;
					if(ttl.getWordCount() > 0){
						for(WordType tw : ttl.getWord() ){
							TrpWordType ttw = (TrpWordType) tw;
							System.out.println("\t\t\t"+ttw.getUnicodeText()+" \n\t\t\t\tCoords:"+ttw.getCoordinates());
						}
					}
					else{
						System.out.println("\t\t"+tl.getId()+": "+ttl.getUnicodeText() + "\n\t\t\tCoords: " + ttl.getCoordinates());
					}
				}
			}
			
			
		}
		*/

	
		
		
		
		
		
		
		
		//SolrClient solr = new HttpSolrClient.Builder(solrURL).build();
		
	    //doQuery( solr, "","*:*", "" );
		
		
		
		/*
		String searchId = "1052";
		String searchText = "Vor der Stadt";
		String searchPage = "";
		String searchAuthor = "";
		
	
		TrpSearcher searcher = new TrpSearcher();
		
		//Search for id, text, author, page
		QueryResponse response = searcher.search(searchId, searchText, searchAuthor, searchPage);
		
		
		
		SolrDocumentList docs = response.getResults();
		
		
		System.out.println("-----------------------------------");
		System.out.println("Searched for:");
		System.out.println("Id: "+searchId);
		System.out.println("Text: "+searchText);
		System.out.println("Page: "+searchPage);
		System.out.println("Author: "+searchAuthor);		
		System.out.println("-----------------------------------");
		System.out.println("No. Results: "+ docs.getNumFound());
		
		
		for(SolrDocument doc : docs ){

			String resultId = (String) doc.getFieldValue("id");
			int lineIndex = resultId.indexOf("_");
			if(lineIndex != -1) resultId = resultId.substring(0, lineIndex);	
			String pageNr = doc.getFieldValue("pageNr").toString().replaceAll("[^\\d.]", "") ;
			System.out.println("Doc Id: "+resultId);
			System.out.println("Page: "+pageNr);
			//System.out.println( searcher.getCoords(Integer.parseInt(resultId) , Integer.parseInt(pageNr) ,  searchText) );
			System.out.println(doc.getFieldValue("fulltext"));
			
		}
		
		DocManager dMan = new DocManager();
		TrpPage p = dMan.getDocById(1052).getPages().get(2);
		
		System.out.println(p.getCurrentTranscript());
		
		*/
		
	}
	static void doQuery( SolrClient solr, String description, String queryStr, String optFilter ) throws Exception {
	    doQuery( solr, description, queryStr, optFilter, null, null );
	}
	static void doQuery( SolrClient solr, String description, String queryStr, String optFilter,
            String optFields, Map<String,String>extraParams ) throws Exception {
		
	    // Setup Query
	    SolrQuery q = new SolrQuery( queryStr );
	    System.out.println();
	    System.out.println( "Test: " + description );
	    System.out.println( "\tSearch: " + queryStr );
	    if ( null!=optFilter ) {
	        q.addFilterQuery( optFilter );
	        System.out.println( "\tFilter: " + optFilter );
	    }
	    if ( null!=optFields ) {
	        // Use setParam instead of addField
	        q.setParam( "fl", optFields );  // childFilter=doc_type:chapter limit=100
	        System.out.println( "\tFields: " + optFields );
	    }
	    else {
	        q.addField( "*" );  // childFilter=doc_type:chapter limit=100
	    }
	    if ( null!=extraParams ) {
	        for ( Entry<String,String> param : extraParams.entrySet() ) {
	            // Could use q.setParam which allows you to pass in multiple strings
	            q.set( param.getKey(), param.getValue() );
	            System.out.println( "\tParam: " + param.getKey() + "=" + param.getValue() );
	        }
	    }

	    // Run and show results
	    QueryResponse rsp = solr.query( q );
	    SolrDocumentList docs = rsp.getResults();
	    long numFound = docs.getNumFound();
	    System.out.println( "Matched: " + numFound );
	    int docCounter = 0;
	    for (SolrDocument doc : docs) {
	        docCounter++;
	        System.out.println( "Doc # " + docCounter );
	        for ( Entry<String, Object> field : doc.entrySet() ) {
	            String name = field.getKey();
	            Object value = field.getValue();
	            System.out.println( "\t" + name + "=" + value );
	        }
	        List<SolrDocument> childDocs = doc.getChildDocuments();
	        // TODO: make this recursive, for grandchildren, etc.
	        if ( null!=childDocs ) {
	            for ( SolrDocument child : childDocs ) {
	                System.out.println( "\tChild doc:" );
	                for ( Entry<String, Object> field : child.entrySet() ) {
	                    String name = field.getKey();
	                    Object value = field.getValue();
	                    System.out.println( "\t\t" + name + "=" + value );
	                }
	            }
	        }
	    }
	    System.out.println( "Query URL:" );
	    // TODO: should check URL for existing trailing /, and allow for different query handler
	    System.out.println( solrURL + "/select?" + q );
	}
	
}
