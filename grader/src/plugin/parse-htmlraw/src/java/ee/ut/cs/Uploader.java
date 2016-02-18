package ee.ut.cs;
import java.sql.*;
import org.json.JSONObject;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.lang.String;
public class Uploader {

	private static String host = "jdbc:mysql://localhost:3306/mydb";
	private static String user = "root";
	private static String pass = "toor";

	public Boolean postGradesAccess(JSONObject json, String domain, String url) {
		//If uploaded successfully, return 1, otherwise 0
		
		//Try creating the connection and uploading the values
		//>----------------------------------------------->
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			
			String fields = "", values = "";			
		
			for (String el : json.keySet()) {
				fields += "`" + el + "`, ";
				values += "'" + json.get(el).toString().substring(2, json.get(el).toString().length() - 2) + "', ";
			}
			
			String extraFields = "`Time`, `Domain`, `Url`";	
			
			//String extraValues = ", '" + new Date(System.currentTimeMillis()).toString() + "'";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
			String extraValues = "'" + sdf.format(new Date()).toString() + "'";
			extraValues += ", '" + domain + "', '" + url + "'";
			
			String sql = "INSERT INTO `access_lint` (" + fields + extraFields + ") VALUES (" + values + extraValues + ");";
			System.out.println(sql);
			query.execute(sql);

		} catch (SQLException e) {
			System.out.println(e);
		    System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
			return false;
		}
		
		//<-----------------------------------------------<
		
		
		
		return true;
	}
	public Boolean postGradesCodeSniffer(ArrayList<JSONObject> array, String domain, String url) {
		
		//JSONObject keys
		//[selector, message, context, typeCode, code, type]
		int i = 0, j = 0;
		//To understand how many different elements there are
		HashSet<String> uniqueErr = new HashSet<String>(), uniqueWarn = new HashSet<String>();
		for (JSONObject obj : array) {
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

		System.out.println("Element count in answer set: " + array.size());
		System.out.println("Errors : " + i);
		System.out.println("Of those, unique count: " + uniqueErr.size());
		Iterator it = uniqueErr.iterator();
		System.out.println(" and they are: ");
		while (it.hasNext()) {
			String[] str = it.next().toString().split("\\.");
			String out = str[1].substring(str[1].length() - 1) + "_" + str[2].substring(str[2].length() - 3);
			//TO-DO split -> replace
			//out = out.split("_").join(".");
			System.out.println(str[0] + " - " + out);
		}
		System.out.println();
		System.out.println("Warnings : " + j);
		System.out.println("Of those, unique count: " + uniqueWarn.size());
		it = uniqueWarn.iterator();
		System.out.println(" and they are: ");
		while (it.hasNext()) {
			String[] str = it.next().toString().split("\\.");
			String out = str[1].substring(str[1].length() - 1) + "_" + str[2].substring(str[2].length() - 3);
			//TO-DO split -> replace
			//out = out.split("_").join(".");
			System.out.println(str[0] + " - " + out);
		}
		
		return false;
	}
}
