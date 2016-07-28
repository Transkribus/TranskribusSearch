package eu.transkribus.solrSearch.util;


import java.util.HashMap;
import java.util.Map;



/**
 * Convenience class that reflects the Solr Schema as enumerations containing all index field names joined with 
 * url parameter names for automatic matching of url_parameters to fields and a label for displaying without 
 * revealing the index Schema to end users.
 * The SortField enumeration contains sortable type fields that are filled via copyfield directive by Solr. 
 * Other fields for sorting may be added too at the risk of performance loss.
 * The FacetField enumeration contains all fields that may be interesting for faceting but do not have a special type.
 * So feel free to add fields here.
 *  
 * @author Philip Kahle
 *
 */
public class Schema {
	
	public static final String ID_FIELD_NAME = "id";
	public static final String TYPE_FIELD_NAME = "type";
	public static final String DOC_ID_FIELD_NAME = "docId";
	public static final String COL_ID_FIELD_NAME = "collectionId";
	public static final String TITLE_FIELD_NAME = "title";
	public static final String AUTHOR_FIELD_NAME = "author";
	public static final String UL_TIME_FIELD_NAME = "uploadTime";
	public static final String GENRE_FIELD_NAME = "genre";
	public static final String WRITER_FIELD_NAME = "writer";
	public static final String SCRIPT_TYPE_FIELD_NAME = "scriptType";
	public static final String UPLOADER_FIELD_NAME = "uploader";
	public static final String NR_OF_PAGES_FIELD_NAME = "nrOfPages";
	public static final String DESC_FIELD_NAME = "description";
	public static final String DOC_TYPE_FIELD_NAME = "docType";
	public static final String LANG_FIELD_NAME = "language";
	public static final String CREATED_FROM_FIELD_NAME = "createdFrom";
	public static final String CREATED_TO_FIELD_NAME = "createdTo";
	public static final String PAGE_NR_FIELD_NAME = "pageNr";
	public static final String FULLTEXT_FIELD_NAME = "fulltext";
	public static final String FULLTEXT_RAW_FIELD_NAME = "fulltext_raw";
	public static final String WORDTEXT = "wordText";
	public static final String TEXTCOORDS = "textCoords";
	public static final String TEXTREGION = "textRegion";
	public static final String TEXTLINE = "textLine";
	public static final String PAGEURL = "pageUrl";
	public static final String PAGETHUMBURL = "pageThumbUrl";
	
	
	public static final SearchField[] catchAll = {
		SearchField.Title, 
		SearchField.Author, 
		SearchField.Description
		};	
	

	public static enum SearchField{
		
		Id(ID_FIELD_NAME, "ID", FieldType.STRING),
		Type(TYPE_FIELD_NAME, "Type", FieldType.STRING),
		DocId(DOC_ID_FIELD_NAME, "Doc-ID", FieldType.INT),
		ColId(COL_ID_FIELD_NAME, "Collection-ID", FieldType.INT),
		Title(TITLE_FIELD_NAME, "Title", FieldType.TEXT),
		Author(AUTHOR_FIELD_NAME, "Author", FieldType.TEXT),
		UploadTime(UL_TIME_FIELD_NAME, "Uploaded", FieldType.TDATE),
		Genre(GENRE_FIELD_NAME, "Genre", FieldType.TEXT),
		Writer(WRITER_FIELD_NAME, "Writer", FieldType.TEXT),
		ScriptType(SCRIPT_TYPE_FIELD_NAME, "Script Type", FieldType.STRING),
		Uploader(UPLOADER_FIELD_NAME, "Uploader", FieldType.TEXT),
		NrOfPages(NR_OF_PAGES_FIELD_NAME, "Pages", FieldType.INT),
		Description(DESC_FIELD_NAME, "Description", FieldType.TEXT),
		DocType(DOC_TYPE_FIELD_NAME, "Document Type", FieldType.STRING),
		Language(LANG_FIELD_NAME, "Language", FieldType.STRING),
		CreatedFrom(CREATED_FROM_FIELD_NAME, "Created from", FieldType.TDATE),
		CreatedTo(CREATED_TO_FIELD_NAME, "Created to", FieldType.TDATE),
		PageNr(PAGE_NR_FIELD_NAME, "Page", FieldType.INT),
		Fulltext(FULLTEXT_FIELD_NAME, "Fulltext", FieldType.TEXT),
		FulltextRaw(FULLTEXT_RAW_FIELD_NAME, "Fulltext Raw", FieldType.STRING),
		Wordtext(WORDTEXT, "WordText", FieldType.STRING),
		TextCoords(TEXTCOORDS, "TextCoords", FieldType.STRING),
		TextRegion(TEXTREGION,"TextRegion", FieldType.STRING),
		TextLine(TEXTLINE, "TextLine", FieldType.STRING),
		PageUrl(PAGEURL, "PageUrl", FieldType.STRING),
		PageThumbUrl(PAGETHUMBURL, "PageThumbUrl", FieldType.STRING);
		
		private final String fieldName;
		private final String label;
		private final FieldType type;
		private SearchField(String fieldN, String labl, FieldType type) {
			this.fieldName = fieldN;
			this.label = labl;
			this.type = type;
		}
		public final String getFieldName() {
			return fieldName;
		}
		public final String getLabel(){
			return label;
		}
		public final FieldType getType(){
			return type;
		}
		private static final Map<String, SearchField> lookup = new HashMap<>();
        static {
            for (SearchField d : SearchField.values())
                lookup.put(d.getFieldName(), d);
        }
        public static SearchField get(String fieldName) {
            return lookup.get(fieldName);
        }
	}
	
	/**
	 * Enum reflecting the datatypes used for fields in solr's schema.xml</br>
	 * </br>
	 * STRING = no processing on indexing/query time. Exact matching (i.e. for identifier)</br>
	 * P4_TEXT = remove stopwords, lowercase, whitespace tokenization, remove latin accents, process synonyms</br>
	 * TDATE = no processing. Allows range queries.</br>
	 * TINT = integer. Trie based</br>
	 * TFLOAT = float. Trie based</br>
	 */
	public static enum FieldType {
		TEXT,
		STRING,
		TDATE,
		TINT,
		INT,
		TFLOAT,
		BOOL;
	}
}
