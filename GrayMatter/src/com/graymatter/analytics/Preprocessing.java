package com.graymatter.analytics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class Preprocessing {

	/*
	 * public void stem() { EnglishStemmer english = new EnglishStemmer();
	 * String[] test = { "windows", "Windows", "Server", "Windows Server" };
	 * String[] gold = { "bank", "bank", "bank", "banker", "bank", "banker" };
	 * for (int i = 0; i < test.length; i++) { english.setCurrent(test[i]);
	 * english.stem(); System.out.println("English: " + english.getCurrent()); }
	 * }
	 */

	/**
	 * This function removes stop words, special characters and extra white
	 * spaces from a summary.
	 * 
	 * @param documents
	 * @return
	 */
	public List<String> removeStopWords(List<String> documents) {

		for (int i = 0; i < documents.size(); i++) {
			String doc = " " + documents.get(i).toLowerCase().trim() + " ";
			for (String stopWord : Constants.ENGLISH_STOPWORDS)
				doc = doc.replaceAll(" " + stopWord.trim().toLowerCase() + " ",
						" ");
			for (String specialChar : Constants.SPECIAL_CHARACTERS)
				doc = doc.replaceAll(Pattern.quote(specialChar.toLowerCase()),
						"");

			// Remove extra white spaces.
			boolean index = true;
			while (index) {
				doc = doc.replaceAll("  ", " ");
				if (!doc.contains("  "))
					index = false;
			}
			documents.set(i, doc.trim());
		}

		return documents;
	}

	/**
	 * This function takes a Map as input. It removes stop words, special
	 * characters and extra white spaces for each key and then stores this
	 * pre-processed key as its corresponding value.
	 * 
	 * @param documents
	 * @return
	 */
	public Map<String, String> removeStopWords(Map<String, String> documents) {

		for (Map.Entry<String, String> entrySet : documents.entrySet()) {
			String doc = new String(entrySet.getKey().toLowerCase());
			doc = " " + doc + " ";
			for (String escapeChar : Constants.ESCAPE_CHARACTERS)
				doc = doc.replaceAll(Pattern.quote(escapeChar),
						"");
			for (String specialChar : Constants.SPECIAL_CHARACTERS)
				doc = doc.replaceAll(Pattern.quote(specialChar.toLowerCase()),
						"");
			for (String stopWord : Constants.ENGLISH_STOPWORDS)
				doc = doc.replaceAll(" " + stopWord.trim().toLowerCase() + " ",
						" ");
			
			// Remove extra white spaces.
			boolean index = true;
			while (index) {
				doc = doc.replaceAll("  ", " ");
				if (!doc.contains("  "))
					index = false;
			}
			// Add this processed document as value of current summary.
			entrySet.setValue(doc);
		}
		return documents;
	}

	public void convertExcelToCSV(String excelODBCDatasourceName,
			String sheetName, String csvFilePath) {
		String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
		String url = "jdbc:odbc:" + excelODBCDatasourceName;
		String username = "yourName";
		String password = "yourPass";
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		BufferedWriter writer = null;
		try {
			File csvFile = new File(csvFilePath);
			csvFile.createNewFile();
			writer = new BufferedWriter(new FileWriter(csvFile));

			Class.forName(driver);
			con = DriverManager.getConnection(url, username, password);
			st = con.createStatement();
			String query = "Select * from [" + sheetName + "$]";

			rs = st.executeQuery(query);
			int colCount = rs.getMetaData().getColumnCount();
			while (rs.next()) {
				StringBuilder line = new StringBuilder();
				for (int i = 1; i <= colCount; i++) {
					String colVal = rs.getString(i);
					if (colVal == null)
						line.append(",");
					else
						line.append(colVal.trim().replaceAll(",", "")).append(
								",");
				}
				line.deleteCharAt(line.length() - 1);
				writer.write(line.toString());
				writer.newLine();
				writer.flush();
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				st.close();
				con.close();
				writer.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String a[]) {

		new Preprocessing().convertExcelToCSV("excel_ds", "Sheet1",
				"2.csv");

	}
}
