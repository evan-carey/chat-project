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

public class ConnectingClient implements MessageListener {

	private Connection connection;
	private Session session;
	private Destination loginQueue;
	private MessageProducer producer;

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
			validate(loginQueue);
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}

	private void validate(Destination tempDest) {
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		String username = keyboard.nextLine().trim();
		System.out.print("Enter password: ");
		String password = keyboard.nextLine().trim();

		String account = username + " " + password;

		try {
			producer.send(session.createTextMessage(account),
					DeliveryMode.NON_PERSISTENT, 9,
					Message.DEFAULT_TIME_TO_LIVE);
			System.out.println("Account info sent to server");
		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}
		keyboard.close();

	}

	public void onMessage(Message message) {
		try {
			if (((TextMessage) message).getText().equals("valid")) {
				connection.close();
				new ExampleClient();
			} else {
				System.out.println("Invalid account. Terminating...");
				System.exit(0);
			}
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}

}
