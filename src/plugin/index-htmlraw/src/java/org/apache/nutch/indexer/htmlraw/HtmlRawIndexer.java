package org.apache.nutch.indexer.htmlraw;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.nutch.parse.Parse;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.lucene.LuceneWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.hadoop.conf.Configuration;
import org.apache.html.dom.HTMLDocumentImpl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.sksamuel.diffpatch.DiffMatchPatch;
import com.sksamuel.diffpatch.DiffMatchPatch.Diff;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import org.cyberneko.html.parsers.DOMFragmentParser;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.ElementQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.diffxml.diffxml.*;
import org.diffxml.diffxml.xmdiff.XmDiff;

/** 
Finds differences in XML between fetch and last version of fetch. Uses raw html added by HtmlRawParser plugin.
Requires a MongoDB instance to hold last version of fetch for comparison.
The MongoDB collection needs to have composite indexing by url (either ascending or descending) 
and date (descending) to retrieve the newest previous fetch.
 */
public class HtmlRawIndexer implements IndexingFilter {
	private final Log LOG = LogFactory.getLog(HtmlRawIndexer.class.getName());
	private Configuration conf;

	private final Logger performanceLog = Logger.getLogger("DiffPerformanceLogger");
	
	private static String MONGODB_IP = "localhost";
	private static short MONGODB_PORT = 27017;
	private static String MONGODB_DBNAME = "nutch";  
	private static String MONGODB_COLLNAME = "lasthtmlbuffer";
	private static String RABBIT_IP = "";
	private static MongoClient mongoClient = null;
	private static DB db = null;
	private static short DIFFEDITCOST = 100;
	private static short MINIMUMDIFFDISTANCE = 3;
	private static String MODELWRITEMODE = "RDF/XML"; // empty string defaults to RDF/XML
	private static boolean OUTPUT_WRITE_DIFFS_TO_SEPARATE_FILES = false;
	private static boolean OUTPUT_WRITE_DIFFS_TO_SINGLE_FILE = false;
	private static boolean DIFF_LOG_NULL_WARNINGS = true;
	private static boolean LOG_PERFORMANCE_TO_FILE = false;
	private static boolean POST_DO_SEND_REQUEST = false;
	private static String SYSTEM_OUT_FILENAME = "system_out.txt";
	private String[] perflogColumnNames ={
			"doc_size","time_DBread","time_DBwrite",
			"time_parseXML","time_xmdiff","dist_xmdiff",
			"time_diffXML", "time_JenaModel","time_DiffsToPOST",
			"time_DIffsToFiles"};

	
	public NutchDocument filter(NutchDocument doc, Parse parse, 
			Text url, CrawlDatum datum, Inlinks inlinks)
					throws IndexingException {

		// initialise time measurements
		performanceLog.setLevel(Level.INFO);
		Map<String,Long> measurementsMap = new HashMap<String,Long>();
		long startTime = 0;
		long endTime = 0;
		measurementsMap.put("time_DBread", new Long(0));
		measurementsMap.put("time_DBwrite", new Long(0));
		measurementsMap.put("time_parseXML", new Long(0));
		measurementsMap.put("time_diffXML", new Long(0));
		measurementsMap.put("time_JenaModel", new Long(0));
		measurementsMap.put("time_DiffsToPOST", new Long(0));
		measurementsMap.put("time_DiffsToFiles", new Long(0));
		
		
		
		// Get date, url and htmlraw from NutchDoc.
		// XXX: NutchWAX gives null datum to document
		//		Date fetchDate = new Date(datum.getFetchTime());
		Date fetchDate = new Date(); 
		String fetchurl = url.toString().split(" ")[0].trim();
		String htmlraw = parse.getData().getParseMeta().get("htmlraw");	

		measurementsMap.put("doc_size", new Long(htmlraw.length()));
		
		// MongoDB can't handle keys above 1024 bytes (assume trap?)
		if (fetchurl.getBytes().length >= 900) {
			LOG.warn("HtmlRawIndexer: url longer than 900 bytes, avoiding.");
			writePerformanceToLog("URL_TOO_LONG", measurementsMap, fetchurl);
			return doc;
		}

		// Read last fetch from DB.
		startTime = System.currentTimeMillis();
		db.requestEnsureConnection();
		BasicDBObject dbOldDoc = null;
		BasicDBObject query = new BasicDBObject("url",fetchurl);
		DBCollection coll = db.getCollection(MONGODB_COLLNAME);
		dbOldDoc = (BasicDBObject) coll.findOne(query);
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_DBread", new Long(endTime - startTime));

		// Save current fetch to DB.
		startTime = System.currentTimeMillis();
		BasicDBObject dbNewDoc = new BasicDBObject("url",fetchurl).
				append("date",fetchDate).
				append("htmlraw",htmlraw);
		coll.insert(dbNewDoc);

		doc.add("crawldate", fetchDate.toString());
		if(dbOldDoc != null){  // Add last fetch date to NutchDoc. Also checks if last fetch was retrieved from MongoDB.	
			doc.add("lastdate", dbOldDoc.get("date").toString());
		}
		else{ // If couldn't get old doc, can't diff. Return NutchDoc to indexer.
			LOG.warn("HtmlRawIndexer: Could not load previous version of " + fetchurl + " from MongoDB. No diff possible.");
			writePerformanceToLog("NO_PREVIOUS_DOC", measurementsMap, fetchurl);
			return doc;
		}
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_DBwrite", new Long(endTime - startTime));
		
		
		// Diff old and new HTMLs.

		startTime = System.currentTimeMillis();
		LOG.info("URL = "+url);
		LOG.info("htmlraw len = "+htmlraw.length());		

		//--- Parse to xml. ---
		//		DOMFragmentParser parser = new DOMFragmentParser();
		DOMParser parser = new DOMParser();
		InputSource htmlSource = null;

		Document newDoc = null;
		Document oldDoc = null;
		try{

//			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");

			// Parse new document

			htmlSource = new InputSource(new ByteArrayInputStream(htmlraw.getBytes("utf-8")));
			//			htmlSource = new InputSource(new StringReader(htmlraw));
			parser.parse(htmlSource);
			newDoc = parser.getDocument();


			// Parse old document
			htmlSource = null;

			htmlSource = new InputSource(new ByteArrayInputStream(dbOldDoc.getString("htmlraw").getBytes("utf-8")));
			//			htmlSource = new InputSource(new StringReader(dbOldDoc.getString("htmlraw")));
			parser.parse(htmlSource);
			oldDoc = parser.getDocument();

		}catch(Exception e){
			LOG.error("Error parsing XML : " + e.getMessage());
			writePerformanceToLog("ERROR_XML_PARSING", measurementsMap, fetchurl);
			return doc;
		}
		//		org.w3c.dom.Document oldDoc = parser.getDocument();
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_parseXML", new Long(endTime - startTime));
		//--- End of parse to xml. ---
		
		
		//--- Diff xmls using xmlunit. ---
		startTime = System.currentTimeMillis();
		List<Difference> listOfXmlDiffs = null;
		try{
			//			RecursiveElementNameAndTextQualifier eq = new RecursiveElementNameAndTextQualifier();
			//			XMLUnit.setCompareUnmatched(false);
			org.custommonkey.xmlunit.Diff xmlDiff = new org.custommonkey.xmlunit.Diff(oldDoc, newDoc);
			LOG.info("xmlunit diff identical = " + xmlDiff.identical() + ", similar = " + xmlDiff.similar());
			org.custommonkey.xmlunit.DetailedDiff detailedDiff = new DetailedDiff(xmlDiff);
			detailedDiff.overrideElementQualifier(null);
			listOfXmlDiffs = detailedDiff.getAllDifferences();
		}catch(Exception e){
			LOG.error("HtmlRawIndexer XMLUnit Exception when diffing " + fetchurl +" : " + e.toString());
			writePerformanceToLog("ERROR_DIFFING", measurementsMap, fetchurl);
			return doc;
		}
		
		if(listOfXmlDiffs == null){
			LOG.error("HtmlRawIndexer XMLUnit difflist is null!");
			writePerformanceToLog("NULL_DIFFLIST", measurementsMap, fetchurl);
			return doc;
		}

		if (listOfXmlDiffs.isEmpty()){
			LOG.info("HtmlRawIndexer: No differences found for " + fetchurl);
			writePerformanceToLog("NO_DIFFS_FOUND", measurementsMap, fetchurl);
			return doc;
		}    
		else{
			LOG.info("HtmlRawIndexer: "+ listOfXmlDiffs.size() +" differences detected for " + fetchurl);
		}
		// Debug method: output diffs to log. Not needed for functionality.
		//		writeAllXMLUnitDiffsToLog(listOfXmlDiffs);
		// Debug end. 
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_diffXML", new Long(endTime - startTime));

		
		startTime = System.currentTimeMillis();
		long oldTimeUnix = ((Date) dbOldDoc.get("date")).getTime() / 1000;
		long fetchTimeUnix = fetchDate.getTime() / 1000;

		Model model = ModelFactory.createDefaultModel();
		
		// Create diff Jena model.
		try{
			model = createDiffRdf(listOfXmlDiffs, fetchurl, oldTimeUnix, fetchTimeUnix, dbOldDoc);
		}catch(Exception e){
			// if failed to create model, skip pocessing this page
			LOG.info("Diff RDF creation error: " + e.getMessage());
			writePerformanceToLog("ERROR_JENA_MODEL", measurementsMap, fetchurl);
			return doc;
		}
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_JenaModel", new Long(endTime - startTime));
		
		// Write diffs from Jena model to string, then POST diffstring, then add diffstring to NutchDoc.
		startTime = System.currentTimeMillis();
		StringWriter sw = new StringWriter();
		model.write(sw, MODELWRITEMODE);
		createAndSendPOSTRequest(sw.toString()); 
		LOG.debug("Added " + fetchurl + "diffs to doc diff field");
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_DiffsToPOST", new Long(endTime - startTime));

		//--- If enabled, write diffs to file(s). ---
		startTime = System.currentTimeMillis();
		String filenamePrefix = (fetchDate.toString()  + " " + fetchurl + " ").replaceAll("[:.//]", "_");
		if (OUTPUT_WRITE_DIFFS_TO_SINGLE_FILE){

			try {
				BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(
								new FileOutputStream("AllDiffs.txt", true),
								"UTF-8")
						);
				model.write(bw, MODELWRITEMODE);
				bw.close();
				LOG.info("Wrote "+ fetchurl + " to single diff file.");
			} catch (Exception e) {
				LOG.error("HtmlRawIndexer: IOException in writing Diff to file:" + e.getMessage());
				writePerformanceToLog("ERROR_WRITING_TO_SINGLE_FILE", measurementsMap, fetchurl);
				return doc;
			}
		}
		if(OUTPUT_WRITE_DIFFS_TO_SEPARATE_FILES){
			try {
				PrintWriter out = new PrintWriter(filenamePrefix+"JenaOutput.txt");
				model.write(out, MODELWRITEMODE);
				out.close();
				LOG.info("Wrote "+ fetchurl + " to separate diff file.");
			} catch (FileNotFoundException e) {
				LOG.error("HtmlRawIndexer: Unable to write Diff model to file:" + e.getMessage());
				writePerformanceToLog("ERROR_WRITING_TO_SEPARATE_FILE", measurementsMap, fetchurl);
				return doc;
			}

		}
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_DiffsToFiles", new Long(endTime - startTime));
		//--- End of diff to file output. ---

		

		
		//--- Performance comparison : Diff xmls using diffxml xmdiff. ---
		startTime = System.currentTimeMillis();
		int smallestXmDiffDist = -1;
		try{
			smallestXmDiffDist = performXMdiff(oldDoc,newDoc, fetchurl);
		}catch(Exception e){
			LOG.error("Error performing xmdiff : " + e.getMessage());
			writePerformanceToLog("ERROR_XMDIFF", measurementsMap, fetchurl);
			return doc;
		}
		endTime = System.currentTimeMillis();
		measurementsMap.put("time_xmdiff", new Long(endTime - startTime));
		measurementsMap.put("dist_xmdiff", new Long(smallestXmDiffDist));
		
		
		
