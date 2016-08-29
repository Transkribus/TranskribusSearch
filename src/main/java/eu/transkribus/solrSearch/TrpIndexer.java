/*Index transcripts via Solrj 
 * 2016
 * 
 */

package eu.transkribus.solrSearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.WordType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.solrSearch.util.IndexTextUtils;
import eu.transkribus.solrSearch.util.Schema.SearchField;

/**
 * Main class for indexing with Apache Solr.
 */
public class TrpIndexer {
	
	private final String serverUrl; 
	private static SolrClient server;
	private static final Logger LOGGER = LoggerFactory.getLogger(TrpIndexer.class);
	private static final String EMPTY_FIELD = "_EMPTY_FIELD";
	private static final String SPECIAL_SYMBOLS = "[@©«»„“”°■♦¶]";
	
	/**
	 * Constructor
	 * @param serverUrl String containing Url to solr server e.g. "http://localhost:8983/solr/Core1"
	 */
	public TrpIndexer(final String serverUrl){
		if(serverUrl == null || serverUrl.isEmpty())
			throw new IllegalArgumentException("ServerUrl must not be empty!");
		//serverUrl = solrConstants.getString("solrUrl"); //Get server url from properties file
		this.serverUrl = serverUrl;
		server = this.getSolrClient();
		LOGGER.info("Instance of Indexer was created.");
		
	}
	
	public void close() throws IOException {
		server.close();
	}
	
	/**
	 * Index a document and optimize
	 * @param doc Trp Document to be indexed
	 * @see indexDoc(TrpDoc doc, boolean doOptimize)
	 */
	public void indexDoc(TrpDoc doc){
		indexDoc(doc, true);
	}	
	
	/**
	 * Index document and optimize if wanted
	 * @param doc Trp Document to be indexed
	 * @param doOptimize If true optimize index after indexing
	 */
	public void indexDoc(TrpDoc doc, boolean doOptimize){
		
		
//		if(isIndexed(doc)){
//			removeIndex(doc);               //probably no longer necessary
//		}
		
		//indexDocMd(doc.getMd());
		for(TrpPage p: doc.getPages()){
			
			if(!indexPage(p, doc.getMd())){
				LOGGER.error("Could not add page " + p.getPageNr() + " | doc = " + p.getDocId());
				continue;
			} else {
				LOGGER.info("Added page " + p.getPageNr() + " | doc = " + p.getDocId());
			}
//			if(p.getPageNr() % 50 == 0) {
//				commitToIndex();
//			}
		}
//		commitToIndex();
		if(doOptimize){ 
			optimizeIndex();
		}
	}	
	
	private void commitToIndex() {
		try {
			LOGGER.info("Commiting...");
			server.commit();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not commit doc MD to solr server.", e);
		}
	}
	
