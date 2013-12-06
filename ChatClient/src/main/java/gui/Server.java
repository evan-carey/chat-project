package gui;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;


public class Server  {
	private Connection connection;
	private Session session;
	private boolean transacted = false;
	private MessageProducer replyProducer;
	private String database = "src/testFile.txt";
	public static ActiveMQConnectionFactory connectionFactory=  new ActiveMQConnectionFactory("tcp://localhost:61616");
	private ChatRoomContainer ChatRoomList = new ChatRoomContainer();
	private Destination defaultTopic;
	private DefaultChatRoom mainDefault;
	MessageConsumer myConsumer;
	
	public Server(){
		try {
			
			mainDefault = new DefaultChatRoom();
			
			connection = connectionFactory.createConnection();
			connection.start();
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			this.defaultTopic = session.createQueue("server.messages");
				
		} catch (Exception e) {
			// Handle the exception appropriately
			e.printStackTrace();
			System.err.println("Unable to initilize the server.");
		}
	}
	
	public Destination getServerTopic(){
		return this.defaultTopic;
	}

	public DefaultChatRoom getChatRoom() {
		// TODO Auto-generated method stub
		return mainDefault;
	}
	
	public void addChatRoom(ChatRoom temp){
		ChatRoomList.addChatRoom(temp);
	}
	
	public ChatRoomContainer getChatRoomContainer(){
		return ChatRoomList;
	}
}
