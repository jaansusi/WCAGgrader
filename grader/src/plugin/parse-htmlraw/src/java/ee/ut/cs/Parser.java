package ee.ut.cs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.*;
import java.util.Iterator;
import java.util.HashSet;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.NoSuchMethodError;
import java.util.regex.PatternSyntaxException;


public class Parser {
	public JSONObject accessLint (String fileAddress) throws IOException, JSONException {
		//Audit the given file
		//>----------------------------------------------->
		//System.out.println(fileAddress);
		
		//TO-DO
		//Work out a workflow that does not require two separate ifs
		Process process = new ProcessBuilder("access_lint", "audit", fileAddress).start();
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		String answer = "";
		br.readLine();
		while ((line = br.readLine()) != null) {
			if (line != "complete")
				answer += line.replaceAll("=>", ":") + "\n";
		}
		JSONObject json = null;
		try	{
			json = new JSONObject(answer);
		} catch (JSONException e) {
			System.out.println(answer);
			throw new JSONException(e);
		}
		//json.keySet();
		
		//<-----------------------------------------------<
		
        
        
        //Return results
        //>----------------------------------------------->
        /*
         * Find database values based on the key below and return the answer
         */
        JSONArray json2;
        JSONObject jsonAns = new JSONObject();
        
		System.out.println(JSONObject.getNames(json));
		String jsonTemp;
		JSONObject jsonTranslate = getTransformer();
        for (String el : json.keySet()) {
        	System.out.println(el);
        	json2 = json.getJSONArray(el);
        	for (int i = 0; i < json2.length(); i++) {
        		jsonTemp = new JSONObject(json2.get(i).toString()).get("title").toString();
        		//System.out.println(jsonTranslate.get(jsonTemp));
        		jsonAns.append(jsonTranslate.get(jsonTemp).toString(), el);
        	}
		}
		
		return jsonAns;
		//<-----------------------------------------------<
	}

	public String[] pa11y(String fileAddress) throws IOException, JSONException {

		Process process = new ProcessBuilder("pa11y", "-r", "json", "file://" + fileAddress).start();
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		String output = "";
		while ((line = br.readLine()) != null) {
			output += line;
		}

		//TO-DO Remove this horrible ghetto rig

		//Remove the first and last 2 symbols
		//Then split it into different pieces at },{
		//Then add {} around the pieces and we have a list of JSONObjects
		System.out.println("Output len is: " + output.length());
		if (output.length() == 0) {
			return null;
		}
		output = output.substring(2, output.length()-2);
		
		ArrayList<JSONObject> jsonArray = new ArrayList<JSONObject>();

		for (String str : output.split("\\},\\{")) {
			try {
				jsonArray.add(new JSONObject("{" + str + "}"));
			} catch (JSONException e) {
				//For debug
				//System.out.println(str);
				e.printStackTrace();
			}

		}
		
		//JSONObject keys
		//[selector, message, context, typeCode, code, type]
		int i = 0, j = 0;
		//To understand how many different elements there are
		HashSet<String> uniqueErr = new HashSet<String>(), uniqueWarn = new HashSet<String>();
		for (JSONObject obj : jsonArray) {
			if (Integer.parseInt(obj.get("typeCode").toString()) == 1) {
				//System.out.println(obj.get("code"));
				i++;
				uniqueErr.add(obj.get("code").toString());
			}
			if (Integer.parseInt(obj.get("typeCode").toString()) == 2) {
				j++;
				uniqueWarn.add(obj.get("code").toString());
			}
		}

		System.out.println("Element count in answer set: " + jsonArray.size());
		System.out.println("Errors : " + i);
		System.out.println("Of those, unique count: " + uniqueErr.size());
		//Iterate over unique errors
		Iterator it = uniqueErr.iterator();
		System.out.println(" and they are: ");
		ArrayList<String> errors = new ArrayList<String>();
		while (it.hasNext()) {
			String next = it.next().toString();
			errors.add(next);
		}
		System.out.println();
		
		System.out.println("Warnings : " + j);
		System.out.println("Of those, unique count: " + uniqueWarn.size());
		//Iterate over unique warnings
		it = uniqueWarn.iterator();
		System.out.println(" and they are: ");
		ArrayList<String> warnings = new ArrayList<String>();
		while (it.hasNext()) {
			//Original output: WCAG2AA.Principle1.Guideline1_3.1_3_1.H48
			String next = it.next().toString();
			warnings.add(next);
		}
		
		//Parse the json
		
		String outputErr = parseSnifferOutput(warnings);
		String outputWarn = parseSnifferOutput(errors);
		
		//Return them
		
		String[] outputs = new String[2];
		outputs[0] = outputErr;
		outputs[1] = outputWarn;
		return outputs;
	}
	