		writePerformanceToLog("SUCCESS", measurementsMap, fetchurl);
		return doc;
	}


	/**
	 * Performs xmdiff on two documents. Creates temporary files for that as xmdiff uses external memory.
	 * Code extracted and modified from XMDiff.java in diffxml by Adrian Mouat
	 * @param oldDoc one Document
	 * @param newDoc another Document
	 * @param uri TODO
	 * @throws XmlPullParserException 
	 */
	private int performXMdiff(Document oldDoc, Document newDoc, String uri) 
			throws TransformerConfigurationException, TransformerFactoryConfigurationError, 
			IOException, TransformerException, XmlPullParserException {

		XmDiff xmdiff = new XmDiff();
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);

		XmlPullParser doc1 = factory.newPullParser();
		XmlPullParser doc2 = factory.newPullParser();

		File newDocTempFile = outputXMLToTempFile(newDoc, "newDoc");
		File oldDocTempFile = outputXMLToTempFile(oldDoc, "oldDoc");
		newDocTempFile.deleteOnExit();
		oldDocTempFile.deleteOnExit();
		String f1 = newDocTempFile.getAbsolutePath();
		String f2 = oldDocTempFile.getAbsolutePath();
		
		doc1.setInput ( new FileReader ( f1 ) );
		doc2.setInput ( new FileReader ( f2 ) );

		//Intermediate temp files
		//Create output file
		File tmp1 = File.createTempFile("xdiff",null,null);
		File tmp2 = File.createTempFile("xdiff",null,null);
