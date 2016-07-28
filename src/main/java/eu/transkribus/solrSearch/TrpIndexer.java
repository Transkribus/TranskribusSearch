/*Index transcripts via Solrj 
 * 2016
 * 
 */

package eu.transkribus.solrSearch;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;

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
import eu.transkribus.solrSearch.util.Schema.SearchField;


public class TrpIndexer {
	
	//solr url can also be taken from solr.properties - see constructor
	private final String serverUrl; 
	private static SolrClient server;
	protected static final Logger LOGGER = Logger.getLogger(TrpIndexer.class);
	
	//Constructor
	public TrpIndexer(final String serverUrl){
		if(serverUrl == null || serverUrl.isEmpty())
			throw new IllegalArgumentException("ServerUrl must not be empty!");
		//serverUrl = solrConstants.getString("solrUrl"); //Get server url from properties file
		this.serverUrl = serverUrl;
		server = this.getSolrClient();
		LOGGER.info("Instance of Indexer was created.");
		
	};	
	
	//Index document by indexing metadata and all pages
	public void indexDoc(TrpDoc doc){
		//Check if document is already indexed
		if(isIndexed(doc)){
			removeIndex(doc);
		}
		
		indexDocMd(doc.getMd());
		for(TrpPage p: doc.getPages()){
			indexPage(p);
			try {
				server.commit();
				//server.optimize();
				LOGGER.info("Commited page " + p.getPageNr() + " | doc = " + p.getDocId());
			} catch (SolrServerException | IOException e) {
				LOGGER.error("Could not commit doc MD to solr server.");
				e.printStackTrace();
			}

		}
		try {
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error(e);
			e.printStackTrace();
		}
	}	
	
	//Check if document is indexed
	private boolean isIndexed(TrpDoc doc){

		SolrQuery query = new SolrQuery();
		query.add("q", "id:"+doc.getId()+"_md");
		QueryResponse response = null;
		try {
			response = server.query(query);
		} catch (SolrServerException | IOException e1) {
			LOGGER.error("Could not check if document is already indexed.");
			e1.printStackTrace();
		}
		if(response.getResults().getNumFound()>0){
			return true;
		}
		else{		
			return false;
		}
	}
	
	//Check if page is indexed
	private boolean isIndexed(TrpPage page){
		SolrQuery query = new SolrQuery();
		query.add("q", "id:"+page.getDocId()+"_"+page.getPageNr());
		QueryResponse response = null;
		try {
			response = server.query(query);
		} catch (SolrServerException | IOException e1) {
			LOGGER.error("Could not check if page is already indexed.");
			e1.printStackTrace();
		}
		if(response.getResults().getNumFound() > 0){
			return true;
		}
		else{		
			return false;
		}
	}
	
	//Index TrpDoc metadata
	private boolean indexDocMd(TrpDocMetadata md){
		boolean success = false;
		SolrInputDocument doc = createIndexDocument(md);
		if(doc != null){
			success = submitDocToSolr(doc);
		}		
		return success;
	}

	//Index TrpPage metadata
	private boolean indexPage(TrpPage p){
		boolean success = false;

		try {
			SolrInputDocument doc = createIndexDocument(p);
			if(doc != null){
				success = submitDocToSolr(doc);
				indexText(p);
			}
		} catch (JAXBException e) {
			success = false;
		}
		return success;
	}
	
