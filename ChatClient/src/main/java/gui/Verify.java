package gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;


public class Verify {

	//Check to see if the string has illegal characters
		public static boolean containsIllegals(String toExamine) {
			
			//Create a set of illegal chars and check for a match
		    Pattern pattern = Pattern.compile("[~#@*+%{}<>\\[\\]|\"\\^\\(\\)!?\\ ]");
		    Matcher matcher = pattern.matcher(toExamine);
		    return matcher.find();
		}
		
		
		//Check to see if the username is already taken
		public static boolean alreadyTaken(String username, int PassCheck, String password) throws FileNotFoundException{

			Scanner input = new Scanner(new File("src/main/java/testFile.txt"));
			int x;
			String name;
			
			//Check while the file has a next line
			while(input.hasNextLine()){
				String blah = input.nextLine();
				x = blah.indexOf('/');
				name = blah.substring(0, x);
				
				if(name.equalsIgnoreCase(username)){
					if(PassCheck == 1){
						name = blah.substring(x+1);
						if(name.equals(password)){
							input.close();
							return true;
						}
					}
					input.close();
					return true;
				}
			}
			input.close();
			return false;
			
		}
		
		public static boolean acceptableLength(String username, String password){
			//Check if the username and password are of acceptable length
			if(username.length() > 16 || 
					password.length() > 16){
				JOptionPane.showMessageDialog(null, "Please enter a username of up to 16 characters");
				return false;
			}
			return true;
		}
		
		public static boolean verifyCombination(String username, String password) throws FileNotFoundException{
			Scanner input = new Scanner(new File("src/main/java/testFile.txt"));
			int x;
			String name;
			
			//Check while the file has a next line
			while(input.hasNextLine()){
				String blah = input.nextLine();
				x = blah.indexOf('/');
				name = blah.substring(0, x);
				
				if(name.equalsIgnoreCase(username)){
					String pass = blah.substring(x+1);
					if(pass.equalsIgnoreCase(password)){
						input.close();
						return true;
					}
					System.out.println("Username/password combination is invalid");
					System.out.println("Please try again");
				}
			}
			input.close();
			return false;
		}
		
}
