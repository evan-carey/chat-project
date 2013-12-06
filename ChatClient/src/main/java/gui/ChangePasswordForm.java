package gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;


public class ChangePasswordForm {
	JFrame frame;
	JTextField username;
	JTextField password;
	JTextField newPass;
	JTextField newPass2;
	
	public ChangePasswordForm(){
		frame = new JFrame();
		frame.setSize(300, 500);
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		frame.setLayout(new GridLayout(5,2));
		
		JLabel label1 = new JLabel("Enter Username");
		JLabel label2 = new JLabel("Enter Password");
		JLabel label3 = new JLabel("Enter New Password");
		JLabel label4 = new JLabel("ReEnter New Password");
	
	
		this.username = new JTextField(16);
		this.password = new JTextField(16);
		this.newPass = new JTextField(16);
		this.newPass2 = new JTextField(16);
		
	
		JButton enter = new JButton("Enter");
		
		changePass ce = new changePass();
		enter.addActionListener(ce);
		
		frame.add(label1);
		frame.add(username);
		frame.add(label2);
		frame.add(password);
		frame.add(label3);
		frame.add(newPass);
		frame.add(label4);
		frame.add(newPass2);
		frame.add(enter);
		
		frame.pack();
		
		frame.setVisible(true);
	}

	private class changePass implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			String user = username.getText();
			String pass = password.getText();
			String newer = newPass.getText();
			String newer2 = newPass.getText();
			
			try{
			if(newer.equals(newer2) == false){
				System.out.println("Please enter and reenter your password identically");
			}
			
			else if(Verify.alreadyTaken(user, 0, null) == false){
				System.out.println("This username has not yet been registered.");
				System.out.println("Go to the 'Create Account' option to create a new account");
			}
			
			else if(!(Verify.alreadyTaken(user, 1, pass))){
				System.out.println("The username and password combination you have entered is incorrect");
			}
			else {
					if(!editUser(user, pass, newer)){
						System.out.println("Error changing password");
					}
					else
						System.out.println("Password change was a success!");
			}
			
			}
			catch (Exception q){
				q.printStackTrace();
			}
			
			
		}
		
	}
	
	private boolean editUser(String username, String password, String newPassword){

		try{
			
			File file = new File("src\\testFile.txt");
			
			
			String temp;
			StringBuffer strBuff = new StringBuffer();
			Scanner input = new Scanner(file);
			int i = 0;
			
			while(input.hasNextLine() && (temp = input.nextLine()) != null){
				strBuff.append(temp).append("\r\n");
			}
					temp = username + '/' + password;
					int start = strBuff.indexOf(temp);
					if(start == -1)
						return false;
					int end = start + temp.length();
					temp = username + '/' + newPassword;
					
					strBuff.replace(start, end, temp);
					
						BufferedWriter bw = new BufferedWriter(new FileWriter(file));
						bw.write(strBuff.toString());
						
						bw.close();

			}
			catch(IOException e1){
				e1.printStackTrace();
				return false;
			}
		return true;
	}
	
	
}
