package gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;


public class DefaultChatRoom extends ChatRoom  implements MessageListener  {

	/**
	 * 
	 */

	JButton logoff;
	JLabel lblChatRooms;
	JList<String> ChatRoomList;
	JButton btnJoin;

	
	public DefaultChatRoom(){
		super();
	}
	

	public DefaultChatRoom(DefaultChatRoom chatRoom, Client client) {
		// TODO Auto-generated constructor stub
		this(client, "Default");
		String t = "User " + client.getName() + " has LoggedOn.\n";	
		sendMessage(t);
	}
	
	public DefaultChatRoom(Client client, String name1) {

		super(client, name1);
		this.clientContainer = Chatwindow.clientContainer;
		frame.setBounds(100, 100, 800, 350);
		frame.setDefaultCloseOperation(frame.DO_NOTHING_ON_CLOSE);
		frame.removeWindowListener(frame.getWindowListeners()[0]);
		
		WindowListener exitListen = new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				int i = JOptionPane.showConfirmDialog(null, "Exiting will terminate the program, are you sure you wish to exit?");
				if(i == 0){
					if(Chatwindow.clientContainer.removeClient(currentUser)){
						currentUser.logoff();
					}
					else
						System.out.println("Error logging off");
				}
				else
					return;
			}
		};
		
		logoff = new JButton("LogOff");	
		frame.addWindowListener(exitListen);
		
		logoff.setBounds(685, 1, 89, 23);
		LogOff l = new LogOff();
		logoff.addActionListener(l);
		contentPane.add(logoff);
		
		lblChatRooms = new JLabel("Chat Rooms");
		lblChatRooms.setBounds(558, 5, 72, 14);
		contentPane.add(lblChatRooms);
		
		UserList = new JList<String>(clientContainer.getClientNames());
		UserList.setBackground(Color.GREEN);
		UserList.setValueIsAdjusting(true);
		UserList.setBounds(331, 21, 89, 190);
		contentPane.add(UserList);
		
		ChatRoomList = new JList<String>(Chatwindow.server.getChatRoomContainer().getChatRoomNames());
		ChatRoomList.setValueIsAdjusting(true);
		ChatRoomList.setBackground(Color.MAGENTA);
		ChatRoomList.setBounds(547, 21, 94, 190);
		contentPane.add(ChatRoomList);
		
		btnJoin = new JButton("Join");
		btnJoin.setToolTipText("Join selected ChatRoom.");
		btnJoin.setBounds(651, 188, 89, 23);
		JoinChatRoom jcr = new JoinChatRoom();
		btnJoin.addActionListener(jcr);
		
