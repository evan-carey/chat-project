package gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;


public class createAccountForm {
	JFrame frame;
	JTextField username;
	JTextField password;
	JTextField reEnter;
	
	public createAccountForm(){
		frame = new JFrame();
		frame.setSize(300, 500);
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		frame.setLayout(new GridLayout(4,2));
		
		JLabel label1 = new JLabel("Enter Username");
		JLabel label2 = new JLabel("Enter Password");
		JLabel label3 = new JLabel("ReEnter Password");
	
	
	this.username = new JTextField(16);
	this.password = new JTextField(16);
	this.reEnter = new JTextField(16);
	
	JButton enter = new JButton("Enter");
	
	createEnter ce = new createEnter();
	enter.addActionListener(ce);
	
	frame.add(label1);
	frame.add(username);
	frame.add(label2);
	frame.add(password);
	frame.add(label3);
	frame.add(reEnter);
	frame.add(enter);
	
	frame.pack();
	
	frame.setVisible(true);
	
}

private class createEnter implements ActionListener{
	@Override
	public void actionPerformed(ActionEvent e) {
		String user = username.getText();
		String pass = password.getText();
		String reE = reEnter.getText();
		
		if(!(reE.equals(pass))){
			System.out.println("Passwords do not match, please try again");
		}
		
		else if(passesTests(user, pass, 0)){
			
			if(!addUser(user, pass))
				System.out.println("Error adding name");
			else{
				JOptionPane.showMessageDialog(null, "Account created, you can now login!");
				frame.dispose();
			}
				
		}
	}
	
}


public boolean passesTests(String username, String password, int priority){
	
	//Check if username or password has illegal chars
	if(Verify.containsIllegals(username) || Verify.containsIllegals(password)){
		String str = "Username/Password contains Illegal Characters" +
				"Please use only letters, numbers, and/or underscores";
	JOptionPane.showMessageDialog(null, str);
	return false;
	}
	
	//Check if the username and password are of acceptable length
	else if(!Verify.acceptableLength(username, password)){
		JOptionPane.showMessageDialog(null, "Please enter a username of up to 16 characters");
		return false;
	}
	
	else if(priority == 0){
		try{
			//Check if username is already taken
			if(Verify.alreadyTaken(username, 0, null)){
				System.out.println("That username is already taken, please use another");
				return false;
		}
		
	}
	
	catch(Exception e1){
		System.out.println(e1.getMessage());
		return false;
	}
	}
	return true;
}

private boolean addUser(String username, String password){

	try{
		
		File file = new File("src\\testFile.txt");
		if (!file.exists()){
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write(username+ '/' + password+"\n");
			bw.close();
			
			System.out.println(username);
			System.out.println(password);
			return true;
		}
		
		catch(IOException e1){
			e1.printStackTrace();
			return false;
		}
	
	}
}
