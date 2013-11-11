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

import edu.ucsd.cse110.server.accountinfo.AccountException;
import edu.ucsd.cse110.server.accountinfo.Accounts;
 
public class ExampleServer implements MessageListener {
	
	
    private static int ackMode;
    //private static String messageQueueName;
    private static String messageBrokerUrl;
    private static String messageTopicName;
    
    // server-to-client topic
    private static String produceTopicName;
 
    private Connection connection;
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProtocol messageProtocol;
    
    /** User accounts */
    private Accounts accounts;
    
    static {
        messageBrokerUrl = "tcp://localhost:61616";
        //messageQueueName = "client.messages";
        messageTopicName = "client.messages";
        produceTopicName = "server.messages";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }
 
    public ExampleServer() {
    	accounts = new Accounts();
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
        
        // Username, password verification
    	/*try {
			if (message.getJMSCorrelationID().equals("verifyAccount")) {
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String[] account = ((TextMessage)message).getText().split(" ");
				String username = account[0];
				String password = account[1];
				String responseText = validate(username, password);
				response.setText(responseText);
				this.replyProducer.send(message.getJMSReplyTo(), response);
				return;
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}*/
    	
    	// Regular message handling
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
    
    public String validate(String username, String password) {
    	// check if username is in database
    	if (!accounts.hasUsername(username))
    		return "Account does not exist.";
    	// if it does, verify the password
    	try {
    		if (password.equals(accounts.getPassword(username)))
    			return "valid";
    		else
    			return "Invalid username/password combination.";
    	} catch (AccountException e) {
    		return e.getMessage();
    	}
    }
    
    public static void main(String[] args) {
        new ExampleServer();
    }
}
