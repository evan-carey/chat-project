package edu.ucsd.cse110.client;

import java.util.Random;
import java.util.Scanner;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class Client implements MessageListener {

	/** Username associated with the client */
	private String username;
	private String oldPass;
	private String newPass;

	private static int ackMode;
	// private static String clientQueueName;

	// private static String clientTopicName;
	private boolean flag = true;
	private boolean transacted = false;

	// Create the necessary variables to connect to the server
	private MessageProducer producer;
	private MessageProducer accountProducer;
	private Session session;
	private Session accountSession;
	private Connection connection;

	private Destination editQueueProduce;
	private Destination editQueueConsume;

	private Destination adminQueue;
	private Destination consumeQueue;
	// Randomly Generated Client Queue
	private String clientQueue;
	// Randomly Generated Client Topic
	private String clientTopic;


	public Client(String username) {
		this.username = username;

		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				ClientConstants.messageBrokerUrl);
		System.out.println(">>>>>>>>>>>>>>" + username);

		// attach shutdown hook to client
		ShutdownHook.attachShutdownHook(this);

		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(transacted,
					ClientConstants.ackMode);


			// adminQueue =
			// session.createTopic(ClientConstants.consumeTopicName);

			// client Produces to adminQueue ("server.messages")
			adminQueue = session.createQueue(ClientConstants.consumeTopicName);


			// Setup a message producer to send message to the queue the server
			// is consuming from
			this.producer = session.createProducer(adminQueue);
			System.out.println("server" + adminQueue);

			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// Create a temporary queue that this client will listen for
			// responses on then create a consumer
			// that consumes message from this temporary queue...for a real
			// application a client should reuse
			// the same temp queue for each message to the server...one temp
			// queue per client
			// Destination tempDest = session.createTemporaryTopic();
			// validate(tempDest);

			// client consumes from a randomly generated queue
			clientQueue = createRandomString();
			consumeQueue = session.createQueue(clientQueue);
			MessageConsumer responseConsumer = session.createConsumer(consumeQueue);
			System.out.println("client:" + consumeQueue);
			// Destination consumeTopic = session.createTopic(clientTopic);
			// MessageConsumer responseConsumer =
			// session.createConsumer(consumeTopic);


			// This class will handle the messages to the temp queue as well
			responseConsumer.setMessageListener(this);

			enterServer();

		} catch (JMSException e) {
			// Handle the exception appropriately
		}

	}

	private String createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}

	public boolean enterServer() {

		try {

			TextMessage tm = session.createTextMessage("-a " + username);
			tm.setJMSCorrelationID(username);
			tm.setJMSReplyTo(consumeQueue);
			producer.send(tm);

			while (flag) {
				System.out.print("[" + username + "]: ");

				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();

				// If the user types the keyword "logoff"
				// they will be disconnected
				if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText(username + " has logged off");

					txtMessage.setJMSCorrelationID(username);

					// txtMessage.setJMSReplyTo(tempDest);
					// String correlationId = this.createRandomString();

					// txtMessage.setJMSCorrelationID(correlationId);
					this.producer.send(txtMessage);
					System.exit(0);
				} else if ("editAccount".equalsIgnoreCase(message)) {
					editAccount();
				} else if ("Command:enterchatroom".equalsIgnoreCase(message)) {
					new EnterChatRoom(username);
				} else if (message.equals("disconnect")) {
					setProducer(adminQueue);
				} else {

					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText(message);
					txtMessage.setJMSCorrelationID(username);
					txtMessage.setJMSReplyTo(consumeQueue);
					txtMessage.setJMSDestination(adminQueue);
					// Set the reply to field to the temp queue you created
					// above,
					// this is the queue the server
					// will respond to
					// txtMessage.setJMSReplyTo(tempDest);

					// Set a correlation ID so when you get a response you know
					// which sent message the response is for
					// If there is never more than one outstanding message to
					// the
					// server then the
					// same correlation ID can be used for all the messages...if
					// there is more than one outstanding
					// message to the server you would presumably want to
					// associate
					// the correlation ID with this
					// message somehow...a Map works good
					// String correlationId = this.createRandomString();
					// txtMessage.setJMSCorrelationID(correlationId);
					this.producer.send(txtMessage);
				}
			}
			return true;
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return false;
	}

	
	private boolean isValidInput(String text){
		if(text == null || text.length() <= 0 )
			return false;
	    char[] c = text.toCharArray();
		for(int i = 0; i < c.length; i++){
			if((c[i] >= 'A' && c[i] <='Z') || (c[i] >= 'a' && c[i] <='z') || (c[i] >= '0' && c[i] <='9') || c[i] == '_'){
				continue;
			}
			else
				return false;
		}
		
		return true;
	}

	private void getInfo() {
		System.out.println("Please enter your old account info along with a new password. ");

		boolean InputAccepted = false;
		
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		this.username = keyboard.nextLine().trim();
		

		System.out.print("Enter old password: ");
		this.oldPass = keyboard.nextLine().trim();
		
		while(!InputAccepted){
			Scanner keyboard2 = new Scanner(System.in);
			System.out.print("Enter new password: ");
			this.newPass = keyboard2.nextLine().trim();
			InputAccepted = isValidInput(this.newPass);
			if(InputAccepted == false)
				System.out.println
					("Password may only contain uppercase letters, lowercase letters, numbers, and underscores");
		}
	}

	private void editAccount() {
		try {
			getInfo();
			// set producer
			editQueueProduce = session
					.createTopic(ClientConstants.clientTopicName);
			this.accountProducer = session.createProducer(editQueueProduce);

			this.accountProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// set consumer
			editQueueConsume = session.createTemporaryTopic();
			MessageConsumer responseConsumer = session
					.createConsumer(editQueueConsume);
			responseConsumer.setMessageListener(this);

			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.oldPass + " "
					+ this.newPass);
			txtMessage.setJMSReplyTo(editQueueProduce);
			String correlationID = "editAccount";
			txtMessage.setJMSCorrelationID(correlationID);

			//this.producer.send(txtMessage);

			this.accountProducer.send(txtMessage);

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}
	}

	public void onMessage(Message message) {

		String messageText = null;
		try {
			if (((TextMessage) message).getText().equals("edited")) {
				System.out.println("Account edited. ");
				accountProducer.close();
				session.close();
			} else if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				messageText = textMessage.getText();

				System.out.println("["+message.getJMSCorrelationID()+"]: "+messageText);

				if (messageText.equals("connect"))
					setProducer(message.getJMSReplyTo());
				else if (messageText.equals("disconnect"))
					setProducer(adminQueue);
			}
		} catch (JMSException e) {
			// Handle the exception appropriately
		}

	}

	private void setProducer(Destination dest) throws JMSException {
		if (dest instanceof Queue) {
			// System.out.println(((Queue) dest).getQueueName());
			Destination suscribe = (Queue) dest;
			//adminQueue = (Queue)dest;
			//System.out.println(suscribe);
			this.producer = session.createProducer(suscribe);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
	}

	/**
	 * Shutdown Hook class to close connections when client logs off.
	 */
	private static class ShutdownHook {
		Client client;

		private ShutdownHook(Client client) {
			this.client = client;
		}

		public static void attachShutdownHook(final Client client) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.print("Logging off...");
					try {
						client.producer.close();
						client.session.close();
						client.connection.close();
					} catch (JMSException e) {
						e.printStackTrace();
					}
					System.out.println(" Done!");

				}
			});
		}
	}

	public static void main(String[] args) {
		/*
		 * NOTE: If you want to test the messaging without having to log in
		 * first, create a new Client(). If you want to test logging in too,
		 * create a new ConnectingClient() instead.
		 */

		// new Client();
		new ConnectingClient();
	}

}
