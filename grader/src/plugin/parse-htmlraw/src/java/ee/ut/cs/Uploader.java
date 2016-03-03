package ee.ut.cs;

import java.sql.*;
import org.json.JSONObject;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.lang.String;
import java.lang.Error;


public class Uploader {

	private static String host = "jdbc:mysql://localhost:3306/mydb";
	private static String user = "root";
	private static String pass = "toor";

	public Boolean postGradesAccess(JSONObject json, String domain, String url) {
		//If uploaded successfully, return true, otherwise false
		
		String fields = "", values = "";			
		
		//Iterate over elements in json keyset
		for (String el : json.keySet()) {
			fields += "`" + el + "`, ";

			//json.get(el) retrieves for example ["PASS"]
			//Brackets and quotation marks are removed, and the value itself is added to SQL with upper commas
			values += "'" + json.get(el).toString().substring(2, json.get(el).toString().length() - 2) + "', ";
		}
		
		String extraFields = "`Time`, `Domain`, `Url`";	
		
		//Date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
		String extraValues = "'" + sdf.format(new Date()).toString() + "'";
		
		//Add page address to sql
		extraValues += ", '" + domain + "', '" + url + "'";
		
		//Combine sql
		String sql = "INSERT INTO `access_lint` (" + fields + extraFields + ") VALUES (" + values + extraValues + ");";
		//System.out.println(sql);
		
		//Try creating the connection and uploading the values
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			
			query.execute(sql);
			//Return true if upload didn't throw errors
			return true;
		} catch (SQLException e) {
			System.out.println(e);
		    System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		    //If error is thrown, report that upload failed
			return false;
		}
		
		
		
	}
	public Boolean postGradesCodeSniffer(String[] outputs, String domain, String url) {
		//Returns true on successful upload, otherwise false
		
		String outputErr = outputs[0];
		String outputWarn = outputs[1];
		String values = "";
		
		//If not null, add to sql
		if (outputErr != null) {
			//System.out.println(outputErr);
			values += outputErr;
		}
		
		//If both are not null, add a comma between them
		if (outputErr != null && outputWarn != null)
			values += ", ";

		//If not null, add to sql
		if (outputWarn != null) {
			//System.out.println(outputWarn);
			values += outputWarn;
		}
		
		//Start generating the sql
		
		//Date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
		String time = "'" + sdf.format(new Date()).toString() + "', ";
		
		//Combine everything
		String sql = "INSERT INTO `html_codesniffer` (`domain`, `url`, `time`, `errors`, `notices`) VALUES " + "('" + domain + "', '" + url + "', " + time + values + ");";
		//System.out.println(sql);
		
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			query.execute(sql);
			//If upload successful, return true
			return true;
		} catch (SQLException e) {
			System.out.println(e);
		    System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		    //If error is thrown, return that upload failed
			return false;
		}
	}
	
}
