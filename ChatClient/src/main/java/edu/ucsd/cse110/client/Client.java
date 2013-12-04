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
	//private Destination broadcastTopic;
	private String multicastTopic;
	private boolean broadcastFlag = false;
	private boolean multicastFlag = false;
	private boolean privateChat = false;
	private String privateObject;

	public Client(String username) {
		super();
		setUsername(username);
		multicastTopic = username + ".multicast";
		System.out.println(">>>>>>>>>>>>>>" + username);
		enterServer();
	}

	public void enterServer() {

		try {
			// broadcastTopic = this.session.createTopic("server.broadcast");
			TextMessage tm = session.createTextMessage(" ");
			tm.setJMSCorrelationID(username);
			tm.setJMSReplyTo(consumerQueue);
			producer.send(tm);

			while (true) {
				//System.out.print("[" + username + "]: ");

				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();

				// If the user types the keyword "logoff", they will be disconnected from the server
				if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText(username + " has logged off");

					txtMessage.setJMSCorrelationID("LOGOFF");

					this.producer.send(txtMessage);
					System.exit(0);

				} else if ("editAccount".equalsIgnoreCase(message)) {
					editAccount();

				} else if ("whereami".equalsIgnoreCase(message)) {
					whereAmI();

				} else if ("Command:enterchatroom".equalsIgnoreCase(message)) {
					new EnterChatRoom(username);
				
				} else if ("cancel broadcast".equalsIgnoreCase(message)) {
					if(broadcastFlag){
						setProducer(producerQueue);
						broadcastFlag = false; 
					}
				
				} else if ("cancel multicast".equalsIgnoreCase(message)) {
					if(multicastFlag) {
						setProducer(producerQueue);
					
						TextMessage txtMessage = session.createTextMessage();
						txtMessage.setText(multicastTopic);
						txtMessage.setJMSCorrelationID("cancelmulticast");
						txtMessage.setJMSReplyTo(consumerQueue);
						txtMessage.setJMSDestination(producerQueue);
						this.producer.send(txtMessage);
						multicastFlag = false;
					}
				} else if (message.equals("disconnect")) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText("disconnect");
					//txtMessage.setJMSReplyTo(message.get);
					this.producer.send(txtMessage);
					// txtMessage.setJMSCorrelationID("disconnect");
					// txtMessage.setJMSReplyTo(consumerQueue);
					privateChat = false;
					setProducer(producerQueue);

					/*
					 * } else if(message.equals("setbroadcast")){
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

	private void whereAmI() {
		if (broadcastFlag) {
			System.out.println("You are in Broadcast Mode");
			return;
		}
		if (multicastFlag) {
			System.out.println("You are in Multicast Mode");
			return;
		}
		if (privateChat) {
			System.out.println("You are in a Peer-to-Peer chat with" + privateObject);
			return;
		}
		System.out.println("You are sending messages to the Server");
		return;
	}

	private void getInfo() {
		System.out.println("Please enter your old account info along with a new password. ");

		boolean InputAccepted = false;

		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter username: ");
		this.username = keyboard.nextLine().trim();

		System.out.print("Enter old password: ");
		this.oldPass = keyboard.nextLine().trim();

		while (!InputAccepted) {
			Scanner keyboard2 = new Scanner(System.in);
			System.out.print("Enter new password: ");
			this.newPass = keyboard2.nextLine().trim();
			InputAccepted = isValidInput(this.newPass);
			if (!InputAccepted)
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
			txtMessage.setText(this.username + " " + this.oldPass + " " + this.newPass);
			txtMessage.setJMSReplyTo(editQueueProduce);
			String correlationID = "editAccount";
			txtMessage.setJMSCorrelationID(correlationID);

			// this.producer.send(txtMessage);
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

				if(message.getJMSCorrelationID()!=null && message.getJMSCorrelationID().equals("setMulticastConsumer")){
					String[] tempName=messageText.split("multicast");
					System.out.println("You will be multicast by "+ tempName[0]);
					setTopicConsumer(messageText);
					return;
				}

				if (message.getJMSCorrelationID() != null && message.getJMSCorrelationID().equals("failtosetmulticast")) {
					System.out.println("Username Parameters not valid! Please reenter your command, this command will do nothing");
					return;
				}
				
				if (message.getJMSCorrelationID()!=null && message.getJMSCorrelationID().equals("removemulticastconsumer")){
					TextMessage temp=((TextMessage) message);
					String arg=temp.getText();
					//System.out.println("recieved "+message.getJMSCorrelationID()+" and to pass"+arg);// for test
					removeTopicConsumer(arg);
					
					String[] tempName=arg.split(".multicast");
					System.out.println(tempName[0]+" just cancelled multicasting to you");
					return;
				}

				System.out.println("[" + message.getJMSCorrelationID() + "]: " + messageText);

				if (messageText.equals("connect")) {
					privateChat = true;
					setProducer(message.getJMSReplyTo());
					privateObject = message.getJMSCorrelationID();
				} else if (messageText.equals("disconnect")) {
					privateChat = false;
					
					setProducer(producerQueue);
				} else if (messageText.equals("setbroadcast")) {
					setTopicProducer("publicBroadcast");
					broadcastFlag = true;
				} else if (messageText.equals("setmulticast")) {
					setTopicProducer(multicastTopic);
					multicastFlag = true;
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