	//Index TrpPage text
	private boolean indexText(TrpPage p){
		boolean success = true;
		
		PcGtsType pc = new PcGtsType();
		
		List<SolrInputDocument> words = new ArrayList<SolrInputDocument>();
		
		try {
			pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
		} catch (JAXBException e) {
			e.printStackTrace();
			success = false;
		}
		for( TextRegionType tr : PageXmlUtils.getTextRegions(pc)){			//Check all textregions of page
			for(TextLineType tl : tr.getTextLine()){						//Check all textlines of textregion
					TrpTextLineType ttl = (TrpTextLineType) tl;
					if(!ttl.getUnicodeText().isEmpty()){					//Check if textline is empty
						if(ttl.getWordCount() > 0){							//If textline contains TrpWords index them
							for(WordType tw: tl.getWord()){
								TrpWordType trptw = (TrpWordType) tw;
								words.add(createIndexDocument(trptw, p.getPageNr(), p.getDocId()) );
							}
						}else{												//If textline contains no words create them
							for(TrpWordType trptw: getWordsFromLine(ttl)){
								if(trptw != null){
									words.add(createIndexDocument(trptw, p.getPageNr(), p.getDocId()) );
								}
							}
						}
					
					}

			}

		}	
		if(!words.isEmpty())
			submitDocsToSolr(words);
		return success;
	}
	
	
	//Update single page index
	public boolean updatePageIndex(TrpPage p){
		boolean success = false;
		if(isIndexed(p)){
			removeIndex(p);
		}
		try {
			SolrInputDocument doc = createIndexDocument(p);
			if(doc != null){
				success = submitDocToSolr(doc);
				indexText(p);
			}
		} catch (JAXBException e) {
			success = false;
		}
		try {
			server.commit();
			server.optimize();
			LOGGER.info("Commited page to solr server.");
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not commit page to solr server.");
			e.printStackTrace();
		}
		
		return success;
	}
	
	//Delete document and all children from index
	public void removeIndex(TrpDoc doc){
		String queryString = "id:"+doc.getId()+"*";
		try {
			server.deleteByQuery(queryString);
			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not remove document "+doc.getId()+" from index.");
			e.printStackTrace();
		}
	}
	
	//Delete single page and all children from index
	public void removeIndex(TrpPage page){
		String queryString = "id:"+page.getDocId()+"_"+page.getPageNr()+"*";
		try {
			server.deleteByQuery(queryString);
			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not remove page "+page.getDocId()+" from index.");
			e.printStackTrace();
		}
	}
	
	//Delete entire index
	public void resetIndex(){
		String queryString = "*:*";
		try {
			server.deleteByQuery(queryString);
			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not delete from index.");
			e.printStackTrace();
		}
		
	}
	
	//Set connection to solr
	private SolrClient getSolrClient(){
		
		//SolrClient solr = new ConcurrentUpdateSolrClient(serverUrl, 20, 3);	
		SolrClient solr = new HttpSolrClient.Builder(serverUrl).build();
		return solr;
	}
	
	//Submit a SolrInputDocument to solr server
	private boolean submitDocToSolr(SolrInputDocument doc){
		boolean success = true;
		try{
			server.add(doc);
		} catch (SolrServerException e) {
			success = false;
		} catch (IOException e) {
			success = false;
		}
		return success;
	}
	
	//Submit several SolrInputDocuments
	private boolean submitDocsToSolr(List<SolrInputDocument> docs){
		boolean success = true;
		try{
			server.add(docs);
		} catch (SolrServerException e) {
			success = false;
		} catch (IOException e) {
			success = false;
		}
		return success;
}
	
