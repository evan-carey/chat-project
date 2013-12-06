package edu.ucsd.cse110.client;

import java.util.Random;
import java.util.Scanner;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class ConnectingClient extends AbstractClient {


	private String response;
	private int maxAttempts;


	public ConnectingClient() {
		super();
		maxAttempts = 3;
		System.out.print("Do you have an account? (y/n): ");
		getResponse();
		getAccountInfo();
		if ("n".equalsIgnoreCase(response) || "no".equalsIgnoreCase(response)) {
			createAccount();
		}
		verifyAccount();

	}

	public void getResponse() {
		Scanner keyboard = new Scanner(System.in);
		this.response = keyboard.nextLine().trim();
	}
	
	public String returnResponse(){
		String LastInput = null;
		LastInput = this.response;
		return LastInput;
	}

	private void getAccountInfo() {
		boolean InputAccepted = false;
		if ("n".equalsIgnoreCase(response) || "no".equalsIgnoreCase(response)) {
			System.out.println("Please enter your new account info");
		}

		while (!InputAccepted) {
			Scanner keyboard = new Scanner(System.in);
			System.out.print("Enter username: ");
			this.username = keyboard.nextLine().trim();
			InputAccepted = isValidInput(this.username);
			if (InputAccepted == false)
				System.out.println("Username may only contain uppercase letters, lowercase letters, numbers, and underscores");
		}
		InputAccepted = false;
		while (!InputAccepted) {
			Scanner keyboard = new Scanner(System.in);
			System.out.print("Enter password: ");
			this.password = keyboard.nextLine().trim();
			InputAccepted = isValidInput(this.username);
			if (InputAccepted == false)
				System.out.println("Password may only contain uppercase letters, lowercase letters, numbers, and underscores");

		}
	}


    private void createAccount() {
		try {
			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.password);
			txtMessage.setJMSReplyTo(consumerQueue);
			String correlationID = "createAccount";
			txtMessage.setJMSCorrelationID(correlationID);
			this.producer.send(txtMessage);
		} catch (JMSException e) {
			System.err.println(e.getMessage());
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

				Client client = new Client(username);
				client.enterServer();

			} else {

				maxAttempts -= 1;
				System.out.println(((TextMessage) message).getText());
				if (maxAttempts > 0) {
					System.out.println("Invalid account. You have " + maxAttempts + " attempts remaining.");
					System.out.println("Hit Return to try again.");
					getResponse();
					getAccountInfo();
					verifyAccount();
				} else {
					System.out.println("Invalid account. Terminating...");
					System.exit(0);
				}

			}
		} catch (JMSException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 0)
			ClientConstants.messageBrokerUrl = "tcp://" + args[0];
		new ConnectingClient();
	}

}
