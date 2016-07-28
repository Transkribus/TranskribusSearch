package org.dea.transcript.trp.solrSearch;

public class searchResult {
	
	private int id;
	public String author;
	public int pageNr;
	public String coords;
	public String fullText;
	
	
	public searchResult(int iid, String iauthor, int iPageNr, String iCoords, String iFullText){
		id = iid;
		author = iauthor;
		pageNr = iPageNr;
		coords = iCoords;
		fullText = iFullText;
		

	}

}
