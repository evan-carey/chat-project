package edu.ucsd.cse110.client;

import java.util.Scanner;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class ChatClient implements MessageListener{

	private static Connection connection;
	private static Session session;
	private static Destination destination;

	private String username;
	private String password;
	private ChatClient client;

	public ChatClient(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public static void main(String[] args) throws JMSException {

		// Log in screen
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		String username = keyboard.nextLine();
		System.out.print("Enter password: ");
		String password = keyboard.nextLine();

		ChatClient client = new ChatClient(username, password);
		// Verify client with server
		

		// TODO Auto-generated method stub
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				Constants.URL);
		connection = connectionFactory.createConnection();
		connection.start();

		// Create session for receiving message
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// Getting the Queue destination
		destination = session.createQueue(Constants.DEFAULTQUEUE);

		// Get message from client
		System.out.print("Enter message: ");
		String message = keyboard.nextLine();

		// client.receiveMessage();
		keyboard.close();

		// System.out.println("Client connected");
		client.produceMessage(message);
		//client.receiveMessage();
	}

	/* SHOULD NOT BE STATIC */
	public void receiveMessage() {
		new Consumer(connection, session, destination);
	}

	/* SHOULD NOT BE STATIC */
	public void produceMessage(String stringMessage) {
		new Producer(connection, session, destination, stringMessage);
	}

	public void onMessage(Message message) {
		new Consumer(connection, session, destination);
	}
	
	
}