//		btnInvite.removeActionListener(btnInvite.getActionListeners()[0]);
//		InviteUser inv = new InviteUser();
//		btnInvite.addActionListener(inv);
		
		
		JButton btnCreateRoom = new JButton("Create ChatRoom");
		btnCreateRoom.setToolTipText("Create a new ChatRoom");
		btnCreateRoom.setBounds(651, 100, 89, 23);
		
		this.SendButton.removeActionListener(SendButton.getActionListeners()[0]);
		SendButtonAction s = new SendButtonAction();
		SendButton.addActionListener(s);
		
		CreateChatRoom ccr = new CreateChatRoom();
		btnCreateRoom.addActionListener(ccr);
		contentPane.add(btnCreateRoom);
		contentPane.add(btnJoin);
	}

	public class CreateChatRoom implements ActionListener{
		
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			String room = JOptionPane.showInputDialog("Please enter the name of the ChatRoom");
			
			if(Chatwindow.server.getChatRoomContainer().contains(room)){
				String t = "A ChatRoom with that name already exists; please use another name.\n";
				TextMessage m;
				try {
					m = currentUser.getSession().createTextMessage(t);
					m.setJMSReplyTo(currentUser.getDestination());
					currentUser.getProducer().send(Chatwindow.server.getServerTopic(), m);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			else if(room != null){
				ClientContainer cc = new ClientContainer();
				ChatRoom temp = new ChatRoom(currentUser, room, cc);
				Chatwindow.server.addChatRoom(temp);
				currentUser.addChatRoom(temp);
			}	
		}	
	}
	
	
	public class LogOff implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
//			if(Chatwindow.clientContainer.removeClient(currentUser)){
//				currentUser.logoff();
//			}
//
//			else
//				System.out.println("Error logging off");
			currentUser.setProducer(Chatwindow.server.getServerTopic());
			sendMessageSever("LOGOFF",currentUser.getName() + " has logged off");
			dispose();
		}
	}
	
	public class JoinChatRoom implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			try{
				int index = ChatRoomList.getSelectedIndex();
				
				if (index < 0){
					JOptionPane.showMessageDialog(null, "You must select a ChatRoom to join");
				}
				else{
					ChatRoom c = Chatwindow.server.getChatRoomContainer().getChatRoom(index);
					if(currentUser.getChatRoomContainer().contains(c.getName())){
						c.sendMessage("You are already in this ChatRoom!\n",  currentUser.getDestination());
						return;
					}
					currentUser.addChatRoom(new ChatRoom(currentUser, c, c.getClientContainer()));
				}
			}
			catch(Exception e1){
				e1.printStackTrace();
			}
		}
	}
	public class SendButtonAction implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			sendMessage();
		}
		
		
	}
	
	public class InviteUser implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			int[] indices = UserList.getSelectedIndices();
			if(indices.length <= 0){
				JOptionPane.showMessageDialog(null, "You must select at least one user to invite.");
			}
			else if (indices.length == 1 && clientContainer.getClient(indices[0]).getName().equals(currentUser.getName())){
				sendMessage("Invite a user other than yourself!\n", currentUser.getDestination());
			}
			else{
				String room = JOptionPane.showInputDialog(null, "Which ChatRoom would you like to invite users to?");
				if(Chatwindow.server.getChatRoomContainer().getChatRoomNames().contains(room)){
					Client c;
					ChatRoom temp = Chatwindow.server.getChatRoomContainer().getChatRoom(room);
					for(int i = 0; i < indices.length; i++){
						c = Chatwindow.clientContainer.getClient(indices[i]);
						
						if(!currentUser.getName().equals(c.getName())){
							String t = c.joinRoom(currentUser, temp);
							if(!t.equals("")){
								JOptionPane.showMessageDialog(null, t);
							}
						}
					}
				}
				else{
					JOptionPane.showMessageDialog(null, "Please enter an existing ChatRoom");
				}
			}
		}
	}
	public void dispose(){
		String t = "User " + currentUser.getName() + " has disconnected.\n";
		sendMessage(t);
		this.frame.dispose();
	}
	
	public boolean sendMessage(){
		try {
			String text =InputBox.getText();
			

					
					MessageProducer p = currentUser.getProducer();
					TextMessage message = currentUser.getSession().createTextMessage(text);////
					message.setJMSCorrelationID(currentUser.getName());
					message.setJMSReplyTo(currentUser.getDestination());
					p.send(message);
					text = text + "\n";
					InputBox.setText("");
					OutputBox.append(text);

		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean sendMessage(String t){	
		try{
				MessageProducer p = currentUser.getProducer();
				TextMessage message = currentUser.getSession().createTextMessage(t);
				message.setJMSReplyTo(currentUser.getDestination());
				p.send(message);//Chatwindow.server.getServerTopic(),
			return true;
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
	}
	
	
	public boolean sendMessageSever(String JMScorrelationID,String t){	
		try{
				MessageProducer p = currentUser.getProducer();
				TextMessage message = currentUser.getSession().createTextMessage(t);
				message.setJMSReplyTo(currentUser.getDestination());
				message.setJMSCorrelationID(JMScorrelationID);
				p.send(Chatwindow.server.getServerTopic(),message);//Chatwindow.server.getServerTopic(),
			return true;
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
	}
	
	
	public boolean sendMessage(String JMScorrelationID, String text){
		try {	
			MessageProducer p = currentUser.getProducer();
			TextMessage message = currentUser.getSession().createTextMessage(text);
			message.setJMSCorrelationID(JMScorrelationID);
			//message.setJMSReplyTo(getDestination());
			p.send(message);
			return true;
			
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
}