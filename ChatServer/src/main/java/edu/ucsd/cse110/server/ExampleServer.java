package edu.ucsd.cse110.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
import org.apache.activemq.broker.BrokerService;
 
public class ExampleServer implements MessageListener {
	private static final String USER_FILE = "src/main/java/edu/ucsd/cse110/server/accounts.txt";
	
    private static int ackMode;
    //private static String messageQueueName;
    private static String messageBrokerUrl;
    private static String messageTopicName;
    
    // server-to-client topic
    private static String produceTopicName;
 
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProtocol messageProtocol;
    
    static {
        messageBrokerUrl = "tcp://localhost:61616";
        //messageQueueName = "client.messages";
        messageTopicName = "client.messages";
        produceTopicName = "server.messages";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }
 
    public ExampleServer() {
        try {
            //This message broker is embedded
            BrokerService broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(messageBrokerUrl);
            broker.start();
        } catch (Exception e) {
            //Handle the exception appropriately
        }
 
        //Delegating the handling of messages to another class, instantiate it before setting up JMS so it
        //is ready to handle messages
        this.messageProtocol = new MessageProtocol();
        this.setupMessageQueueConsumer();
    }
 
    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination adminQueue = this.session.createTopic(messageTopicName);
            
            // set server-to-client topic
            Destination produceTopic = this.session.createTopic(produceTopicName);
 
            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(produceTopic);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
 
            //Set up a consumer to consume messages off of the admin queue
            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            //Handle the exception appropriately
        }
    }
 
    public void onMessage(Message message) {
    	
        System.out.println("Message received by server");
    	try {
			if( message.getJMSPriority() == 9) {
				if (validate(((TextMessage) message).getText())) {
					this.replyProducer.send(session.createTextMessage("valid"), DeliveryMode.NON_PERSISTENT, 9, Message.DEFAULT_TIME_TO_LIVE);
					return;
				} else {
					this.replyProducer.send(session.createTextMessage("invalid"), DeliveryMode.NON_PERSISTENT, 9, Message.DEFAULT_TIME_TO_LIVE);
					return;
				}
			}
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	try {
            TextMessage response = this.session.createTextMessage();
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                System.out.println(messageText);
                response.setText(this.messageProtocol.handleProtocolMessage(messageText));
            }
 
            //Set the correlation ID from the received message to be the correlation id of the response message
            //this lets the client identify which message this is a response to if it has more than
            //one outstanding message to the server
            response.setJMSCorrelationID(message.getJMSCorrelationID());
 
            //Send the response to the Destination specified by the JMSReplyTo field of the received message,
            //this is presumably a temporary queue created by the client
            this.replyProducer.send(response);
        } catch (JMSException e) {
            //Handle the exception appropriately
        }
    }
    
    public boolean validate(String account) {
    	BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(USER_FILE));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equalsIgnoreCase(account)) {
					reader.close();
					System.out.println("Account validated");
					return true;
				}
			}
			System.out.println("Invalid username/password combination");
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
    }
    
//    public boolean validatePassword(String password) {
//    	BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new FileReader("accounts.txt"));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				String[] account = line.split(" ");
//				if (account[1].equalsIgnoreCase(password)) {
//					reader.close();
//					return true;
//				}
//			}
//			reader.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return false;
//    }
// 
    public static void main(String[] args) {
        new ExampleServer();
    }
}
