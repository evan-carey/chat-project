package edu.ucsd.cse110.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.activemq.broker.BrokerService;

import edu.ucsd.cse110.server.accountinfo.AccountException;
import edu.ucsd.cse110.server.accountinfo.Accounts;

public class Server implements MessageListener {

	private Connection connection;
	private Session session;
	private boolean transacted = false;
	private MessageProducer replyProducer;
	private MessageProtocol messageProtocol;
	private ServerRunChatRoom serverrunchatroom;// added by JW
	/** User accounts */
	private Accounts accounts;
	private HashMap<String, String> onlineUsers;

	private Map<String, Destination> loggedOn; // hashmap of online users and
												// their Destinations

	public Server() {
		accounts = new Accounts();
		onlineUsers = new HashMap<String, String>();
		loggedOn = new HashMap<String, Destination>();
		try {
			// This message broker is embedded
			BrokerService broker = new BrokerService();
			broker.setPersistent(false);
			broker.setUseJmx(false);
			broker.addConnector(ServerConstants.messageBrokerUrl);
			broker.start();
			serverrunchatroom = new ServerRunChatRoom();// added by JW
		} catch (Exception e) {
			// Handle the exception appropriately
			System.err.println("Unable to initilize the server.");
		}

		// Delegating the handling of messages to another class, instantiate it
		// before setting up JMS so it
		// is ready to handle messages
		this.messageProtocol = new MessageProtocol();
		this.setupMessageQueueConsumer();

		// Attach shutdown hook to server
		ShutdownHook.attachShutdownHook(this);

	}

