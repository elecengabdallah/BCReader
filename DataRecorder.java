import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;


public class DataRecorder {
	
	GregorianCalendar date;
	
	String userName;
	String dataBaseURL = "jdbc:mysql://127.0.0.1:3306";
	String password;
	BCReaderGUI gui;
	Connection conn;
	ResultSet rs;
	Statement stm;
	
	ArrayList<Product> productAL = new ArrayList<Product>();
	String currentCode = null;
	String lastGoodCode = null;
	int badCode = 0;
	int goodCode = 0;
	
	/***
	 * construct and initialize database 
	 * need to create new table every day and run timer every 24hrs to create new table 
	 * and a timer for every bad code detected to help in correction 
	 * @param userName
	 * @param password
	 * @param gui
	 */
	public DataRecorder(String userName, String password, BCReaderGUI gui) {
		
		this.userName = userName;
		this.password = password;
		this.gui = gui;
		
		date = new GregorianCalendar();
		
		try {
			conn = DriverManager.getConnection(dataBaseURL, this.userName, this.password);
			stm = conn.createStatement();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		try {
			stm.execute("USE Cognex");
		} catch (SQLException e) {
			gui.append("Creating new database...");
			try {
				stm.execute("CREATE DATABASE Cognex");
				
			} catch (SQLException e1) {
				gui.append("Error creating dataBase!!!");
			}
		}
		
		try {
			stm.execute("USE Cognex");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			System.out.println(e1.getMessage());
		}
		
		try {
			if(stm.execute("CHECK TABLE data1")) {
				stm.execute("CREATE TABLE data1 (productCode varchar(20), totalValid int)");
			};
		}catch (Exception e){
			gui.append("Error creating table in MYSQL");
		}
		
		
		
		
//		create monitoring timers
//		timer for every day 
//		timer to differentiate between codes
	}
	
	/***
	 * analyse data if the string has new code updates the total and correct the last bad one 
	 * @param string
	 */
	public void analyseData(String string) {
		
		StringTokenizer st = new StringTokenizer(string);
		int count = st.countTokens();
		
		if(count == 3) {
			
			String firstToken = st.nextToken();
			
			if(lastGoodCode.equals(null) && currentCode.equals(null)) {
				currentCode = firstToken;
				lastGoodCode = currentCode;
				goodCode++;
				
				if(badCode > 0) {
					goodCode = goodCode + badCode;
					badCode = 0;
				}
			}
			
			else if(lastGoodCode.equals(firstToken)) {
				currentCode = firstToken;
				if(badCode > 0) {
					goodCode = goodCode + badCode + 1;
					badCode = 0;
				}
				else {
					goodCode++;
				}
			}
			
			else if(!(lastGoodCode.equals(firstToken))){
				currentCode = firstToken;
				lastGoodCode = currentCode;
				if(badCode > 0) {
					goodCode = badCode + 1;
					badCode = 0;
				}
				else 
					goodCode = 1;
			};
			
			recordDataInMySQL(currentCode, goodCode);
		}
		
		else if(count == 2) {
			badCode++;
		}
		
		gui.updateTotalPanel(currentCode, goodCode, badCode);
	}
	
	/***
	 * recordata into database...check if the code is exist then update value else insert new code
	 * @param code
	 * @param total
	 */
	private void recordDataInMySQL(String code, int total) {
		
		try {
			rs = stm.executeQuery("SELECT productCode FROM data1 WHERE productCode = " + code);
			if(rs.first()) {
				stm.execute("UPDATE DATA1 SET totalValid = " + String.valueOf(total) + "WHERE productCode = " + code);
			}
			else 
				stm.execute("INSERT INTO data1 (productCode, totalValid) VALUES (" + code + " ," + String.valueOf(total) + ")" );
		} catch (Exception e) {
			gui.append("Can't write to database!!!");
		}
		
	}

}
