package gui;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.swing.JOptionPane;



public class Client implements MessageListener{

	private String name;
	private String password;
	private DefaultChatRoom defaultChatRoom;
	
	private Connection connection;
	private Session session;
	private Destination destination;
	private MessageConsumer consumer;
	private MessageConsumer broadcastConsumer;
	private MessageProducer producer;
	private ChatRoomContainer chatRoomContainer;
	
	public Client(){
		Chatwindow.clientContainer.addClient(this);
	}
	
	public Client(String name, String password){
		this.name = name;
		this.password = password;
		
		
		Chatwindow.clientContainer.addClient(this);
		chatRoomContainer = new ChatRoomContainer();
		
		try{
			this.connection = (Connection) Server.connectionFactory.createConnection();
		
			connection.start();
		
			this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
			this.destination = session.createQueue(name);/////////////
			
			Destination broadcastDestination = session.createTopic("publicBroadcast");/////////////
		
			consumer = session.createConsumer(destination);
			consumer.setMessageListener(this);
			
			broadcastConsumer=session.createConsumer(broadcastDestination);
			broadcastConsumer.setMessageListener(this);
			
			producer = session.createProducer(session.createQueue("server.messages"));
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			this.defaultChatRoom = new DefaultChatRoom(Chatwindow.server.getChatRoom(), this);
		}
		catch (JMSException q){
			q.printStackTrace();
		}
	}

	public String getName() {
		return this.name;
	}

	public String getPassword() {
		return this.password;
	}
	
	

	public void onMessage(Message arg0) {
		// TODO Auto-generated method stub
		this.defaultChatRoom.onMessage(arg0);
		
	}
	
	public boolean promptPrivate(Client c){
		String str = "Would you like to join " + c.getName() + "'s private Chat Room?";
		int choice = JOptionPane.showConfirmDialog(null, str, this.getName() + " PrivateMessage?", JOptionPane.YES_NO_OPTION);
		
		if(choice == 0)
			return true;
		
		else
			return false;
		
	}
	
	
	public Session getSession(){
		return this.session;
	}
	
	public void setSession(Session sesh){
		this.session = sesh;
	}
	
	public Connection getConnection(){
		return this.connection;
	}
	
	public void setConnection(Connection connect){
		this.connection = connect;
	}
	
	public Destination getDestination(){
		return this.destination;
	}
	
	public void setDestination(Topic dest){
		this.destination = dest;
	}
	
	public MessageConsumer getConsumer(){
		return this.consumer;
	}
	
	public void setConsumer(MessageConsumer cons){
		this.consumer = cons;
	}
	
	public MessageProducer getProducer(){
		return this.producer;
	}
	
	public void setProducer(Destination destination) {
		try {
			this.producer = session.createProducer(destination);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void setProducer(MessageProducer produce){
		this.producer = produce;
	}
	
	public void logoff(){
		try {
			for(int i = 0; i < chatRoomContainer.size(); i++){
				chatRoomContainer.getChatRoom(i).dispose();
			}
			this.defaultChatRoom.dispose();
			this.session.close();
			this.connection.close();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ChatRoomContainer getChatRoomContainer() {
		// TODO Auto-generated method stub
		return this.chatRoomContainer;
	}

	public void addChatRoom(ChatRoom temp) {
		// TODO Auto-generated method stub
		this.chatRoomContainer.addChatRoom(temp);
		String t = "User " + this.getName() + " has joined the ChatRoom.\n";
		temp.sendMessage(t);
	}

	public String joinRoom(Client client, ChatRoom room) {
		// TODO Auto-generated method stub
		String text = "User " + client.getName() + " would like you to join ChatRoom " + room.getName();
		
		if(chatRoomContainer.contains(room.getName())){
			return "User is already in this ChatRoom!\n";
		}
		
			int i = JOptionPane.showConfirmDialog(null, text);
			if (i ==0){
			ChatRoom t = new ChatRoom(this, room, room.getClientContainer());
			addChatRoom(t);
			return "";
		}
		return "User did not wish to join the ChatRoom.\n";
	}
}

