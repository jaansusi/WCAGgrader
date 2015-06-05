package ee.ut.cs;
import java.sql.*;
import org.json.JSONObject;


public class Uploader {
	public Boolean PostGrades(JSONObject json, String domain, String url) {
		//If uploaded successfully, return 1, otherwise 0
		String host = "jdbc:mysql://localhost:3306/mydb";
		String user = "root";
		String pass = "toor";
		
		
		
		//Try creating the connection and uploading the values
		//>----------------------------------------------->
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			
			String fieldsTemp = "", valuesTemp = "";
			//int i = 0;
			for (String el : json.keySet()) {
				fieldsTemp += "`" + el + "`, ";
				valuesTemp += "'" + json.get(el).toString().substring(2, json.get(el).toString().length() - 2) + "', ";
			}
			String fields = fieldsTemp.substring(0, fieldsTemp.length() - 2);
			String values = valuesTemp.substring(0, valuesTemp.length() - 2);
			//System.out.println(fields);
			//System.out.println(values);
			
			
			String sql = "INSERT INTO `Single` (" + fields + ") VALUES (" + values + ");";
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
