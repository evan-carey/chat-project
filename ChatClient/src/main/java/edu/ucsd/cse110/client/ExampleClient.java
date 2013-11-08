package edu.ucsd.cse110.client;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

import java.util.Random;
import java.util.Scanner;

public class ExampleClient implements MessageListener {
	private static int ackMode;
	// private static String clientQueueName;
	private static String clientTopicName;
	private boolean flag = true;
	private boolean transacted = false;
	private MessageProducer producer;
	private Session session;
	private Connection connection;

	private boolean inServer = false;

	// server-to-client topic
	private static String consumeTopicName;

	static {
		// clientQueueName = "client.messages";
		clientTopicName = "client.messages";
		ackMode = Session.AUTO_ACKNOWLEDGE;
		consumeTopicName = "server.messages";

	}

	public ExampleClient() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"tcp://localhost:61616");
		// Connection connection;
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(transacted, ackMode);
			Destination adminQueue = session.createTopic(clientTopicName);

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
			
			Destination consumeTopic = session.createTopic(consumeTopicName);
			MessageConsumer responseConsumer = session.createConsumer(consumeTopic);

			// This class will handle the messages to the temp queue as well
			responseConsumer.setMessageListener(this);

			enterServer();

			/*
			 * while (flag) { // Now create the actual message you want to send
			 * Scanner keyboard = new Scanner(System.in); //
			 * System.out.print("Enter username: "); // String username =
			 * keyboard.nextLine(); // System.out.print("Enter password: "); //
			 * String password = keyboard.nextLine();
			 * 
			 * System.out.print("Enter Message: \n"); String message =
			 * keyboard.nextLine(); // keyboard.close();
			 * 
			 * if ("logoff".equalsIgnoreCase(message)) { TextMessage txtMessage
			 * = session.createTextMessage();
			 * txtMessage.setText("Client logged off");
			 * //txtMessage.setJMSReplyTo(tempDest); String correlationId =
			 * this.createRandomString(); //
			 * txtMessage.setJMSCorrelationID(correlationId);
			 * this.producer.send(txtMessage);
			 * System.out.println("Successfully logged off");
			 * connection.close(); break; }
			 * 
			 * TextMessage txtMessage = session.createTextMessage();
			 * txtMessage.setText(message);
			 * 
			 * // Set the reply to field to the temp queue you created above, //
			 * this is the queue the server // will respond to
			 * //txtMessage.setJMSReplyTo(tempDest);
			 * 
			 * // Set a correlation ID so when you get a response you know //
			 * which sent message the response is for // If there is never more
			 * than one outstanding message to the // server then the // same
			 * correlation ID can be used for all the messages...if // there is
			 * more than one outstanding // message to the server you would
			 * presumably want to associate // the correlation ID with this //
			 * message somehow...a Map works good String correlationId =
			 * this.createRandomString(); //
			 * txtMessage.setJMSCorrelationID(correlationId);
			 * this.producer.send(txtMessage); }
			 */
		} catch (JMSException e) {
			// Handle the exception appropriately
		}

	}

//	private void validate(Destination tempDest) {
//		Scanner keyboard = new Scanner(System.in);
//		System.out.print("Enter username: ");
//		String username = keyboard.nextLine().trim();
//		System.out.print("Enter password: ");
//		String password = keyboard.nextLine().trim();
//
//		String account = username + " " + password;
//
//		try {
//			producer.send(session.createTextMessage(account),
//					DeliveryMode.NON_PERSISTENT, 9,
//					Message.DEFAULT_TIME_TO_LIVE);
//			System.out.println("Account info sent to server");
//		} catch (JMSException e) {
//			System.err.println(e.getMessage());
//		}
//		keyboard.close();
//
//	}

	private String createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}

	private void enterServer() {

		try {
			
			while (flag) {
				System.out.print(">>");
				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);

				String message = keyboard.nextLine();
				// keyboard.close();

				if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText("Client logged off");
					// txtMessage.setJMSReplyTo(tempDest);
					String correlationId = this.createRandomString();
					// txtMessage.setJMSCorrelationID(correlationId);
					this.producer.send(txtMessage);
					System.out.println("Successfully logged off");
					connection.close();
					// break;
				}

				TextMessage txtMessage = session.createTextMessage();
				txtMessage.setJMSPriority(4);
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
		} catch (JMSException e) {
			// Handle the exception appropriately
		}
	}

	public void onMessage(Message message) {

//		if (!inServer) {
//			try {
//				if (message.getJMSPriority() == 9) {
//					if (((TextMessage) message).getText().equals("valid")) {
//						inServer = true;
//						enterServer();
//						return;
//					} else {
//						System.out.println("Invalid account. Terminating...");
//						System.exit(0);
//					}
//				}
//			} catch (JMSException e) {
//				System.out.println(e.getMessage());
//			}
//		}

			String messageText = null;
			try {
				if (message instanceof TextMessage) {
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
		new ExampleClient();
	}
}