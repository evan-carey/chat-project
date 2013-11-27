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
	private Destination consumeTopic;
	private Destination serverQueue;

	private String username;

	// server-to-client topic
	private static String consumeTopicName;

	static {
		// clientQueueName = "client.messages";
		//clientTopicName = "client.messages";
		ackMode = Session.AUTO_ACKNOWLEDGE;
		consumeTopicName = "server.messages";

	}

	public ExampleClient(String username) {
		
		this.username = username;
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"tcp://localhost:61616");
		// Connection connection;
		try {
            String clientDestination = createRandomString();
            
            connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(transacted, ackMode);
			serverQueue = session.createQueue(consumeTopicName);

			// Setup a message producer to send message to the queue the server
			// is consuming from
			this.producer = session.createProducer(serverQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			//set the Destination (queue/topic) the client consumes from
			consumeTopic = session.createQueue(clientDestination);
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

	private void enterServer() {

		try {
			
			while (flag) {
				System.out.print("[" + username + "]: ");
				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);

				String message = keyboard.nextLine();
				String usermessage = "[" + username + "]: " + message;
				// keyboard.close();

				/*if ("logoff".equalsIgnoreCase(message)) {
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setText("Client logged off");
					// txtMessage.setJMSReplyTo(tempDest);
					String correlationId = this.createRandomString();
					// txtMessage.setJMSCorrelationID(correlationId);
					this.producer.send(txtMessage);
					System.out.println("Successfully logged off");
					connection.close();
					// break;
				}*/

				TextMessage txtMessage = session.createTextMessage();
				//txtMessage.setJMSPriority(4);
				txtMessage.setText(usermessage);
				txtMessage.setJMSCorrelationID(username);
				txtMessage.setJMSReplyTo(consumeTopic);
				txtMessage.setJMSDestination(serverQueue);
				//System.out.println(consumeTopic + txtMessage.getJMSDestination().toString());
				this.producer.send(txtMessage);
			}
		} catch (JMSException e) {
			// Handle the exception appropriately
		}
	}

	public void onMessage(Message message) {

			String messageText = null;
			try {
				if (message instanceof TextMessage) {
					TextMessage textMessage = (TextMessage) message;
					messageText = textMessage.getText();

					System.out.println("\n" + messageText);
					
					if(messageText.equals("connect")){
						produce(message.getJMSReplyTo());
					}
					if(messageText.equals("suscribe"))
						suscribe(message.getJMSReplyTo());
						
					if(messageText.equals("[u]: disconnect"))
						produce(session.createQueue(consumeTopicName));
				}
				
			} catch (JMSException e) {
				// Handle the exception appropriately
			}
		
	}
  
   public void produce(Destination dest) throws JMSException{
	   if(dest instanceof Queue){
		   System.out.println(((Queue) dest).getQueueName());
		   serverQueue = (Queue)dest;
		   this.producer = session.createProducer(serverQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	   }
		   
   }
   
   public void suscribe(Destination dest) throws JMSException{
	   if(dest instanceof Queue){
		   System.out.println(((Queue) dest).getQueueName());
		   serverQueue = (Queue)dest;
		   this.producer = session.createProducer(serverQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	   }
		   
   }

	public static void main(String[] args) {
		new ConnectingClient();
	}
}