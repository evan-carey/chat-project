package gui;

import java.awt.BorderLayout;

import javax.swing.JButton;


public class MainMenu {
	
	public MainMenu(){
		enterInforGUI enter = new enterInforGUI();
		JButton forgotPassword = new JButton("Forgot Password");
		JButton createAccount = new JButton("Create Account");
		JButton changePassword = new JButton("Change password");
		
		createAccount s = new createAccount();
		createAccount.addMouseListener(s);
		
		changePassword change = new changePassword();
		changePassword.addMouseListener(change);
		
		enter.getFrame().add(forgotPassword, BorderLayout.CENTER);
		enter.getFrame().add(createAccount, BorderLayout.SOUTH);
		enter.getFrame().add(changePassword, BorderLayout.EAST);
		enter.getFrame().pack();
	}

}
