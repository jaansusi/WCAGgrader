package ee.ut.cs;
import java.sql.*;
import org.json.JSONObject;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Uploader {
	public Boolean postGrades(JSONObject json, String domain, String url, Integer choice) {
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
			
			if (choice == 1) {
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
			} else if (choice == 2) {


				
			}
			
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
