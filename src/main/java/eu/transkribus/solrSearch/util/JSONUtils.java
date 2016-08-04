package eu.transkribus.solrSearch.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.WordType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;

public class JSONUtils {
	
	private final static String JSONPATH = "C:\\Users\\c608393\\TrpDocs\\jsontest\\";
	public static void createFilesFromDoc(TrpDoc doc){
		
		List<TrpPage> pages = doc.getPages(); 
		
		PcGtsType pc = new PcGtsType();
		for(TrpPage p : pages){
			List<TrpWordType> words = new ArrayList<TrpWordType>();
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
									words.add(trptw);
								}
							}else{												//If textline contains no words create them
								for(TrpWordType trptw: IndexTextUtils.getWordsFromLine(ttl)){
									if(trptw != null){
										words.add(trptw);
									}
								}
							}						
						}
				}

			}	
			
			writeJSON(words, p);

		
			}
		}
	
	public static void writeJSON(List<TrpWordType> words, TrpPage p){

		JSONObject obj = new JSONObject();
		obj.put("Name", p.getDocId()+"_"+p.getPageNr()+"_PixelCoords");
		
		JSONArray jWords = new JSONArray();
		
		
		
		System.out.println(p.getDocId()+ " "+p.getPageNr() +"Nr. of words: " +words.size());
		List<String> sWords = new ArrayList<String>();
		int i = 0;
		for(TrpWordType tw : words){
				String sWord = tw.getUnicodeText().replaceAll("¬", ".").replaceAll("\\p{Punct}", "");
				if(!sWord.isEmpty())
					jWords.add(sWord +":"+tw.getCoordinates());
			}
			
			obj.put("Words", jWords);
			
			
			try (FileWriter file = new FileWriter(JSONPATH+p.getDocId()+"\\"+p.getDocId()+"_"+p.getPageNr()+".json")) {
				
				file.write(obj.toJSONString());
				
				file.close();
				System.out.println("Successfully Copied JSON Object to File...");
				System.out.println("\nJSON Object: " + obj);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
}
