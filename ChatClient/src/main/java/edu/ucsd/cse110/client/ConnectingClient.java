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
	
	private String username, password, response;

	public ConnectingClient() {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"tcp://localhost:61616");
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			System.out.print("Do you have an account? (y/n): ");
			getResponse();
			getAccountInfo();
			// set producer
			producerQueue = session.createTopic("client.messages");
			this.producer = session.createProducer(producerQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			
			// set consumer
			consumerQueue = session.createTemporaryTopic();
			MessageConsumer responseConsumer = session.createConsumer(consumerQueue);
			responseConsumer.setMessageListener(this);
			
			if ("n".equalsIgnoreCase(response) || "no".equalsIgnoreCase(response)){
				createAccount();
			}
			verifyAccount();
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void getResponse() {
		Scanner keyboard = new Scanner(System.in);
		this.response = keyboard.nextLine().trim();
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
	private void getAccountInfo() {
		boolean InputAccepted = false;
		if ("n".equalsIgnoreCase(response) || "no".equalsIgnoreCase(response)){
			System.out.println("Please enter your new account info");
		}
		
		while(!InputAccepted){
			Scanner keyboard = new Scanner(System.in);
			System.out.print("Enter username: ");
			this.username = keyboard.nextLine().trim();
			InputAccepted = isValidInput(this.username);
			if(InputAccepted == false)
				System.out.println
					("Username may only contain uppercase letters, lowercase letters, numbers, and underscores");
		}
		InputAccepted = false;
		while(!InputAccepted){
			Scanner keyboard = new Scanner(System.in);
			System.out.print("Enter password: ");
			this.password = keyboard.nextLine().trim();
			InputAccepted = isValidInput(this.username);
			if(InputAccepted == false)
				System.out.println
					("Password may only contain uppercase letters, lowercase letters, numbers, and underscores");
		}
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
	
	private void createAccount() {
		try{
			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.password);
			txtMessage.setJMSReplyTo(consumerQueue);
			String correlationID = "createAccount";
			txtMessage.setJMSCorrelationID(correlationID);
			this.producer.send(txtMessage);
		} catch (JMSException e){
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
			if (((TextMessage) message).getText().equals("created")) {
				System.out.println("Account created. Validating account.");
			} else if (((TextMessage) message).getText().equals("valid")) {
				System.out.println("Account validated. Connecting to server.");
				producer.close();
				session.close();
				connection.close();
				new Client(username);
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
