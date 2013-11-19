package edu.ucsd.cse110.client;

import java.util.Scanner;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;


public class Client extends AbstractClient {

	/** Username associated with the client */
	private String oldPass;
	private String newPass;

	private MessageProducer accountProducer;

	private Destination editQueueProduce;
	private Destination editQueueConsume;
    private Destination broadcastTopic;
    
	

	public Client(String username) {
		super();
		setUsername(username);

		System.out.println(">>>>>>>>>>>>>>" + username);
		enterServer();
	}

	public void enterServer() {
		
		try {
			//broadcastTopic = this.session.createTopic("server.broadcast");
			TextMessage tm = session.createTextMessage("-a " + username);
			tm.setJMSCorrelationID(username);
			tm.setJMSReplyTo(consumerQueue);
			producer.send(tm);

			while (true) {
				System.out.print("[" + username + "]: ");

				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();

				// If the user types the keyword "logoff"
				// they will be disconnected
				if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText(username + " has logged off");

					txtMessage.setJMSCorrelationID("LOGOFF");

					this.producer.send(txtMessage);
					System.exit(0);
				
				} else if ("editAccount".equalsIgnoreCase(message)) {
					editAccount();
				
				} else if ("Command:enterchatroom".equalsIgnoreCase(message)) {
					new EnterChatRoom(username);
				
				} else if (message.equals("disconnect")) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText("disconnect");
					this.producer.send(txtMessage);
					//txtMessage.setJMSCorrelationID("disconnect");
					//txtMessage.setJMSReplyTo(consumerQueue);
					setProducer(producerQueue);
				
				/*} else if(message.equals("setbroadcast")){
				*/	
				} else {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText(message);
					txtMessage.setJMSCorrelationID(username);
					txtMessage.setJMSReplyTo(consumerQueue);
					txtMessage.setJMSDestination(producerQueue);

					this.producer.send(txtMessage);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
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
			if(!InputAccepted)
				System.out.println("Password may only contain uppercase letters, lowercase letters, numbers, and underscores");
		}
	}

	private void editAccount() {
		try {
			getInfo();
			// set producer
			editQueueProduce = session.createTopic(ClientConstants.clientTopicName);
			this.accountProducer = session.createProducer(editQueueProduce);

			this.accountProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			// set consumer
			editQueueConsume = session.createTemporaryTopic();
			MessageConsumer responseConsumer = session.createConsumer(editQueueConsume);
			responseConsumer.setMessageListener(this);

			TextMessage txtMessage = session.createTextMessage();
			txtMessage.setText(this.username + " " + this.oldPass + " "+ this.newPass);
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
					setProducer(producerQueue);
				else if (messageText.equals("setbroadcast")){
					setProducer(message.getJMSReplyTo());
					setConsumer(producerTopic);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
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