	public void optimizeIndex(){
		LOGGER.debug("Optimizing index...");
		try {
			server.optimize();
			LOGGER.debug("Index is now optimized.");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
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
	
	/*
	//Index TrpDoc metadata
	private boolean indexDocMd(TrpDocMetadata md){
		boolean success = false;
		SolrInputDocument doc = createIndexDocument(md);
		if(doc != null){
			success = submitDocToSolr(doc);
		}		
		return success;
	}
	*/

	//Index TrpPage and doc metadata
	private boolean indexPage(TrpPage p, TrpDocMetadata md){
		boolean success = false;

		try {
			SolrInputDocument doc = createIndexDocument(p, md);
			if(doc != null){
				success = submitDocToSolr(doc);
				//indexText(p);
			}
		} catch (Exception e) {
			success = false;
		}
		return success;
	}
	
	/*
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
							for(TrpWordType trptw: IndexTextUtils.getWordsFromLine(ttl)){
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
	*/
	
	@Deprecated
	public boolean updatePageIndex(TrpPage p, TrpDoc trpDoc){
		return this.updatePageIndex(p, trpDoc.getMd());
	}
	
	//Update single page index
	public boolean updatePageIndex(TrpPage p, TrpDocMetadata trpDocMd){
		boolean success = false;
		if(isIndexed(p)){
			removeIndex(p);
		}		
		
		try {
			SolrInputDocument doc = createIndexDocument(p, trpDocMd);
			if(doc != null){
				success = submitDocToSolr(doc);
				//indexText(p);
			}
		} catch (JAXBException e) {
			success = false;
		}
		try {
//			server.commit();
			server.optimize();
			LOGGER.info("Added page to solr server.");
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not commit page to solr server.");
			e.printStackTrace();
		}
		
		return success;
	}
	
	/**
	 * Delete all pages of document from index
	 * @param doc TrpDoc to be deleted from index
	 * @see removeIndex(int docId) to directly remove by document id
	 */
	public void removeIndex(TrpDoc doc){
		removeIndex(doc.getId());
	}
	
	/**
	 * Delete all pages of document from index
	 * @param docId Id of document to be deleted
	 */
	public void removeIndex(int docId){
		String queryString = "id:"+docId+"*";
		try {
			server.deleteByQuery(queryString);
//			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not remove document "+docId+" from index.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Delete single page from index
	 * @param page TrpPage to be deleted
	 */
	public void removeIndex(TrpPage page){
		removeIndex(page.getDocId(), page.getPageNr());
	}
	
	/**
	 * Delete single page from index
	 * @param docId Id of document containing page
	 * @param pageNr Page number of page to be deleted
	 */
	public void removeIndex(int docId, int pageNr){
		String queryString = "id:"+docId+"_"+pageNr+"*";
		try {
			server.deleteByQuery(queryString);
//			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not remove page "+docId+" from index.");
			e.printStackTrace();
		}
	}
		
	
	/**
	 * Deletes the entire index.
	 * <br>
	 * <b>Use with caution!</b>
	 */
	public void resetIndex(){
		String queryString = "*:*";
		try {
			server.deleteByQuery(queryString);
//			server.commit();
			server.optimize();
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not delete from index.");
			e.printStackTrace();
		}
		
	}
	
	//Set connection to solr
	private SolrClient getSolrClient(){
		
//		SolrClient solr = new ConcurrentUpdateSolrClient(serverUrl, 20, 3);	
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
	
	/*Create SolrInputDocument from Trp metadata - obsolete
	 * with fields:
	 * Id
	 * Title
	 * Author
	 * DocId
	 * Upload time
	 * Uploader
	 * Nr. of pages
	 * Description
vate SolrInputDocument createIndexDocument(TrpDocMetadata md){
		
		SolrInputDocument doc = new SolrInputDocument();
		if (md != null) {
			doc.addField(SearchField.Id.getFieldName(), md.getDocId() + "_md");
			doc.addField(SearchField.DocId.getFieldName(), md.getDocId());
			doc.addField(SearchField.Type.getFieldName(), "md");
			doc.addField(SearchField.Title.getFieldName(), md.getTitle());
			if(md.getAuthor() != null)
				doc.addField(SearchField.Author.getFieldName(), md.getAuthor());
			else
				doc.addField(SearchField.Author.getFieldName(), EMPTY_FIELD);
			
			if(md.getGenre() != null)
				doc.addField(SearchField.Genre.getFieldName(), md.getGenre());
			else
				doc.addField(SearchField.Genre.getFieldName(), EMPTY_FIELD);
			
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
	*/

	
	/*Create SolrInputDocument from Trp page - obsolete
	 * 
	 
	private SolrInputDocument createIndexDocument(TrpPage p) throws JAXBException{
		SolrInputDocument doc = new SolrInputDocument();
		if(p != null){
			
			doc.addField(SearchField.Id.getFieldName(), p.getDocId() + "_" + p.getPageNr());
			//doc.addField(SearchField.Type.getFieldName(), "p");			
			doc.addField(SearchField.PageNr.getFieldName(), p.getPageNr());
			if(p.getCurrentTranscript().getUserName() != null)
				doc.addField(SearchField.Uploader.getFieldName(), p.getCurrentTranscript().getUserName());
			doc.addField(SearchField.UploadTime.getFieldName(), p.getCurrentTranscript().getTime());
			doc.addField(SearchField.DocId.getFieldName(), p.getDocId());
			doc.addField(SearchField.PageUrl.getFieldName(), p.getUrl().toString());
			
			
			PcGtsType pc = new PcGtsType();
			try {
				pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
			} catch (JAXBException e) {
				e.printStackTrace();
			}
			doc.addField(SearchField.Fulltext.getFieldName(), PageXmlUtils.getFulltextFromLines(pc));
			
			
			doc.addField("_root_", p.getDocId() + "_md");

		}
		
		return doc;
	}
	*/
	
	
	private SolrInputDocument createIndexDocument(TrpPage p, TrpDocMetadata md) throws JAXBException{
		SolrInputDocument doc = new SolrInputDocument();
		if(p != null){
			
			doc.addField(SearchField.Id.getFieldName(), p.getDocId() + "_" + p.getPageNr());
			doc.addField(SearchField.DocId.getFieldName(), p.getDocId());
			doc.addField(SearchField.Title.getFieldName(), md.getTitle());
			doc.addField(SearchField.ScriptType.getFieldName(), md.getScriptType().getStr());
			doc.addField(SearchField.Author.getFieldName(), md.getAuthor());
			doc.addField(SearchField.Genre.getFieldName(), md.getGenre());
			//doc.addField(SearchField.Type.getFieldName(), "p");			
			doc.addField(SearchField.PageNr.getFieldName(), p.getPageNr());
			doc.addField(SearchField.NrOfPages.getFieldName(), md.getNrOfPages());
			if(p.getCurrentTranscript().getUserName() != null)
				doc.addField(SearchField.Uploader.getFieldName(), p.getCurrentTranscript().getUserName());
			doc.addField(SearchField.UploadTime.getFieldName(), p.getCurrentTranscript().getTime());
			doc.addField(SearchField.PageUrl.getFieldName(), p.getUrl().toString());
						
			List<TrpCollection> colls = md.getColList();
			ArrayList<Integer> colIds = new ArrayList<Integer>();
			ArrayList<String> colNames = new ArrayList<>();
			for(TrpCollection c : colls){
				colIds.add(c.getColId());
				colNames.add(c.getColName());
			}
			doc.addField(SearchField.ColId.getFieldName(), colIds);	
			doc.addField(SearchField.ColName.getFieldName(), colNames);	
			
			
			

			
			
					
			
			PcGtsType pc = new PcGtsType();
			try {
				pc = PageXmlUtils.unmarshal(p.getCurrentTranscript().getUrl());
			} catch (Exception e) {
				LOGGER.error("XML Unmarshal failed for Doc "+p.getDocId() +" page "+p.getPageNr());
				throw e;
			}
			
			String fullTextFromWords ="";
			TrpPageType pt = (TrpPageType)pc.getPage();
			ArrayList<CustomTag> tags = new ArrayList<CustomTag>();
			for(TrpTextRegionType ttr : pt.getTextRegions(false)){
				//Get page fulltext from words
				fullTextFromWords += ttr.getTextFromWords(true).replaceAll("\n", " ");//.replaceAll("\\p{Punct}", ".");				
				
				//Get tags on page
				List<TextLineType> tls = ttr.getTextLine();
				for(TextLineType tl : tls){
					TrpTextLineType ttl = (TrpTextLineType) tl;
					if(ttl.getCustomTagList() != null){
						if(!ttl.getCustomTagList().getIndexedTagNames().isEmpty()){
							for(CustomTag tag : ttl.getCustomTagList().getTags()){
								if(!(tag.getTagName() == "readingOrder" || tag.getTagName() == "textStyle" )){
									tags.add(tag);									
								}
							}							
						}						
					}				
				}
			}
			
			doc.addField(SearchField.Fulltextfromlines.getFieldName(), PageXmlUtils.getFulltextFromLines(pc));
			
			doc.addField(SearchField.Fulltextfromwords.getFieldName(), fullTextFromWords);		
			
			ArrayList<TrpWordType> words = getWordList(pc);
			for(TrpWordType word : words){
				if(!word.getUnicodeText().trim().replaceAll("\\p{Punct}", "").replaceAll(SPECIAL_SYMBOLS, "").isEmpty()){
					String wordAndCoords = word.getUnicodeText().replaceAll("\\p{Punct}", "")
											+ ":"+word.getLine().getRegion().getId()+"/"
											+ word.getLine().getId()+"/"
											+ (word.getId().isEmpty() ? "_empty_" : word.getId())
											+ ":"+word.getCoordinates();
	
					doc.addField(SearchField.WordCoords.getFieldName(), wordAndCoords.replaceAll(SPECIAL_SYMBOLS, ""));
				}
			}
			
			
			

			
			for(CustomTag tag : tags){
				doc.addField("tags",
						tag.getTagName().replaceAll("\\p{Punct}", "")
						+ "|" 
						+tag.getContainedText().replaceAll("\\p{Punct}", ""));
			}
			
			//doc.addField("_root_", p.getDocId() + "_md");

		}
		
		return doc;
	}
	
	
	
	/**
	 * Returns a list of TrpWords contained in page transcript.<br>
	 * TrpWords are generated from TrpLines when necessary.
	 * @param pc PcGtsType gained from PageXmlUtils.unmarshal(<i>pageUrl</i>)
	 */
	public ArrayList<TrpWordType> getWordList(PcGtsType pc){
		
		ArrayList<TrpWordType> words = new ArrayList<TrpWordType>();		
		
		for( TextRegionType tr : PageXmlUtils.getTextRegions(pc)){			//Check all textregions of page
			for(TextLineType tl : tr.getTextLine()){						//Check all textlines of textregion
					TrpTextLineType ttl = (TrpTextLineType) tl;
					if(!(ttl.getUnicodeText().isEmpty()  && ttl.getTextFromWords(true).isEmpty()) ){
						
						
						ArrayList<TrpWordType> trpWordsInLine = new ArrayList<TrpWordType>();	
						ArrayList<TrpWordType> wordsGeneratedFromLine = IndexTextUtils.getWordsFromLine(ttl);
						
						
						if(ttl.getWordCount() > 0){							//If textline contains TrpWords index them
							
							for(WordType tw: tl.getWord()){		
	
								TrpWordType trptw = (TrpWordType) tw;
								
								String[] singleCoords = trptw.getCoordinates().split(" ");
								if(singleCoords.length > 4){								//If > 4 coordinates reduce to edges
									String newWordCoords = IndexTextUtils.reduceCoordinates(singleCoords);
									trptw.setCoordinates(newWordCoords, trptw);
								}
								if(!trptw.getUnicodeText().trim().isEmpty()){
									trpWordsInLine.add(trptw);
								}
							}
						}else{												//If textline contains no TrpWords create them
							for(TrpWordType trptw: wordsGeneratedFromLine){
								if(trptw != null){
									if(!trptw.getUnicodeText().trim().isEmpty()){
										trpWordsInLine.add(trptw);
									}
								}
							}
						}					
						
						//Check if line based text contains different words than word based text
						//and add missing words if necessary
						if(ttl.getWordCount() > 0){	
							boolean contained = false;
							for(TrpWordType generatedWord : wordsGeneratedFromLine){
								contained = false;
								
								for(TrpWordType wordInLine : trpWordsInLine){
									if(wordInLine.getUnicodeText().replaceAll("\\p{Punct}", "").equals(generatedWord.getUnicodeText())){
										contained = true;
									}
								}
								if(!contained){
									trpWordsInLine.add(generatedWord); 
								}
				
							}
						}
						words.addAll(trpWordsInLine);
					}
			}

		}
		
		return words;
	}
	
}
