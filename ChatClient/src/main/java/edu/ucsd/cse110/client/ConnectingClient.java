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
	private Destination loginQueue;
	private MessageProducer producer;
	
	private String username, password;

	public ConnectingClient() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"tcp://localhost:61616");
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			loginQueue = session.createTemporaryQueue();
			this.producer = session.createProducer(loginQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			MessageConsumer responseConsumer = session.createConsumer(loginQueue);
			responseConsumer.setMessageListener(this);
			getAccountInfo();
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

		keyboard.close();
		
		verifyAccount(loginQueue);
	}

	private void verifyAccount(Destination tempDest) {
		try {
			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.password);
			txtMessage.setJMSReplyTo(tempDest);
			String correlationId = "verifyAccount";
			txtMessage.setJMSCorrelationID(correlationId);
			this.producer.send(txtMessage);
			System.out.println("Connecting to server...");
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
				connection.close();
				new ExampleClient();
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