	/*Create SolrInputDocument from Trp metadata
	 * with fields:
	 * Id
	 * Title
	 * Author
	 * DocId
	 * Upload time
	 * Uploader
	 * Nr. of pages
	 * Description
	 */
	private SolrInputDocument createIndexDocument(TrpDocMetadata md){
		
		SolrInputDocument doc = new SolrInputDocument();
		if (md != null) {
			doc.addField(SearchField.Id.getFieldName(), md.getDocId() + "_md");
			doc.addField(SearchField.DocId.getFieldName(), md.getDocId());
			doc.addField(SearchField.Type.getFieldName(), "md");
			doc.addField(SearchField.Title.getFieldName(), md.getTitle().toLowerCase());
			if(md.getAuthor() != null)
				doc.addField(SearchField.Author.getFieldName(), md.getAuthor().toLowerCase());
			doc.addField(SearchField.UploadTime.getFieldName(), md.getUploadTime());
			if(md.getUploader() != null)
				doc.addField(SearchField.Uploader.getFieldName(), md.getUploader());
			doc.addField(SearchField.NrOfPages.getFieldName(), md.getNrOfPages());
			if(md.getDesc() != null)
				doc.addField(SearchField.Description.getFieldName(), md.getDesc());	
			
			List<TrpCollection> colls = md.getColList();
			ArrayList<Integer> colIds = new ArrayList<Integer>();
			for(TrpCollection c : colls){
				colIds.add(c.getColId());
			}
			doc.addField(SearchField.ColId.getFieldName(), colIds);		
			
		}			
		return doc;
	}

	
	/*Create SolrInputDocument from Trp page
	 * 
	 */
	private SolrInputDocument createIndexDocument(TrpPage p) throws JAXBException{
		SolrInputDocument doc = new SolrInputDocument();
		if(p != null){
			
			doc.addField(SearchField.Id.getFieldName(), p.getDocId() + "_" + p.getPageNr());
			doc.addField(SearchField.Type.getFieldName(), "p");			
			doc.addField(SearchField.PageNr.getFieldName(), p.getPageNr());
			if(p.getCurrentTranscript().getUserName() != null)
				doc.addField(SearchField.Uploader.getFieldName(), p.getCurrentTranscript().getUserName());
			doc.addField(SearchField.UploadTime.getFieldName(), p.getCurrentTranscript().getTime());
			doc.addField(SearchField.DocId.getFieldName(), p.getDocId());
			doc.addField(SearchField.PageUrl.getFieldName(), p.getUrl().toString());
			doc.addField("_root_", p.getDocId() + "_md");
			
			
			/*
			//Create word index documents
			//-------------------------------------------------------------------
			PcGtsType pc = new PcGtsType();
			
			List<SolrInputDocument> children = new ArrayList<SolrInputDocument>();
			
			try {
				pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
			} catch (JAXBException e) {
				e.printStackTrace();
			}
			for( TextRegionType tr : PageXmlUtils.getTextRegions(pc)){			//Check all textregions of page
				for(TextLineType tl : tr.getTextLine()){						//Check all textlines of textregion
						TrpTextLineType ttl = (TrpTextLineType) tl;
						if(!ttl.getUnicodeText().isEmpty()){					//Check if textline is empty
							if(ttl.getWordCount() > 0){							//If textline contains TrpWords index them
								for(WordType tw: tl.getWord()){
									TrpWordType trptw = (TrpWordType) tw;
									children.add(createIndexDocument(trptw, p.getPageNr(), p.getDocId()) );
								}
							}else{												//If textline contains no words create them
								for(TrpWordType trptw: getWordsFromLine(ttl)){
									if(trptw != null){
										children.add(createIndexDocument(trptw, p.getPageNr(), p.getDocId()) );
									}
								}
							}
						
						}

				}
			}
			//--------------------------------------------------------------------------------------
			//Attach words as children to page document
			doc.addChildDocuments(children);			
			*/
			
			
		}
		
		return doc;
	}
	
	
	//Create Solr document from Words
	private SolrInputDocument createIndexDocument(TrpWordType word, int pageNr, int docId){
		SolrInputDocument doc = new SolrInputDocument();
		String wordText = word.getUnicodeText().toLowerCase();		//Convert to lowercase
		wordText = wordText.replaceAll("\\p{P}", ""); 				//Remove punctuation
		String wordCoords = word.getCoordinates();					//Get coordinates 
		String[] singleCoords = wordCoords.split(" ");
		if(singleCoords.length > 4){								//If > 4 coordinates reduce to 4 at outlines
			wordCoords = reduceCoordinates(singleCoords);
		}				
		
		doc.addField(SearchField.Id.getFieldName(), docId + "_" + pageNr + "_" + word.getId());
		doc.addField(SearchField.Type.getFieldName(), "w");
		doc.addField(SearchField.Wordtext.getFieldName(), wordText);
		doc.addField(SearchField.TextCoords.getFieldName(), wordCoords);
		doc.addField(SearchField.TextRegion.getFieldName(), word.getLine().getRegion().getId());
		doc.addField(SearchField.TextLine.getFieldName(), word.getLine().getId());
		doc.addField("_root_", docId+"_"+pageNr);
		
		return doc;
		
	}
	
	
	//Create TrpWords from TextLine
	private static ArrayList<TrpWordType> getWordsFromLine(TrpTextLineType line){
		
		ArrayList<TrpWordType> trpWords = new ArrayList<TrpWordType>();
		if (line == null || (line.getBaseline() == null)){
			return trpWords;
		}
		String baseLine = line.getBaseline().getPoints();
		String string = line.getUnicodeText();	
		string = string.replaceAll("\\[", ".").replaceAll("\\]",".").replaceAll("\\p{P}", ".").replaceAll("¬", ".");
		String[] basePts = baseLine.split(" ");

		
		ArrayList<Integer> xPts = new ArrayList<Integer>();
		ArrayList<Integer> yPts = new ArrayList<Integer>();		
		
		for(String s: basePts){
			String[] coords = s.split(",");
			xPts.add(Integer.parseInt(coords[0]));					//Baseline X coords
			yPts.add(Integer.parseInt(coords[1]));					//Baseline Y coords
		}
		int baseLen = Math.abs(xPts.get(xPts.size()-1)-xPts.get(0)); //Length of  baseline in px
		int baseStartX = xPts.get(0);
		
		int wordCounter = 0;
		for(String s : string.split(" ")){
			wordCounter++;
			int subIndex = string.indexOf(s);										//Position of word in string
			float subStart = (float) subIndex / (float)string.length();				//Relative start position 
			int subStartPx = (int) (subStart*(float)baseLen);						//Relative start position in px
			float subLength = (float) s.length() / (float) string.length();			//Length of word
			int subLengthPx = (int) (subLength*(float)baseLen);						//Length of word in px
			int subHeightPx = (int)((float) baseLen / (float)string.length() * 2.0);//Height of word in px (est. 3 characters)
			int wordCoordY1 = yPts.get(0) + (int)((float) subHeightPx / 4.0);			//Y coordinates of baseline
			int wordCoordY2 = yPts.get(0) - subHeightPx;								//Y coordinates of word ceiling				
			int wordCoordX1 = (baseStartX + subStartPx);							//X ccordinates of word start
			int wordCoordX2 =  baseStartX + subStartPx + subLengthPx;				//X coordinates of word end		
			
			String outputCoords =  wordCoordX1 + "," + wordCoordY1 
								+ " " + wordCoordX2 + "," + wordCoordY1
								+ " " + wordCoordX2 + "," + wordCoordY2
								+ " " + wordCoordX1 + "," + wordCoordY2;								
								
			String outputWord = s.replaceAll("\\p{P}", "").toLowerCase().trim(); //Remove punctuation / set lowercase / trim
			
			TrpWordType trpWord = new TrpWordType();
			if(trpWord != null && outputWord != ""){			
				trpWord.setParent(line);
				trpWord.setId(line.getId() + "_" + wordCounter);
				trpWord.setUnicodeText(outputWord, trpWord);
				trpWord.setCoordinates(outputCoords, trpWord);
				trpWord.setLine(line);
				trpWords.add(trpWord);
			}

			String replacement = "";			
			for(int i = 0; i< s.length(); i++){
				replacement +=" " ;
			}		
			string = string.replaceFirst(s, replacement); //Replace word chars with empty spaces	
		}
		return trpWords;
	}
	