	private void setupMessageQueueConsumer() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				ServerConstants.messageBrokerUrl);

		try {

			// Create the client-to-server Queue
			connection = connectionFactory.createConnection();
			connection.start();
			this.session = connection.createSession(this.transacted,
					ServerConstants.ackMode);
			// Destination adminQueue =
			// this.session.createTopic(ServerConstants.messageTopicName);

			// Server consumes from adminqueue
			Destination adminQueue = this.session
					.createQueue(ServerConstants.produceTopicName);

			// Create the server-to-client topic
			// Destination produceTopic =
			// this.session.createTopic(ServerConstants.produceTopicName);

			// Setup a message producer to respond to messages from clients, we
			// will get the destination
			// to send to from the JMSReplyTo header field from a Message

			// server produces to nothing (null)
			this.replyProducer = this.session.createProducer(null);
			this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// Set up a consumer to consume messages off of the administration
			// queue
			MessageConsumer consumer = this.session.createConsumer(adminQueue);
			consumer.setMessageListener(this);
			
			
			Destination adminQueue_2 = this.session//
					.createQueue("templistchatroomqueue");//
			MessageConsumer consumer_2 = this.session.createConsumer(adminQueue_2);//
			consumer_2.setMessageListener(this);	//
			
		} catch (JMSException e) {
			// Handle the exception appropriately
		}
	}

	private void addUserOnline(TextMessage tm) throws JMSException {
		if(tm.getText() == null || tm.getJMSMessageID() == null){
			return;
		}
		String clientID = tm.getJMSMessageID();
		String user = tm.getText();
		String[] temp = user.split(" ");
		user = temp[1];

		this.onlineUsers.put(user, clientID);
		System.out.println("User: " + user + " at client: " + clientID);
	}

	private void reportOnlineUsers(Destination dest) throws JMSException {
		if(onlineUsers.isEmpty()){
			TextMessage tm = this.session.createTextMessage();
			tm.setText("No users found");
			this.replyProducer.send(dest, tm);
		}
		String users = "\nOnline Users: \n";

		for (String s : onlineUsers.keySet()) {
			users += "    " + s + "\n";
		}

		System.out.println(users);
		TextMessage tm = this.session.createTextMessage();
		tm.setText(users);
		this.replyProducer.send(dest, tm);
		/**
		 * Find a way to set the reply destination and were golden on this
		 */
		// this.replyProducer.send( dest, this.session.createTextMessage(users)
		// );
	}

	private void handleMessage(Message tmp) throws JMSException {
        TextMessage tm = (TextMessage) tmp;
		String text = tm.getText();

		switch (text.charAt(1)) {
		case 'a':
			addUserOnline(tm);
			break;
		case 'g':
			reportOnlineUsers(tmp.getJMSReplyTo());
			break;
		case 'c': 
			setChat(tmp);
			break;
		default:
			break;
		}

	}

	public void onMessage(Message message) {

		TextMessage tm = (TextMessage) message;
		String text = "";
		Destination userDest;
		boolean chatflag = false;
		try {
			text = tm.getText();
			if(text.length() <= 0){
				return;
			}
			if (text.charAt(0) == '-') {
				handleMessage(message);
			}
		} catch (JMSException e1) {
			e1.printStackTrace();
		}

		// Username, password verification
		try {
			// System.out.println(message.getJMSCorrelationID());
			
			if (message.getJMSCorrelationID() != null
					&& (message.getJMSCorrelationID().equals("createAccount")
							|| message.getJMSCorrelationID().equals(
									"verifyAccount") || message
							.getJMSCorrelationID().equals("editAccount"))) {

				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String[] account = ((TextMessage) message).getText().split(" ");
				System.out.println("Reading account info: " + ((TextMessage) message).getText());
				
				if(((TextMessage) message).getText().equals("edited")){
					return;
				}
				String username = account[0];
				String password = null;
				password = account[1];
				String newPassword = null;
				
				if (message.getJMSCorrelationID().equals("editAccount")) {
					newPassword = account[2];
				}
				String responseText;
				
				if (message.getJMSCorrelationID().equals("createAccount")) {
					responseText = create(username, password);
				} else if (message.getJMSCorrelationID()
						.equals("verifyAccount")) {
					responseText = validate(username, password);
				} else {
					responseText = edit(username, password, newPassword);
					message.setJMSCorrelationID(null);//server will respond with regular message after verification
				}
				response.setText(responseText);
				MessageProducer tempProducer = this.session.createProducer(message.getJMSReplyTo());
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}

			// added by JW
			if (message.getJMSCorrelationID() != null
					&& message.getJMSCorrelationID().equalsIgnoreCase("listChatRoom")) {
				System.out.println("RECEIVED LISTCHATROOM COMMAND");
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String responseText = serverrunchatroom.transmitChatRoomList();
				
				System.out.println(responseText);
				
				response.setText(responseText);
				MessageProducer tempProducer = this.session
						.createProducer(message.getJMSReplyTo());
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}
			

			
			//createchatroom
			if (message.getJMSCorrelationID() != null
					&& message.getJMSCorrelationID().equalsIgnoreCase("createChatRoom")) {
				System.out.println("RECEIVED createchatroom COMMAND");
				String[] command = ((TextMessage)message).getText().split(" ");
				
				MessageProducer tempProducer = this.session
						.createProducer(message.getJMSReplyTo());
				
				if(serverrunchatroom.roomExists(command[1])){
					TextMessage response = this.session.createTextMessage();
					response.setJMSCorrelationID(message.getJMSCorrelationID());
					response.setText("Room already exists. Failure;");
					tempProducer.send(message.getJMSReplyTo(), response);
					tempProducer.close();
					return;
				}
				serverrunchatroom.createChatRoom(command[1]);
				
				
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());

				
				response.setText("creation complete");
				tempProducer.send(message.getJMSReplyTo(), response);
				String responseText = serverrunchatroom.transmitChatRoomList();
				response.setText(responseText);
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}
			

			//delete user from list on logoff 
			//Really need to split these cases
			if (message.getJMSCorrelationID() != null
					&& message.getJMSCorrelationID().equals("LOGOFF")) {
				String[] msg = tm.getText().split(" ");
				onlineUsers.remove(msg[0]);
				return;
			}
			
			//Add user to list when logging on

			/*
			if (message.getJMSCorrelationID() != null
					&& message.getJMSCorrelationID().equals("LOGON")) {
				String msg = tm.getText();
				addUserOnline(tm);
				return;
			}*/
			
		} catch (JMSException e) {
			e.printStackTrace();
		}

		// Regular message handling
		System.out.println("Message received by server");
		try {
			TextMessage response = this.session.createTextMessage();
			// TextMessage response2 = this.session.createTextMessage();
			if (message instanceof TextMessage) {
				TextMessage txtMsg = (TextMessage) message;
				String messageText = txtMsg.getText();
				String user = txtMsg.getJMSCorrelationID();

				// if user is not logged on, add it to the Map of logged on
				// users
				if (!loggedOn.containsKey(user) && user != null) {
					userDest = message.getJMSReplyTo();
					loggedOn.put(user, userDest);
					System.out.println("map users: " + loggedOn.size());
				}

				if (messageText.contains("-chat")) {
					handleMessage(message);
					chatflag = true; //<<< this is garbage, use for testing purposes
				}
				// respond message
				// TextMessage response = this.session.createTextMessage();

				if (messageText.contains("get")
						&& messageText.contains("online")
						&& messageText.contains("user")) {

					reportOnlineUsers(tm.getJMSDestination());
					return;

					// txtMsg.setText("-g" + messageText);
					// Message tm1 = session.createTextMessage( "-g" );
					// tm.setJMSDestination(tm.getJMSReplyTo() );
					// handleMessage( session.createTextMessage("-g") );
					//
					// return;
				}

				System.out.println(message.getJMSCorrelationID()+">> " + messageText);
				response.setText(this.messageProtocol.handleProtocolMessage(messageText));
				// response.setJMSDestination(message.getJMSDestination());
			}

			// Set the correlation ID from the received message to be the
			// correlation id of the response message
			// this lets the client identify which message this is a response to
			// if it has more than
			// one outstanding message to the server
			if (!chatflag) {
				response.setJMSCorrelationID(message.getJMSCorrelationID());

				// Send the response to the Destination specified by the
				// JMSReplyTo field of the received message,
				// this is presumably a temporary queue created by the client
				try{
					this.replyProducer.send(message.getJMSReplyTo(), response);
				}catch(UnsupportedOperationException e){
					return;
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public String validate(String username, String password) {
		// check if username is in database
		if (!accounts.hasUsername(username))
			return "Account does not exist.";
		// if it does, verify the password
		try {
			if (password.equals(accounts.getPassword(username)))

				return "valid";
			else
				return "Invalid username/password combination.";
		} catch (AccountException e) {
			return e.getMessage();
		}
	}

	public String create(String username, String password) {
		try {
			accounts.addAccount(username, password);
			return "created";
		} catch (AccountException e) {
			return e.getMessage();
		}
	}

	public String edit(String username, String oldPass, String newPass) {
		try {
			accounts.setPassword(username, oldPass, newPass);
			return "edited";
		} catch (AccountException e) {
			return e.getMessage();
		}
	}

	private void setChat(Message message) throws JMSException {
		TextMessage tmp = (TextMessage) message;
		String[] msg = tmp.getText().split(" ");
		String user2 = msg[1];
		TextMessage response = this.session.createTextMessage();
		TextMessage response2 = this.session.createTextMessage();
		System.out.println("Chat session between: " + message.getJMSCorrelationID() + " and " + user2);

		// set new producer for user1
		response.setText("connect");
		response.setJMSReplyTo(loggedOn.get(user2));

		// set new producer for user2
		response2.setText("connect");
		response2.setJMSReplyTo(message.getJMSReplyTo());

		this.replyProducer.send(message.getJMSReplyTo(), response);
		this.replyProducer.send(loggedOn.get(user2), response2);
	}

	/**
	 * Shutdown Hook class to handle saving account information when program
	 * terminates.
	 */
	private static class ShutdownHook {
		Server server;

		private ShutdownHook(Server server) {
			this.server = server;
		}

		public static void attachShutdownHook(final Server server) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.println("Saving account information to file...");
					server.accounts.writeToFile();
					System.out.println("Done!");
				}
			});
		}
	}

	public static void main(String[] args) {
		// start server
		new Server();

		// ugly hack to gracefully terminate server
		System.out
				.println("Server initialized. To terminate, press ENTER in the console.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Terminating server.");
		System.exit(0);

	}
}
