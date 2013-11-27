
package edu.ucsd.cse110.server.accountinfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Accounts {

	private static final String USER_FILE = "src/main/java/edu/ucsd/cse110/server/accounts.txt";
	private HashMap<String, String> accounts;
	
	public Accounts() {
		accounts = new HashMap<String, String>();
		loadAccounts(USER_FILE);
	}
	/**
	 * Load accounts from file into the database.
	 * @param file The .txt file that stores usernames and passwords
	 */
	private void loadAccounts(String file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] account = line.split(" ");
				accounts.put(account[0], account[1]);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Change the password for an account.
	 * @param username The username for the account
	 * @param oldPassword The old password for the account
	 * @param newPassword The new password to be set
	 * @throws AccountException if oldPassword doesn't match username
	 */
	public void setPassword(String username, String oldPassword, String newPassword) throws AccountException {
		String password = getPassword(username);
		if (!oldPassword.equals(password)) 
			throw new AccountException("Password does not match this account");
		accounts.put(username, newPassword);
	}
	
	/**
	 * Get the password for an account.
	 * @param username The username for the account
	 * @return the password for the account
	 * @throws AccountException if username is not in database
	 */
	public String getPassword(String username) throws AccountException {
		String password = accounts.get(username);
		if (password == null) 
			throw new AccountException("Account does not exist");
		return password;
	}
	
	/**
	 * Add a new account to the database.
	 * @param username The username for the account to be added
	 * @param password The password for the account to be added
	 * @throws AccountException if username is already in database
	 */
	public void addAccount(String username, String password) throws AccountException {
		if (accounts.containsKey(username))
			throw new AccountException("Username unavailable");
		accounts.put(username, password);
	}
	
	/**
	 * Check accounts for a given username.
	 * @param username The given username to be checked
	 * @return True if accounts contains username, false otherwise
	 */
	public boolean hasUsername(String username) {
		return accounts.containsKey(username);
	}
	
	/**
	 * Write contents of accounts hashmap to the accounts.txt file.
	 * This method is called when the server application terminates.
	 */
	public void writeToFile() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(USER_FILE));
			Iterator<Entry<String, String>> it = accounts.entrySet().iterator();
			
			while (it.hasNext()) {
				Map.Entry<String, String> account = it.next();
				out.write(account.getKey() + " " + account.getValue() + "\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
//>>>>>>> branch 'master' of https://flemagut@bitbucket.org/evan_carey/cse110_project.git
