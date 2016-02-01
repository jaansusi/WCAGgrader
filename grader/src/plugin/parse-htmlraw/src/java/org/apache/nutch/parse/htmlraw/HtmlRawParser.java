package org.apache.nutch.parse.htmlraw;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.json.JSONObject;
import org.json.JSONException;
import org.w3c.dom.DocumentFragment;

import ee.ut.cs.Parser;
import ee.ut.cs.Uploader;

import java.util.Properties;
import java.util.ArrayList;
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
		System.out.println(f.getAbsolutePath() + " created");
				
		try {
			FileWriter fw = new FileWriter(f.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(htmlraw);
			bw.close();
			fw.close();

		//<----------------------------------------------------------<
		
		//Make a decision based on the conf file located in root folder
			Properties prop = new Properties();
			InputStream is = new FileInputStream("plugin-conf.cfg");
			prop.load(is);
			String grader = prop.getProperty("GRADER");
			is.close();

		//Audit
		//>---------------------------------------------------------->
			Parser p = new Parser();
			Uploader sql = new Uploader();
			JSONObject j = new JSONObject();

			URL domUrl = new URL(content.getUrl());
			//System.out.println(grader);
			if (grader.equals("AL")) {
				j = p.accessLint(f.getAbsolutePath());
				sql.postGradesAccess(j, domUrl.getHost(), domUrl.getFile());
			} else if (grader.equals("HTML")) {
				ArrayList<JSONObject> array = p.pa11y(f.getAbsolutePath());
				if (array != null)
					if (sql.postGradesCodeSniffer(array, domUrl.getHost(), domUrl.getFile()) == true)
						System.out.println("upload successful");
					else
						System.out.println("upload failed");
			}
			

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		//<----------------------------------------------------------<
		
		//Delete created file
		//>---------------------------------------------------------->
		if (f.delete())
			System.out.println("\t and file deleted.");
		else System.out.println("\t but file not deleted.");
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
