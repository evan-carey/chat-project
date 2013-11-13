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
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class ExampleClient implements MessageListener {
	
	/** Username associated with the client */
	private String username;
	private String oldPass;
	private String newPass;

	private static int ackMode;
	// private static String clientQueueName;
	
	//private static String clientTopicName;
	private boolean flag = true;
	private boolean transacted = false;
	
	//Create the necessary variables to connect to the server 
	private MessageProducer producer;
	private MessageProducer accountProducer;
	private Session session;
	private Connection connection;
	
	private Destination editQueueProduce;
	private Destination editQueueConsume;


	public ExampleClient(String username) {
		this.username = username;

		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				ClientConstants.messageBrokerUrl);
		
		// Connection connection;
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(transacted, ClientConstants.ackMode);
			Destination adminQueue = session.createTopic(ClientConstants.clientTopicName);

			// Setup a message producer to send message to the queue the server
			// is consuming from
			this.producer = session.createProducer(adminQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// Create a temporary queue that this client will listen for
			// responses on then create a consumer
			// that consumes message from this temporary queue...for a real
			// application a client should reuse
			// the same temp queue for each message to the server...one temp
			// queue per client
			//Destination tempDest = session.createTemporaryTopic();
			//validate(tempDest);
			
			Destination consumeTopic = session.createTopic(ClientConstants.consumeTopicName);
			MessageConsumer responseConsumer = session.createConsumer(consumeTopic);

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
						
			while (flag) {
				System.out.print(">>");
				
				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();

				//If the user types the keyword "logoff"
				//they will be disconnected
				if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText("Client logged off");
					
					// txtMessage.setJMSReplyTo(tempDest);
					//String correlationId = this.createRandomString();

					// txtMessage.setJMSCorrelationID(correlationId);
					this.producer.send(txtMessage);
					System.out.println("Successfully logged off");
					connection.close();

				}
				else if ("editAccount".equalsIgnoreCase(message)) {
					editAccount();
				}
				

				TextMessage txtMessage = session.createTextMessage();
				txtMessage.setText(message);

				// Set the reply to field to the temp queue you created above,
				// this is the queue the server
				// will respond to
				// txtMessage.setJMSReplyTo(tempDest);

				// Set a correlation ID so when you get a response you know
				// which sent message the response is for
				// If there is never more than one outstanding message to the
				// server then the
				// same correlation ID can be used for all the messages...if
				// there is more than one outstanding
				// message to the server you would presumably want to associate
				// the correlation ID with this
				// message somehow...a Map works good
				//String correlationId = this.createRandomString();
				// txtMessage.setJMSCorrelationID(correlationId);
				this.producer.send(txtMessage);
			}
			return true;
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void getInfo() {
		System.out.println("Please enter your old account info along with a new password. ");
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		this.username = keyboard.nextLine().trim();
		System.out.print("Enter old password: ");
		this.oldPass = keyboard.nextLine().trim();
		System.out.print("Enter new password: ");
		this.newPass = keyboard.nextLine().trim();
	}
	
	private void editAccount() {
		try{
			getInfo();
			//set producer
			editQueueProduce = session.createTopic(ClientConstants.clientTopicName);
			this.accountProducer = session.createProducer(editQueueProduce);
			this.accountProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			
			//set consumer
			editQueueConsume = session.createTemporaryTopic();
			MessageConsumer responseConsumer = session.createConsumer(editQueueConsume);
			responseConsumer.setMessageListener(this);
			
			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.oldPass + " " + this.newPass);
			txtMessage.setJMSReplyTo(editQueueProduce);
			String correlationID = "editAccount";
			txtMessage.setJMSCorrelationID(correlationID);
			this.producer.send(txtMessage);
		} catch (JMSException e){
			System.err.println(e.getMessage());
		}
	}

	public void onMessage(Message message) {

			String messageText = null;
			try {
				if (((TextMessage) message).getText().equals("edited")) {
					System.out.println("Account edited. ");
				}
				else if (message instanceof TextMessage) {
					TextMessage textMessage = (TextMessage) message;
					messageText = textMessage.getText();

					
					System.out.print("\n\"" + messageText 
							+ "\"\n\n>>");
				}
			} catch (JMSException e) {
				// Handle the exception appropriately
			}
		
	}



	public static void main(String[] args) {
		/*
		 * NOTE: If you want to test the messaging without having to
		 * log in first, create a new ExampleClient().
		 * If you want to test logging in too, create a new
		 * ConnectingClient() instead.
		 */
		
		// new ExampleClient();
		new ConnectingClient();	
	}

}