	//Takes coordinate string and returns 4 coordinate points on outline
	private String reduceCoordinates(String[] singleCoords){
		ArrayList<Integer> xPts = new ArrayList<Integer>();
		ArrayList<Integer> yPts = new ArrayList<Integer>();		

		for(String s: singleCoords){
			String[] coords = s.split(",");
			xPts.add(Integer.parseInt(coords[0]));					//All X coords
			yPts.add(Integer.parseInt(coords[1]));					//All Y coords
		}
		int wordCoordX1 = xPts.get(0);
		int wordCoordX2 = xPts.get(0);
		int wordCoordY1 = yPts.get(0);
		int wordCoordY2 = yPts.get(0);
		
		for(int x : xPts){
			if(wordCoordX1>x){
				wordCoordX1 = x;
			}
			if(wordCoordX2<x){
				wordCoordX2 = x;
			}
		}
		for(int y : yPts){
			if(wordCoordY1<y){
				wordCoordY1 = y;
			}
			if(wordCoordY2>y){
				wordCoordY2 = y;
			}
		}
		
		String outputCoords =  wordCoordX1 + "," + wordCoordY1 
				+ " " + wordCoordX2 + "," + wordCoordY1
				+ " " + wordCoordX2 + "," + wordCoordY2
				+ " " + wordCoordX1 + "," + wordCoordY2;
		
		
		return outputCoords;
	}
	
}
