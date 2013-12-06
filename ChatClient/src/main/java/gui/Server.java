package gui;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.swing.DefaultListModel;

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
			

			// This message broker is embedded
//			BrokerService broker = new BrokerService();
//			broker.setPersistent(false);
//			broker.setUseJmx(false);
//			broker.addConnector("tcp://localhost:61616");
//			broker.start();
			
			connection = connectionFactory.createConnection();
			connection.start();
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			this.defaultTopic = session.createQueue("server.messages");
			
//			myConsumer = session.createConsumer(defaultTopic);
//			replyProducer = session.createProducer(null);
//			myConsumer.setMessageListener(this);		
		} catch (Exception e) {
			// Handle the exception appropriately
			e.printStackTrace();
			System.err.println("Unable to initilize the server.");
		}
	}
	
//	@Override
//	public void onMessage(Message message) {
//
//
//		try {
//			Destination dest = message.getJMSReplyTo();
//			TextMessage tm = (TextMessage) message;
//
//			replyProducer.send(dest, tm);
//			
//		} catch (JMSException e1) {
//			e1.printStackTrace();
//		}
//	}
	
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
//	public static void main(String[] args) throws JMSException {
//		new Server();
//	}
}
