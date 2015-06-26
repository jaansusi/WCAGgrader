package unifier;


public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String host = "jdbc:mysql://localhost:3306/mydb";
		String user = "root";
		String pass = "toor";
		
		
		
		//Try creating the connection
		//>----------------------------------------------->
		try {
			Connection con = DriverManager.getConnection(host, user, pass);
			Statement query = con.createStatement();
			String select = "SELECT * FROM `Single`";
			query.execute(select);
			//String sql = "INSERT INTO `Single` (" + fields + ") VALUES (" + values + ");";
			//query.execute(sql);
			
		} catch (SQLException e) {
			System.out.println(e);
		    System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
	}

}
