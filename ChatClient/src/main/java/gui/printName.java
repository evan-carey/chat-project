package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;


public class printName implements ActionListener {

	public void actionPerformed(ActionEvent event, JTextField field){
		
		System.out.println(field.getName());
	}
	
	public void actionPerformed(ActionEvent e) {
		System.out.println("something random");
		
	}
}
