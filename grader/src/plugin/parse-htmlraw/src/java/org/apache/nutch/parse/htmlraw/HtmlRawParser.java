package org.apache.nutch.parse.htmlraw;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;

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
import java.util.HashMap;

import java.lang.management.ManagementFactory;

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
		
		Integer rand;
		int min = 1;
		int max = 10000;
		do {
			rand = new Random().nextInt((max - min) + 1) + min;
		} while (new File("TempFile-" + rand + ".html").exists());
		File f = new File("TempFile-" + rand + ".html");
		//System.out.println(f.getAbsolutePath() + " created");
		
		//Write to a TempFile
		FileWriter fw = new FileWriter(f.getAbsolutePath());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(htmlraw);
		bw.close();
		fw.close();
		
		//Java VM process id
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		
		//nutchWAX process id
		Process process = new ProcessBuilder("ps", "-o", "ppid=", "-p", pid.split("@")[0]).start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
	        String ppid = br.readLine();
		ppid = ppid.replace(" ", "");
		
		//time process id
		process = new ProcessBuilder("ps", "-o", "ppid=", "-p", ppid).start();
		is = process.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);
		String pppid = br.readLine();
		pppid = pppid.replace(" ", "");
		
		//run.sh process id
		process = new ProcessBuilder("ps", "-o", "ppid=", "-p", pppid).start();
		is = process.getInputStream();
		isr = new InputStreamReader(is);
		br = new BufferedReader(isr);
		String ppppid = br.readLine();
		ppppid = ppppid.replace(" ", "");
		/*
		System.out.println("java finds pid = '" + pid + "'");
		System.out.println("java finds ppid = '" + ppid + "'");
		System.out.println("java finds pppid = '" + pppid + "'");
		*/
		System.out.println("java finds ppppid = '" + ppppid + "'");
		
		BufferedReader br2 = new BufferedReader(new FileReader("./" + ppppid + ".txt"));
		
		//Read the warc name
		String warc = br2.readLine();
		//Warc creation date
		String date = warc.substring(4,12);
		System.out.println(warc);
		date = date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6,7);
		System.out.println(date);
		br2.close();

		//Make a decision based on the conf file located in root folder
		Properties prop = new Properties();
		is = new FileInputStream("plugin-conf.cfg");
		prop.load(is);
		String grader = prop.getProperty("GRADER");
		br.close();
		is.close();
		isr.close();
		/*
		 * Audit
		 */
		
		Parser p = new Parser();
		Uploader sql = new Uploader();
		JSONObject j = new JSONObject();

		URL domUrl = new URL(content.getUrl());
		//System.out.println(domUrl);
		
		if (grader.equals("AL")) {
			try {
				j = p.accessLint(f.getAbsolutePath());
			} catch (JSONException e) {
				e.printStackTrace();
			}
				if(sql.postGradesAccess(j, domUrl.getHost(), domUrl.getFile()) == true)
				System.out.println("upload successful");
			else
				System.out.println("upload failed");;
		} else if (grader.equals("HTML")) {
			String[] standards = {"A", "AA", "AAA"};
			for (String std : standards) {
				HashMap<String, String> array = p.pa11y(f.getAbsolutePath(), warc, domUrl.getHost()+domUrl.getFile(), std);
				array.put("warcDate", date);
				if (array != null)
					sql.postGradesCodeSniffer(array, domUrl.getHost(), domUrl.getFile());
			}
				//if (sql.postGradesCodeSniffer(array, domUrl.getHost(), domUrl.getFile()) == true)
				//	System.out.println("upload successful");
				//else
				//	System.out.println("upload failed");
		}
		f.delete();
//		if (f.delete())
//			System.out.println("\t and file deleted.");
//		else System.out.println("\t but file not deleted.");
	} catch (FileNotFoundException e) {
		System.out.println(e.getMessage());
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} catch (JSONException e) {
		e.printStackTrace();
	}
	
	//System.out.print(".");
    return parseResult;
  }
}
