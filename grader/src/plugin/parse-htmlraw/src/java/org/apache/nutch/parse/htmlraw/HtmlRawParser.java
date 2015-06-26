package org.apache.nutch.parse.htmlraw;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.json.JSONObject;
import org.w3c.dom.DocumentFragment;

import ee.ut.cs.Parser;
import ee.ut.cs.Uploader;



import java.util.Random;


/** 
 * Parse raw HTML into metatag of document.
 ***/

public class HtmlRawParser implements HtmlParseFilter {

  private static final Log LOG = LogFactory.getLog(HtmlRawParser.class.getName());
  
  private Configuration conf;

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  public Configuration getConf() {
    return this.conf;
  }

  public ParseResult filter(Content content, ParseResult parseResult, 
		  HTMLMetaTags metaTags, DocumentFragment doc) {
	//Parse parse = parseResult.get(content.getUrl());
	//System.out.println("content.getUrl(): " + content.getUrl());
	//Metadata metadata = parse.getData().getParseMeta();
	//LOG.info(metadata);
	byte[] contentInOctets = content.getContent();
	String htmlraw = new String();
	try {
		//XXX: utf-8 only? could get encoding from parseresult?
		htmlraw = new String (contentInOctets,"UTF-8");
		//Create random file
		//>---------------------------------------------------------->
		Integer rand;
		int min = 1;
		int max = 10000;
		do {
			rand = new Random().nextInt((max - min) + 1) + min;
		} while (new File("TempFile-" + rand + ".html").exists());
		File f = new File("TempFile-" + rand + ".html");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(f.getAbsolutePath(), "UTF-8");
			writer.println(htmlraw);
			writer.close();

		//<----------------------------------------------------------<
		
		//Audit
		//>---------------------------------------------------------->
			Parser p = new Parser();
			//JSONObject j = p.accessLint(f.getAbsolutePath());
			Uploader sql = new Uploader();
			//LOG.info("This works");
			
			URL domUrl = new URL(content.getUrl());
			sql.postGrades(null, domUrl.getHost(), domUrl.getFile());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchMethodError e) {
			e.printStackTrace();
		}
		//<----------------------------------------------------------<
		
		//Delete created file
		//>---------------------------------------------------------->
		f.delete();
		//<----------------------------------------------------------<
		//LOG.info(htmlraw);
		
	} catch (UnsupportedEncodingException e) {
		LOG.error("unable to convert content into string");
	}
	
	//metadata.add("htmlraw", htmlraw);
    //LOG.info("Added parse meta tag: \"htmlraw\", length="+htmlraw.length());
    
    return parseResult;
  }
}
