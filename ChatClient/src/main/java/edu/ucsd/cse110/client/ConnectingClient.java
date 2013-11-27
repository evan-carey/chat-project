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

public class ConnectingClient implements MessageListener {

	private Connection connection;
	private Session session;
	private Destination producerQueue;
	private Destination consumerQueue;
	private MessageProducer producer;
	
	private String username, password;

	public ConnectingClient() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"tcp://localhost:61616");
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			getAccountInfo();
			// set producer
			producerQueue = session.createQueue("server.messages");
			this.producer = session.createProducer(producerQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			
			// set consumer
			consumerQueue = session.createTemporaryQueue();
			MessageConsumer responseConsumer = session.createConsumer(consumerQueue);
			responseConsumer.setMessageListener(this);
			
			verifyAccount();
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void getAccountInfo() {
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		this.username = keyboard.nextLine().trim();
		System.out.print("Enter password: ");
		this.password = keyboard.nextLine().trim();

	}

	private void verifyAccount() {
		try {
			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.password);
			txtMessage.setJMSReplyTo(consumerQueue);
			
			String correlationId = "verifyAccount";
			txtMessage.setJMSCorrelationID(correlationId);
			this.producer.send(txtMessage);
			//System.out.println("Connecting to server...");
		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}

	}

	private String createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}
	
	public void onMessage(Message message) {
		try {
			if (((TextMessage) message).getText().equals("valid")) {
				System.out.println("Account validated. Connecting to server.");
				session.close();
				connection.close();
				new ExampleClient(username);
			} else {
				System.out.println(((TextMessage) message).getText());
				System.out.println("Invalid account. Terminating...");
				System.exit(0);
			}
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}

}
