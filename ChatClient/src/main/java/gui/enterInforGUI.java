package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class enterInforGUI {
	
	private JFrame frame;
	private JTextField userName;
	private JTextField passWord;

public enterInforGUI(){
	frame = new JFrame();
	frame.setForeground(Color.WHITE);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setLocation(new Point(10,50));
	frame.setSize(new Dimension(300,120));
	frame.setTitle("ChatClient");
	frame.setVisible(true);
	frame.setLayout(new BorderLayout());
	
	JPanel north = new JPanel(new GridLayout(3,2));
	north.add(new JLabel("Enter username: "));
	
	userName = new JTextField(16);
	north.add(userName);
	
	passWord = new JTextField(16);
	
	SignIn s = new SignIn();
	
	north.add(new JLabel("Enter password: "));
	north.add(passWord);
	
	
	JButton enter = new JButton("Enter");
	enter.addActionListener(s);
	
	north.add(enter);
	frame.add(north, BorderLayout.NORTH);
	
	
//	frame.pack();
}

	public JFrame getFrame(){
		return this.frame;
	}
	
	public class SignIn implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
			if(testInfo()){
				System.out.println("Log in successful, Welcome!");
				Client client = new Client(userName.getText(), passWord.getText());
				try {
				TextMessage textmessage= client.getSession().createTextMessage();
				textmessage.setText(" ");
				textmessage.setJMSCorrelationID(userName.getText());
				textmessage.setJMSReplyTo(client.getSession().createQueue(userName.getText()));
				System.out.println(userName.getText());
				client.getProducer().send(Chatwindow.server.getServerTopic(), textmessage);
				} catch (JMSException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		private boolean testInfo(){
			String username = userName.getText();
			String password = passWord.getText();
			
			if(!Verify.acceptableLength(username, password)){
				String str = "Username/Password must be between 5 and 16 characters";
				JOptionPane.showMessageDialog(null, str);
				return false;
			}
			
			else if (Verify.containsIllegals(username) || Verify.containsIllegals(password)){
				String str = "Username/Password contains Illegal Characters" +
						"Please use only letters, numbers, and/or underscores";
			JOptionPane.showMessageDialog(null, str);
			return false;
			
			} else
				try {
					if (!Verify.verifyCombination(username, password)){
						System.out.println("Log in unsuccessful, please check username/password combination");
						return false;
					}
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return false;
				}
			return true;
		}
	}
}
