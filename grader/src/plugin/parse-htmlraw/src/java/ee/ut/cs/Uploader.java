package ee.ut.cs;
import java.sql.*;
import org.json.JSONObject;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

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
		for (JSONObject json : array) {
			System.out.println(json.get("code"));
		}
		
		return false;
	}
}