	private String parseSnifferOutput(ArrayList<String> content) {
		
		//Cycle through the content (Errors/Warnings)
		
		String parsed = "";
		
		for (String next : content) {
			
			//To check whether output is right, uncomment next line
			//System.out.print("Original: " + next + " --- ");
			
			Pattern p = Pattern.compile("WCAG2(\\S+)\\.Principle(\\d)\\.Guideline(\\d)_(\\d)\\..*");
			Matcher m = p.matcher(next);
			
			if (m.find()) {
				
				//A, AA or AAA
				String WCAGlevel = m.group(1);
				System.out.print(WCAGlevel + " - ");
				
				//Principle and guideline number, e.g "1.1.3"
				String guideline = m.group(2) + "." + m.group(3) + "." + m.group(4);
				System.out.println(guideline);
				
				//Add to returning string
				
				parsed += "'" + WCAGlevel + guideline+ "',";
			}
		}
		//Cut out the last comma
		if (parsed.length() > 0)
			return parsed.substring(0, parsed.length() - 1);
		return null;
		
		
	}

	private JSONObject getTransformer() {

		/*
		 * Key to transforming values to database format for access_lint
		 */
		try {
			JSONObject jsonTranslate = new JSONObject("{"
        		+	"\"Elements with ARIA roles must use a valid, non-abstract ARIA role\" : \"ARIA-1\" ,"
        		+	"\"aria-labelledby attributes should refer to an element which exists in the DOM\" : \"ARIA-2\" ,"
        		+	"\"Elements with ARIA roles must have all required attributes for that role\" : \"ARIA-3\" ,"
        		+	"\"ARIA state and property values must be valid\" : \"ARIA-4\" ,"
        		+	"\"role=main should only appear on significant elements\" : \"ARIA-5\" ,"
        		+	"\"aria-owns should not be used if ownership is implicit in the DOM\" : \"ARIA-6\" ,"
        		+	"\"An element's ID must not be present in more that one aria-owns attribute at any time\" : \"ARIA-7\" ,"
        		+	"\"Elements with ARIA roles must ensure required owned elemenare present\" : \"ARIA-8\" ,"
        		+	"\"Elements with ARIA roles must be in the correct scope\" : \"ARIA-9\" ,"
        		+	"\"This element has an unsupported ARIA attribute\" : \"ARIA-10\" ,"
        		+	"\"This element has an invalid ARIA attribute\" : \"ARIA-11\" ,"
        		+	"\"This element does not support ARIA roles, states and properties\" : \"ARIA-12\" ,"
        		+	"\"Audio elements should have controls\" : \"AUDIO-1\" ,"
        		+	"\"Text elements should have a reasonable contrast ratio\" : \"COLOR-1\" ,"
        		+	"\"These elements are focusable but either invisible or obscured by another element\" : \"FOCUS-1\" ,"
        		+	"\"Elements with onclick handlers must be focusable\" : \"FOCUS-2\" ,"
        		+	"\"Avoid positive integer values for tabIndex\" : \"FOCUS-3\" ,"
        		+	"\"The web page should have the content's human language indicated in the markup\" : \"HTML-1\" ,"
        		+	"\"An element's ID must be unique in the DOM\" : \"HTML-2\" ,"
        		+	"\"Meaningful images should not be used in element backgrounds\" : \"IMAGE-1\" ,"
        		+	"\"Controls and media elements should have labels\" : \"TEXT-1\" ,"
        		+	"\"Images should have an alt attribute\" : \"TEXT-2\" ,"
        		+	"\"The purpose of each link should be clear from the link text\" : \"TEXT-4\" ,"
        		+	"\"The web page should have a title that describes topic or purpose\" : \"TITLE-1\" ,"
        		+	"\"Video elements should use <track> elements to provide captions\" : \"VIDEO-1\" "
        		+	"}");
			return jsonTranslate;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null; 
	}
}
