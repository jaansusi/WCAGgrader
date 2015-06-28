package ee.ut.cs;
import java.sql.*;
import org.json.JSONObject;
import java.util.Calendar;

public class Uploader {
	public Boolean postGrades(JSONObject json, String domain, String url) {
		//If uploaded successfully, return 1, otherwise 0
		String host = "jdbc:mysql://localhost:3306/mydb";
		String user = "root";
		String pass = "toor";
		
		
		
		//Try creating the connection and uploading the values
		//>----------------------------------------------->
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			
			String fields = "", values = "";			
			/*
			 * TO-DO
			 * Uncomment next section so elements from json are added
			 * once json functions have been fixed
			 */
			
			for (String el : json.keySet()) {
				fields += "`" + el + "`, ";
				values += "'" + json.get(el).toString().substring(2, json.get(el).toString().length() - 2) + "', ";
			}
			
			// ------------------
			// TO-DO once json is fixed, switch out these values since a comma is needed when there
			// are values before it
			
			String extraFields = "`Time`, `Domain`, `Url`";	
			
			//String extraValues = ", '" + new Date(System.currentTimeMillis()).toString() + "'";
			String extraValues = "'" + new Date(System.currentTimeMillis()).toString()
									//+ ", " + Calendar.getInstance().getTime()
									+ "'";
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
}
