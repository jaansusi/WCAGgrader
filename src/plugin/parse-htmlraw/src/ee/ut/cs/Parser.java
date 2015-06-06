package ee.ut.cs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;

import java.util.Set;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.lang.NoSuchMethodError;

public class Parser {
	public JSONObject accessLint (String fileAddress) throws IOException {
		//Audit the given file
		//>----------------------------------------------->
		System.out.println(fileAddress);
		Process process2 = new ProcessBuilder("access_lint", "audit", fileAddress).start();
		InputStream is = process2.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		String answer = "";
		br.readLine();
		while ((line = br.readLine()) != null) {
			//System.out.println(line);
			if (line != "complete")
				answer += line.replaceAll("=>", ":") + "\n";
		}
		
		//System.out.println("Answer is: " +answer);
		JSONObject json = new JSONObject(answer);
		try {
	        //json = new JSONObject(answer);
			System.out.println("Keyset= " + json.keySet());
		} catch (NoSuchMethodError e) {
			System.out.println("Answer = " + answer);
			e.printStackTrace();
			new File(fileAddress).delete();
			return null;
		}
		//<-----------------------------------------------<
		
        
        
        //Return results
        //>----------------------------------------------->
        //First find what we need, the codes and status
        JSONArray json2;
        JSONObject jsonAns = new JSONObject();
        String jsonTemp;
        for (String el : json.keySet()) {
        	System.out.println(el);
        	json2 = json.getJSONArray(el);
        	for (int i = 0; i < json2.length(); i++) {
        		jsonTemp = new JSONObject(json2.get(i).toString()).get("title").toString();
        		System.out.println(jsonTranslate.get(jsonTemp));
        		jsonAns.append(jsonTranslate.get(jsonTemp).toString(), el);
        	}
		}
        
		return jsonAns;
		//<-----------------------------------------------<
	}

//Key to uploading
//>----------------------------------------------->
private JSONObject jsonTranslate = new JSONObject("{"
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
//<-----------------------------------------------<
}
