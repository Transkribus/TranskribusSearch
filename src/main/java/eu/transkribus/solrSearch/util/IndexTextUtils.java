package eu.transkribus.solrSearch.util;

import java.util.ArrayList;

import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;

public class IndexTextUtils {	
	
	
	//Create TrpWords from TextLine
	public static ArrayList<TrpWordType> getWordsFromLine(TrpTextLineType line){
		
		ArrayList<TrpWordType> trpWords = new ArrayList<TrpWordType>();
		if (line == null || (line.getBaseline() == null)){
			return trpWords;
		}
		String baseLine = line.getBaseline().getPoints();
		String string = line.getUnicodeText();	
		string = string.replaceAll("\\[", ".").replaceAll("\\]",".").replaceAll("\\p{Punct}", ".").replaceAll("¬", ".");
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
								
			String outputWord = s.replaceAll("\\p{Punct}", "").trim(); //Remove punctuation / set lowercase / trim
			
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
	public static String reduceCoordinates(String[] singleCoords){
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