//		File out = File.createTempFile("xdiff",null,null);
		tmp1.deleteOnExit();
		tmp2.deleteOnExit();
		RandomAccessFile fA = new RandomAccessFile(tmp1, "rw");
		RandomAccessFile fB = new RandomAccessFile(tmp2, "rw");

		//Algorithm mmdiff
		int D[][] = new int[1024][1024];	
		D[0][0]=0;
		
		//Calculate delete costs
		//Returns number of nodes in doc1
	        int num_doc1=xmdiff.delcosts(doc1, D, fA);
		
		//Calculate insert costs
		//Returns number of nodes in doc2
		int num_doc2=xmdiff.inscosts(doc2, D, fB);

		//Calculate everything else
		//Need to reset inputs
		//doc1.setInput ( new FileReader ( args [0] ) );
	        //doc2.setInput ( new FileReader ( args [1] ) );
		//Need to be able to reset parser so pass filename with parser
		xmdiff.allcosts(doc1, f1, num_doc1, doc2, f2, num_doc2, D);
		
		int smallestLength = D[num_doc1-1][num_doc2-1];
		
		newDocTempFile.delete();
		oldDocTempFile.delete();
		
		return smallestLength;
	}

	
//	/**
//	 * 
//	 * @param D
//	 * @param size1 dim1 of array
//	 * @param size2 dim2 of array
//	 * @return Minimal distance between 
//	 */
//	private int processXmdiffResult(int[][] D, int size1, int size2){
//		int shortestLength = Integer.MAX_VALUE;
////		int shortesti = 0;
////		int shortestj = 0;
////		for (int i = 0; i < size1; i ++){
////			for (int j = 0; j < size2; i ++){
////				if(D[i][j] < shortestLength & i>= shortesti & j >= shortestj){
////					
////				}
////			}
////		}
//		shortestLength = D[size1-1][size2-1];
//		return shortestLength;
//	}


	/**
	 * Outputs XML of Document to a temporary file. Returns the temp file handle.
	 * @param filename name of output file. ".tmp" suffix is appended automatically.
	 * @param Document doc Document object containing XML.
	 * @return temporary File handle.
	 */
	private File outputXMLToTempFile(Document xmlDoc, String filename)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, IOException,
			TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		DOMSource source = new DOMSource(xmlDoc);
		File tempFile = File.createTempFile(filename, ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile, false);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		StreamResult result = new StreamResult(osw);
		transformer.transform(source, result);
		
		
