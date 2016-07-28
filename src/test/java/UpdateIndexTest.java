import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dea.transcript.trp.solrSearch.TrpIndexer;

import de.planet.tech.roi_core.util.LineType;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.PageXmlUtils;
import eu.transkribus.persistence.logic.CollectionManager;
import eu.transkribus.persistence.logic.DocManager;


public class UpdateIndexTest {
	
	public static void main(String[] args) throws SQLException, ReflectiveOperationException, JAXBException{
			
		
		TrpIndexer indexer = new TrpIndexer();
		DocManager dMan = new DocManager();
		CollectionManager cMan = new CollectionManager();
		//List<TrpCollection> colls = cMan.getCollectionsByUser(true, 43, 0, 0, null, null);
		
		//indexer.resetIndex();
		
		
		
		long start = System.nanoTime();
		/*
		List<TrpDocMetadata> docs = dMan.getDocList(2);
		for(TrpDocMetadata md : docs){
			int id = md.getDocId();
			indexer.indexDoc(dMan.getDocById(id));
		}
		*/
		
		indexer.indexDoc(dMan.getDocById(62));
		
		
		long dt = System.nanoTime() - start;
		System.out.println((float)dt/(float)10E9);
		
	}
	

}
