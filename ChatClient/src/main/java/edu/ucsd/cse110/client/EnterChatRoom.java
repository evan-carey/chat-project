package edu.ucsd.cse110.client;

import java.util.Scanner;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
 
public class EnterChatRoom implements MessageListener {
	/** Username associated with the client */
	private String username;

	// Create the necessary variables to connect to the server
	private MessageProducer producer;
	private MessageProducer producer_chatroom;
	private MessageConsumer responseConsumer;
	private MessageConsumer responseConsumer_chatroom;
	private Session session;
	private Connection connection;
	private String[] ChatRoomStringList;
	private Destination producerQueue;
	private Destination consumerQueue;
	private String currentChatRoom;

	public EnterChatRoom(String username) throws JMSException {
		this.username = username;
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ClientConstants.messageBrokerUrl);

		try {
			// create connection
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, ClientConstants.ackMode);
			
			// set producer
			Destination adminQueue = session.createQueue(ClientConstants.consumeTopicName);
			this.producer = session.createProducer(adminQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// set consumer
			consumerQueue = session.createTemporaryTopic();
			responseConsumer = session.createConsumer(consumerQueue);
			responseConsumer.setMessageListener(this);
			//System.out.println("You are attemping to enter a chatroom....");
			//System.out.println("Current chatrooms are:");
			//System.out.println("Please enter the name of the chatroom you want to join in");
			//commandChatRoom("listchatroom");
			//selectChatRoom();
			// responseConsumer.close(); //unsubscribe from the temporary topic
			// used to transmit chatroomlist
			//inChatRoom();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	
	public void EnterChatRoomNow() throws JMSException{
		new EnterChatRoom(username);
		System.out.println("You are attemping to enter a chatroom....");
		System.out.println("Current chatrooms are:");
		System.out.println("Please enter the name of the chatroom you want to join in");
		commandChatRoom("listchatroom");
		selectChatRoom();
		// responseConsumer.close(); //unsubscribe from the temporary topic
		// used to transmit chatroomlist
		inChatRoom();
		
	}
	
	
	public void onMessage(Message message) {
		try {
			ChatRoomStringList = ((TextMessage) message).getText().split(" ");
		} catch (JMSException e) {
			e.printStackTrace();
		}
		String messageText = null;
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				messageText = textMessage.getText();

				System.out.print("\n\"" + messageText + "\"\n\n>>");
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}

	}

	public void commandChatRoom(String command) throws JMSException {
		txtSender(command, command);
	}

	public void selectChatRoom() throws JMSException {
		// Do you want to create one? Implement later and also:created chatroom
		// never deleted
		boolean chatroomflag = false;
		String chatroomname = null;
		// System.out.println("Please enter the name of the chatroom you want to join in");
		while (!chatroomflag) {
			System.out.print(">>");

			Scanner keyboard = new Scanner(System.in);
			chatroomname = keyboard.nextLine();
			
			for (String chatroomentry : ChatRoomStringList) {
				if (chatroomname.equals(chatroomentry))
					chatroomflag = true;
			}
			if (!chatroomflag) {
				System.out.println("Chatroom name is case sensitive, your chatroom name is not in the list, please reselect");
			}
		}

		// send chatroom login message:
		currentChatRoom = chatroomname;
		txtSender(username + " " + currentChatRoom, "chatroomlogin");

		// set producer
		Destination consumeTopic_chatroom = session.createTopic(chatroomname);
		this.producer_chatroom = session.createProducer(consumeTopic_chatroom);
		this.producer_chatroom.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
		// set consumer
		responseConsumer_chatroom = session.createConsumer(consumeTopic_chatroom);
		responseConsumer_chatroom.setMessageListener(this);
		return;
	}

	public void inChatRoom() {

		try {
			while (true) {
				System.out.print(">>");

				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();

				if ("C:LISTROOMS".equalsIgnoreCase(message)) {
					listChatRoom();
					continue;
				}

				if ("C:LISTUSERS".equalsIgnoreCase(message)) {
					listChatRoomUsers();
					continue;
				}

				if ("C:CREATE".equalsIgnoreCase(message)) {
					createChatRoom();
					continue;
				}
				if ("LOGOFF".equalsIgnoreCase(message)) {
					logoff();
					continue;
				}
				if ("C:HELP".equalsIgnoreCase(message)) {
					listChatroomCommands();
					continue;
				}

				if ("C:quit".equalsIgnoreCase(message)) {
					System.out.println("Client quit current chatroom, return to default interface");
					txtSender(username + " " + currentChatRoom, "chatroomlogout");
					
					//Closed these instead of closing connection
					responseConsumer_chatroom.close();
					producer_chatroom.close();
					
					//Don't us the below method to close, use the two above
					//connection.close();
					return;
				}

				if ("whereami".equalsIgnoreCase(message)) {
					
					System.out.println("You are in the ChatRoom:"
							+ currentChatRoom);

					continue;
				}
				
				message="["+username+"]"+":"+message;
				TextMessage txtMessage = session.createTextMessage();
				txtMessage.setText(message);

				this.producer_chatroom.send(txtMessage);
			}

		} catch (JMSException e) {
			e.printStackTrace();
		}

	}
	
	
	public boolean listChatRoom() throws JMSException{
		try{
			System.out.println("The current chatroom list");
			commandChatRoom("listchatroom");
			return true;
		}catch(JMSException e){
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	public boolean listChatRoomUsers() throws JMSException{
		try{
			System.out.println("The current chatroom userlist is:");
			txtSender(username + " " + currentChatRoom, "listchatroomusers");
			return true;
		}catch(JMSException e){
			System.err.println(e.getMessage());
		}
		return false;
	}
	

	public String createChatRoom() throws JMSException{
		try{
			System.out.println("Please enter the name of the chatroom you want to create.");
			System.out.print(">>");
			Scanner myKeyboard = new Scanner(System.in);
			String chatRoomName = myKeyboard.nextLine();
			//myKeyboard.close();
			
			txtSender("createchatroom " + chatRoomName, "createchatroom");
			return chatRoomName;
		}catch(JMSException e){
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	
	public void logoff(){
		System.out.println("You must first exit the chat room to logoff...");
	}
	
	
	public void txtSender(String content, String correlationid) throws JMSException {
		TextMessage txtMessage = session.createTextMessage();
		txtMessage.setText(content);
		txtMessage.setJMSReplyTo(consumerQueue);
		String correlationId = correlationid;
		txtMessage.setJMSCorrelationID(correlationId);
		this.producer.send(txtMessage);
	}

	
	public static void listChatroomCommands(){
		System.out.println("*********CHATROOM COMMANDS***********");
		System.out.println("whereami");
		System.out.println("   Displays the users current status.");
		System.out.println("c:listroom");
		System.out.println("   Lists all the existing chatrooms.");
		System.out.println("c:listusers");
		System.out.println("   Lists all the users in the current chatroom");
		System.out.println("c:create");
		System.out.println("   Allows the user to create a room, provided it "
				+ "doesn't alreay exist");		
		System.out.println("c:quit");
		System.out.println("  Closes the connection to the current chatroom"
				+ " and returns user to the Server");
		System.out.println("logoff");
		System.out.println("  Cannot logoff while in an active chatroom. First"
				+ " type c:quit to get to the server they type logoff.");
		System.out.println("**************************");
	
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws JMSException {
		new EnterChatRoom("testname");
	}

}