//		//XXX: DEBUG
//		File debug_f = new File(filename+"debug");
//		FileOutputStream debug_fos = new FileOutputStream(debug_f, false);
//		OutputStreamWriter debug_osw = new OutputStreamWriter(debug_fos, "UTF-8");
//		StreamResult debugresult = new StreamResult(debug_osw);
//		transformer.transform(source, debugresult);
//		debug_fos.close();
//		//END of debug
		
		return tempFile;
	}

	
	/**
	 * Outputs to log file the time spent at each step of the process.
	 * @param measurementsMap Map<String, Long> containing measurement name and duration pairs.
	 * @param fetchurl String of URL of the page being processed. 
	 */
	private void writePerformanceToLog(String message, Map<String, Long> measurementsMap, String fetchurl){
		if (LOG_PERFORMANCE_TO_FILE == true){
			try {
				String line = 
						message + "\t" + 
						fetchurl + "\t" + 		
						measurementsMap.get("doc_size") + "\t" + 
						measurementsMap.get("time_DBread") + "\t" + 
						measurementsMap.get("time_DBwrite") + "\t" +
						measurementsMap.get("time_parseXML") + "\t" + 
						measurementsMap.get("time_xmdiff") + "\t" + 
						measurementsMap.get("dist_xmdiff") + "\t" + 
						measurementsMap.get("time_diffXML") + "\t" + 
						measurementsMap.get("time_JenaModel") + "\t" + 
						measurementsMap.get("time_DiffsToPOST") + "\t" + 
						measurementsMap.get("time_DiffsToFiles");
				performanceLog.info(line);
			} catch (Exception e) {
				LOG.info("Failed to save performance to file :" + e.getMessage());
			}
		}
	}

	/**
	 * Convenience method for assigning values from Nutch configuration to local variables.
	 * @param localVariable name of local String variable
	 * @param paramName name of parameter in nutch-site.xml
	 * @param conf Nutch Configuration object
	 * @return value of parameter. If null, localVariable.
	 */
	private String assignValuesFromConfig(String localVariable, String paramName, Configuration conf){
		try{
			if (conf.get(paramName) != null){
				return conf.get(paramName);
			}else{
				LOG.warn("No value for "+ paramName +" found in config. Using default value " + localVariable);
				return localVariable;
			}
		}
		catch (Exception e){
			LOG.warn("Error finding value for "+ paramName +" in config. Using default value " + localVariable);
			return localVariable;
		}
	}

	/**
	 * Convenience method for assigning values from Nutch configuration to local variables.
	 * @param localVariable name of local short variable
	 * @param paramName name of parameter in nutch-site.xml
	 * @param conf Nutch Configuration object
	 * @return value of parameter. If null, localVariable.
	 */
	private short assignValuesFromConfig(short localVariable, String paramName, Configuration conf){
		try{
			if (conf.get(paramName) != null){
				return Short.parseShort(conf.get(paramName));
			}else{
				LOG.warn("No value for "+ paramName +" found in config. Using default value " + localVariable);
				return localVariable;
			}
		}
		catch (Exception e){
			LOG.warn("Error finding value for "+ paramName +" in config. Using default value " + localVariable);
			return localVariable;
		}
	}

	/**
	 * Convenience method for assigning values from Nutch configuration to local variables.
	 * @param localVariable name of local boolean variable
	 * @param paramName name of parameter in nutch-site.xml
	 * @param conf Nutch Configuration object
	 * @return value of parameter. If not "true" or "false" in config, localVariable.
	 */
	private boolean assignValuesFromConfig(boolean localVariable, String paramName, Configuration conf){	
		try{
			if (conf.get(paramName).equals("true")){
				return true;
			}
			else if (conf.get(paramName).equals("false")){
				return false;
			}
			else{
				LOG.warn("No value for "+ paramName +" found in config. Using default value " + localVariable);
				return localVariable;
			}
		}
		catch (Exception e){
			LOG.warn("Error finding value for "+ paramName +" in config. Using default value " + localVariable);
			return localVariable;
		}
	}
	
	/** Creates HTML POST request and sends it to pubsub server.
	 * 
	 * @param diffsString found diffs in string form (assumed following http://vocab.deri.ie/diff ontology), without error checking
	 */
	private void createAndSendPOSTRequest(String diffsString) {
		if (POST_DO_SEND_REQUEST == false){
			return;
		}
		
		try {	  
			String urlParameters = "diffs=" + diffsString;
			String urlString = RABBIT_IP;
			URL serverUrlObj;
			serverUrlObj = new URL("http://"+urlString);
			HttpURLConnection connection = (HttpURLConnection) serverUrlObj.openConnection();           
			connection.setDoOutput(true);
			//		  connection.setDoInput(true);
			//		  connection.setInstanceFollowRedirects(false); 
			connection.setRequestMethod("POST"); 
			//		  connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			connection.setRequestProperty("charset", "utf-8");
			//		  connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			//		  connection.setUseCaches (false);

			DataOutputStream wr = new DataOutputStream( connection.getOutputStream() );
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			int responseCode = connection.getResponseCode();
			LOG.info("DIFF POST response code: " + responseCode);
			connection.disconnect(); 
		} catch (MalformedURLException e) {
			LOG.error("Malformed URL exception in htmlrawindexer POST : " + e.getMessage());
		} catch (IOException e) {
			LOG.error("IOException in htmlrawindexer POST : " + e.getMessage());
		}
	}

	/** Creates Jena model of changeset.
	 * 
	 * @param listOfXMLDiffs List of XMLUnit differences.
	 * @param fetchurl URL of the diffed page
	 * @param oldUnixTime the UNIX time of the previous fetch of this page
	 * @param fetchUnixTime the UNIX time of this fetch of this page
	 * @param dbOldDoc MongoDB database object of the previous fetch of this page
	 * @return Jena Model object of the created RDF.
	 */
	private Model createDiffRdf (List<Difference> listOfXMLDiffs, String fetchurl, long oldUnixTime, long fetchUnixTime, BasicDBObject dbOldDoc){  
		// make sure that fetch url ends with /
		if( fetchurl.charAt( fetchurl.length() -1 ) != '/'){
			fetchurl.concat("/");
		}

		String urlWithDiffAndTimes = fetchurl + "diff/" + oldUnixTime + "-" + fetchUnixTime;
		Model model = ModelFactory.createDefaultModel();

		/*	      
	      #Use diff ontology available at: http://vocab.deri.ie/diff by using the N-Triples syntax described at: http://www.w3.org/TR/n-triples/ .
		  #Namespace: http://vocab.deri.ie/diff#

		  #To construct identifiers for diffs (sets of changes between documents), add suffix "/diff/<timestamp1>-<timestamp2>" to URLs of monitored Web pages, where timestamp1 and timestamp2 are correspondingly the numbers of seconds since Unix epoch (also called Unix time) identifying timestamp of retrieving the previous and current version of a Web page. Thus the identifier of diff of page http://www.postimees.ee retrievad at Unix times 1396187504 and 1396187534 will be http://www.postimees.ee/diff/1396187504-1396187534.

		  <http://www.postimees.ee/diff/1396187504-1396187534> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#Diff> .# State that http://www.postimees.ee/diff/1396187504-1396187534 is a diff instance
		  <http://www.postimees.ee/diff/1396187504-1396187534> <http://vocab.deri.ie/diff#objectOfChange> <http://www.postimees.ee/1396187504> .# Property used to link the "Diff" object to the next version of the document created by this diff. http://www.postimees.ee/1396187504 is the identifier of http://www.postimees.ee Web page at time 1396187504.
		  <http://www.postimees.ee/diff/1396187504-1396187534> <http://vocab.deri.ie/diff#subjectOfChange> <http://www.postimees.ee/1396187534> .# Property used to link the "Diff" object to the previous version of the document changed by this diff.
		 */

		Resource urlWithDiffAndTimesResource = 
				model.createResource(urlWithDiffAndTimes)
				.addProperty(
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
						"http://vocab.deri.ie/diff#Diff"
						)
						.addProperty(
								model.createProperty("http://vocab.deri.ie/diff#objectOfChange"), 
								fetchurl + oldUnixTime
								)
								.addProperty(
										model.createProperty("http://vocab.deri.ie/diff#subjectOfChange"), 
										fetchurl + fetchUnixTime
										);

		int nrOfInsertsAndDeletes = 0;
		for (Difference thisDiff:listOfXMLDiffs){
			/*
			 #To construct identifiers for individual diff components add suffix "/N" to diff identifier, whereas N will be the sequence number of a diff component. State explicitly diff components for reference deletion and insertion, sentence deletion and insertion.

			 <http://www.postimees.ee/diff/1396187504-1396187534/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#ReferenceDeletion> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#ReferenceInsertion> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/3> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#SentenceDeletion> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/4> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#SentenceInsertion> .

			 #Use http://purl.org/dc/terms/hasPart relation to bind components of diffs to a particular diff object:

			 <http://www.postimees.ee/diff/1396187504-1396187534> <http://purl.org/dc/terms/hasPart> <http://www.postimees.ee/diff/1396187504-1396187534/1> .
		 	 <http://www.postimees.ee/diff/1396187504-1396187534> <http://purl.org/dc/terms/hasPart> <http://www.postimees.ee/diff/1396187504-1396187534/2> .
			 <http://www.postimees.ee/diff/1396187504-1396187534> <http://purl.org/dc/terms/hasPart> <http://www.postimees.ee/diff/1396187504-1396187534/3> .
			 <http://www.postimees.ee/diff/1396187504-1396187534> <http://purl.org/dc/terms/hasPart> <http://www.postimees.ee/diff/1396187504-1396187534/4> .    

		     #Removed and added fragments are represented as text objects. To identify these objects use suffixes "/addition" and "/removal". For instance added and removed fragments of diff component http://www.postimees.ee/diff/1396187504-1396187534/1 will be respectively http://www.postimees.ee/diff/1396187504-1396187534/1/addition and http://www.postimees.ee/diff/1396187504-1396187534/1/removal.

		     <http://www.postimees.ee/diff/1396187504-1396187534/1> <http://vocab.deri.ie/diff#removal> <http://www.postimees.ee/diff/1396187504-1396187534/1/removal> .
		     <http://www.postimees.ee/diff/1396187504-1396187534/2> <http://vocab.deri.ie/diff#addition> <http://www.postimees.ee/diff/1396187504-1396187534/2/addition> .
		     <http://www.postimees.ee/diff/1396187504-1396187534/3> <http://vocab.deri.ie/diff#removal> <http://www.postimees.ee/diff/1396187504-1396187534/3/removal> .
		     <http://www.postimees.ee/diff/1396187504-1396187534/4> <http://vocab.deri.ie/diff#addition> <http://www.postimees.ee/diff/1396187504-1396187534/4/addition> .

			 #Identify that these objects are text blocks

		     <http://www.postimees.ee/diff/1396187504-1396187534/1/removal> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#TextBlock> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/2/addition> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#TextBlock> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/3/removal> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#TextBlock> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/4/addition> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.deri.ie/diff#TextBlock> .

			 #Determine content of these text blocks

			 <http://www.postimees.ee/diff/1396187504-1396187534/1/removal> <http://vocab.deri.ie/diff#content> "http://tallinncity.postimees.ee/2740048/juri-ennet-savisaar-on-eesti-vabariigi-aiaisand-ja-erakondliku-sumfooniaorkestri-dirigent" .
			 <http://www.postimees.ee/diff/1396187504-1396187534/2/addition> <http://vocab.deri.ie/diff#content> "http://e24.postimees.ee/2737324/arengufondi-rahastamisotsus-sundis-hamaratel-asjaoludel" .
			 <http://www.postimees.ee/diff/1396187504-1396187534/3/removal> <http://vocab.deri.ie/diff#content> "Riigil ja rahval" .
			 <http://www.postimees.ee/diff/1396187504-1396187534/4/addition> <http://vocab.deri.ie/diff#content> "Kooliharidus" .

			 #Determine location (line number) of these text blocks

			 <http://www.postimees.ee/diff/1396187504-1396187534/1/removal> <http://vocab.deri.ie/diff#lineNumber> "112"^^<http://www.w3.org/2001/XMLSchema#integer> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/2/addition> <http://vocab.deri.ie/diff#lineNumber> "111"^^<http://www.w3.org/2001/XMLSchema#integer> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/3/removal> <http://vocab.deri.ie/diff#lineNumber> "4"^^<http://www.w3.org/2001/XMLSchema#integer> .
			 <http://www.postimees.ee/diff/1396187504-1396187534/4/addition> <http://vocab.deri.ie/diff#lineNumber> "4"^^<http://www.w3.org/2001/XMLSchema#integer> .
			 */



			String newFragmentContent;
			String oldFragmentContent;	
			try{
				newFragmentContent = thisDiff.getTestNodeDetail().getValue();
			}catch(Exception e){
				newFragmentContent = "";	// null content to blank string
			}
			try{
				oldFragmentContent = thisDiff.getControlNodeDetail().getValue();
			}catch(Exception e){
				oldFragmentContent = "";	// null content to blank string
			}

			//--- if reference/link ---
			
			//XXX: Some nodenames are null, therefore aren't legal links
			String nodeName = "";
			try{
				nodeName = thisDiff.getControlNodeDetail().getNode().getNodeName();
			}
			catch (NullPointerException e){
				if (DIFF_LOG_NULL_WARNINGS){
					LOG.warn("Diff to Jena: getNodeName() returns null");
				}
			}
			if (nodeName.compareTo("href") == 0 || 
					nodeName.compareTo( "src") == 0){
				nrOfInsertsAndDeletes += 1;
				Resource urlWithDiffSequenceNumberResource =
						model.createResource(urlWithDiffAndTimes + "/" + nrOfInsertsAndDeletes);
				urlWithDiffAndTimesResource.addProperty(			// Identify this diff as part of changeset
						model.createProperty("http://purl.org/dc/terms/hasPart"),
						urlWithDiffSequenceNumberResource
						);			
				// create addition fragment
				urlWithDiffSequenceNumberResource.addProperty(		// Identify diff as ReferenceInsertion
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
						"http://vocab.deri.ie/diff#ReferenceInsertion"
						);			 
				Resource urlWithDiffSequenceAndAdditionFragmentResource = model.createResource(urlWithDiffSequenceNumberResource + "/addition");
				urlWithDiffSequenceNumberResource.addProperty(		// Identify fragment as addition
						model.createProperty("http://vocab.deri.ie/diff#addition"), 
						urlWithDiffSequenceAndAdditionFragmentResource
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Identify addition fragment content as text
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						"http://vocab.deri.ie/diff#TextBlock"
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Determine addition fragment content
						model.createProperty("http://vocab.deri.ie/diff#content"),
						newFragmentContent
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Determine addition fragment location XXX:Currently XPATH (not in vocabulary) 
						model.createProperty("http://vocab.deri.ie/diff#lineNumber"),
						thisDiff.getTestNodeDetail().getXpathLocation()
						);

				// create removal fragment
				nrOfInsertsAndDeletes += 1;
				urlWithDiffSequenceNumberResource =
						model.createResource(urlWithDiffAndTimes + "/" + nrOfInsertsAndDeletes);


				urlWithDiffAndTimesResource.addProperty(				// Identify this diff as part of changeset
						model.createProperty("http://purl.org/dc/terms/hasPart"),
						urlWithDiffSequenceNumberResource
						);
				urlWithDiffSequenceNumberResource.addProperty(		// Identify diff as ReferenceDeletion
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
						"http://vocab.deri.ie/diff#ReferenceDeletion"
						);
				Resource urlWithDiffSequenceAndRemovalFragmentResource = model.createResource(urlWithDiffSequenceNumberResource + "/removal");
				urlWithDiffSequenceNumberResource.addProperty(		// Identify fragment as removal
						model.createProperty("http://vocab.deri.ie/diff#removal"), 
						urlWithDiffSequenceAndRemovalFragmentResource
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Identify removal fragment content as text
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						"http://vocab.deri.ie/diff#TextBlock"
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Determine removal fragment content
						model.createProperty("http://vocab.deri.ie/diff#content"),
						oldFragmentContent
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Determine removal fragment location XXX:Currently XPATH (not in vocabulary)
						model.createProperty("http://vocab.deri.ie/diff#lineNumber"),
						thisDiff.getControlNodeDetail().getXpathLocation()
						);

			}
			//--- if text ---
			else if (thisDiff.getDescription().compareTo("text value") == 0){
				// ignore minor differences
				LinkedList <Diff> textDiffs = new LinkedList<Diff>();
				DiffMatchPatch diffMatchPatchObj = new DiffMatchPatch();
				diffMatchPatchObj.Diff_EditCost = DIFFEDITCOST;
				diffMatchPatchObj.Diff_Timeout = 0;
				textDiffs = diffMatchPatchObj.diff_main(
						oldFragmentContent,
						newFragmentContent
						);
				if (diffMatchPatchObj.diff_levenshtein(textDiffs) < MINIMUMDIFFDISTANCE){
					continue;
				}

				nrOfInsertsAndDeletes += 1;
				Resource urlWithDiffSequenceNumberResource =
						model.createResource(urlWithDiffAndTimes + "/" + nrOfInsertsAndDeletes);
				urlWithDiffAndTimesResource.addProperty(			// Identify this diff as part of changeset
						model.createProperty("http://purl.org/dc/terms/hasPart"),
						urlWithDiffSequenceNumberResource
						);
				// create addition fragment
				urlWithDiffSequenceNumberResource.addProperty(		// Identify diff as SentenceInsertion
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
						"http://vocab.deri.ie/diff#SentenceInsertion"
						);
				Resource urlWithDiffSequenceAndAdditionFragmentResource = model.createResource(urlWithDiffSequenceNumberResource + "/addition");
				urlWithDiffSequenceNumberResource.addProperty(		// Identify fragment as addition
						model.createProperty("http://vocab.deri.ie/diff#addition"), 
						urlWithDiffSequenceAndAdditionFragmentResource
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Identify addition fragment content as text
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						"http://vocab.deri.ie/diff#TextBlock"
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Determine addition fragment content
						model.createProperty("http://vocab.deri.ie/diff#content"),
						newFragmentContent
						);
				urlWithDiffSequenceAndAdditionFragmentResource.addProperty(   // Determine addition fragment location XXX:Currently XPATH (not in vocabulary) 
						model.createProperty("http://vocab.deri.ie/diff#lineNumber"),
						thisDiff.getTestNodeDetail().getXpathLocation()
						);
				// create removal fragment
				nrOfInsertsAndDeletes += 1;
				urlWithDiffSequenceNumberResource =
						model.createResource(urlWithDiffAndTimes + "/" + nrOfInsertsAndDeletes);
				urlWithDiffAndTimesResource.addProperty(				// Identify this diff as part of changeset
						model.createProperty("http://purl.org/dc/terms/hasPart"),
						urlWithDiffSequenceNumberResource
						);
				urlWithDiffSequenceNumberResource.addProperty(		// Identify diff as SentenceDeletion
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
						"http://vocab.deri.ie/diff#SentenceDeletion"
						);
				Resource urlWithDiffSequenceAndRemovalFragmentResource = model.createResource(urlWithDiffSequenceNumberResource + "/removal");
				urlWithDiffSequenceNumberResource.addProperty(		// Identify fragment as removal
						model.createProperty("http://vocab.deri.ie/diff#removal"), 
						urlWithDiffSequenceAndRemovalFragmentResource
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Identify removal fragment content as text
						model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
						"http://vocab.deri.ie/diff#TextBlock"
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Determine removal fragment content
						model.createProperty("http://vocab.deri.ie/diff#content"),
						oldFragmentContent
						);
				urlWithDiffSequenceAndRemovalFragmentResource.addProperty(   // Determine removal fragment location XXX:Currently XPATH (not in vocabulary)
						model.createProperty("http://vocab.deri.ie/diff#lineNumber"),
						thisDiff.getControlNodeDetail().getXpathLocation()
						);
			}

		} // end loop	

		return model;
	} // end createDiffRdf

	/**
	 * Debugging method for writing all XMLUnit diffs to console.
	 * @param listOfXmlDiffs
	 */
	private void writeAllXMLUnitDiffsToLog(List<Difference> listOfXmlDiffs) {
		for (Difference thisDifference : listOfXmlDiffs){
			try{
				LOG.info("Difference ID: " + thisDifference.getId());
				LOG.info("Difference description value: " + thisDifference.getDescription());
				LOG.info("Control node nodename: " + thisDifference.getControlNodeDetail().getNode().getNodeName());				
				LOG.info("Test node nodename: " + thisDifference.getTestNodeDetail().getNode().getNodeName());
				if(thisDifference.getControlNodeDetail().getValue() != null){
					LOG.info("Control node value: " + thisDifference.getControlNodeDetail().getValue());
				}
				if(thisDifference.getTestNodeDetail().getValue() != null){
					LOG.info("Test node value: " + thisDifference.getTestNodeDetail().getValue());
				}
				LOG.info("Control node XPATH: " + thisDifference.getControlNodeDetail().getXpathLocation());
				LOG.info("Test node XPATH: " + thisDifference.getTestNodeDetail().getXpathLocation());
			}catch(Exception e){
				LOG.info("Failed to write to log :" + e.getMessage());
			}
		}
	}

	
	/**
	 * Sets values based on config. Called from outside, in filter application. 
	 */

	public void setConf(Configuration conf) {
		// Retrieve configuration values.
		Configuration nutchConf = NutchConfiguration.create();
		MONGODB_IP = assignValuesFromConfig(MONGODB_IP,"htmlrawindexer.mongodb.ip",nutchConf);
		MONGODB_PORT = assignValuesFromConfig(MONGODB_PORT,"htmlrawindexer.mongodb.port",nutchConf);
		MONGODB_DBNAME = assignValuesFromConfig(MONGODB_DBNAME,"htmlrawindexer.mongodb.dbname",nutchConf);
		MONGODB_COLLNAME = assignValuesFromConfig(MONGODB_COLLNAME,"htmlrawindexer.mongodb.collectionname",nutchConf);
		RABBIT_IP = assignValuesFromConfig(RABBIT_IP,"htmlrawindexer.hub.ip",nutchConf);
		DIFFEDITCOST = assignValuesFromConfig(DIFFEDITCOST,"htmlrawindexer.diffmatchpatch.editcost",nutchConf);
		MINIMUMDIFFDISTANCE = assignValuesFromConfig(MINIMUMDIFFDISTANCE,"htmlrawindexer.diffmatchpatch.minimumdistance",nutchConf);
		MODELWRITEMODE = assignValuesFromConfig(MODELWRITEMODE,"mongodb.jena.writemode",nutchConf);
		OUTPUT_WRITE_DIFFS_TO_SEPARATE_FILES = assignValuesFromConfig(OUTPUT_WRITE_DIFFS_TO_SEPARATE_FILES,"htmlrawindexer.output.writediffstoseparatefiles",nutchConf);
		OUTPUT_WRITE_DIFFS_TO_SINGLE_FILE = assignValuesFromConfig(OUTPUT_WRITE_DIFFS_TO_SINGLE_FILE,"htmlrawindexer.output.writediffstosinglefile",nutchConf);
		POST_DO_SEND_REQUEST = assignValuesFromConfig(POST_DO_SEND_REQUEST,"htmlrawindexer.post.dopostrequest",nutchConf);
		DIFF_LOG_NULL_WARNINGS = assignValuesFromConfig(DIFF_LOG_NULL_WARNINGS,"htmlrawindexer.debug.logdiffnulls",nutchConf);
		LOG_PERFORMANCE_TO_FILE = assignValuesFromConfig(LOG_PERFORMANCE_TO_FILE,"htmlrawindexer.performance.logperformancetofile",nutchConf);
		SYSTEM_OUT_FILENAME = assignValuesFromConfig(SYSTEM_OUT_FILENAME,"htmlrawindexer.systemout.filename",nutchConf);
		String logPerformanceToFilenameString = "performance_measurements.log";
		logPerformanceToFilenameString = assignValuesFromConfig(logPerformanceToFilenameString,"htmlrawindexer.performance.logperformancefilename",nutchConf);
		
		// Check MongoDB connection. Error out if unable to.  
		if (db == null || db.getName() != MONGODB_DBNAME ){
			try {
				mongoClient = new MongoClient(MONGODB_IP, MONGODB_PORT);
				db = mongoClient.getDB(MONGODB_DBNAME);
			} catch (UnknownHostException e1) {
				LOG.error("Could not connect to MongoDB (Unknown Host Exception)");
			}
		}

		// Performance log file handler : if output file is already handled, don't add again.

		try {
			Handler[] handlerArray = performanceLog.getHandlers();
			boolean hasFileHandler = false;
			for (Handler oneHandler:handlerArray){
				if (oneHandler.getClass().equals(FileHandler.class)){
					hasFileHandler = true;
					break;
				};
			}
			if (hasFileHandler == false){
				FileHandler fh = new FileHandler(logPerformanceToFilenameString, true);
				fh.setFormatter(new SimpleFormatter());
				performanceLog.addHandler(fh);
				performanceLog.setUseParentHandlers(false);
			}
			File perfLogFile = new File(logPerformanceToFilenameString);
			if(perfLogFile.exists() == false){
				String colNames = "";
				for (String oneColName:perflogColumnNames){
					colNames = colNames += oneColName + "\t";
				}
				colNames.trim();
				performanceLog.info(colNames);
			}
		} catch (Exception e) {
			LOG.error("Error confing file handler for performance logger : " + e.getMessage());

		}
		
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}

	@Override
	public void addIndexBackendOptions(Configuration conf) {
		LuceneWriter.addFieldOptions("crawldate", LuceneWriter.STORE.YES,LuceneWriter.INDEX.NO, conf);
		LuceneWriter.addFieldOptions("lastdate", LuceneWriter.STORE.YES,LuceneWriter.INDEX.NO, conf);

	}

}
