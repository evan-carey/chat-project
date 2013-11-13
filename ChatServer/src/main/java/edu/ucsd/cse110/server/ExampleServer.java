package edu.ucsd.cse110.server;

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
	
 
    private Connection connection;
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProtocol messageProtocol;
    
    /** User accounts */
    private Accounts accounts;
    
 
    public ExampleServer() {
    	accounts = new Accounts();
        try {
            //This message broker is embedded
            BrokerService broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(ServerConstants.messageBrokerUrl);
            broker.start();
        } catch (Exception e) {
            //Handle the exception appropriately
        	System.err.println("Unable to initilize the server.");
        }
 
        //Delegating the handling of messages to another class, instantiate it before setting up JMS so it
        //is ready to handle messages
        this.messageProtocol = new MessageProtocol();
        this.setupMessageQueueConsumer();
    }
 
    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ServerConstants.messageBrokerUrl);
        
        try {
        	
        	//Create the client-to-server Queue
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ServerConstants.ackMode);
            Destination adminQueue = this.session.createTopic(ServerConstants.messageTopicName);
            
            //Create the server-to-client topic
            Destination produceTopic = this.session.createTopic(ServerConstants.produceTopicName);
 
            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(produceTopic);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
 
            //Set up a consumer to consume messages off of the administration queue
            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        
        } catch (JMSException e) {
            //Handle the exception appropriately
        }
    }
 
    public void onMessage(Message message) {
        
        // Username, password verification
    	try {
    		//System.out.println(message.getJMSCorrelationID());
			if (message.getJMSCorrelationID() != null && (message.getJMSCorrelationID().equals("createAccount") ||
					message.getJMSCorrelationID().equals("verifyAccount") || 
					message.getJMSCorrelationID().equals("editAccount"))) {
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String[] account = ((TextMessage)message).getText().split(" ");
				String username = account[0];
				String password = account[1];
				String newPassword = null;
				if (message.getJMSCorrelationID().equals("editAccount")){
					newPassword = account[2];
				}
				String responseText;
				if (message.getJMSCorrelationID().equals("createAccount")){
					responseText = create(username, password);
				}
				else if (message.getJMSCorrelationID().equals("verifyAccount")) {
					responseText = validate(username, password);
				}
				else {
					responseText = edit(username, password, newPassword);
				}
				response.setText(responseText);
				MessageProducer tempProducer = this.session.createProducer(message.getJMSReplyTo());
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}
    		/*if (message.getJMSCorrelationID() != null && message.getJMSCorrelationID().equals("verifyAccount")) {
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String[] account = ((TextMessage)message).getText().split(" ");
				String username = account[0];
				String password = account[1];
				
				response.setText(responseText);
				MessageProducer tempProducer = this.session.createProducer(message.getJMSReplyTo());
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}*/
		} catch (JMSException e) {
			e.printStackTrace();
		}
    	
    	// Regular message handling
    	System.out.println("Message received by server");
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
    
    public String create(String username, String password) {
    	try{
    		accounts.addAccount(username, password);
    		return "created";
    	} catch (AccountException e) {
    		return e.getMessage();
    	}
    }
    
    public static void main(String[] args) {
        new ExampleServer();
        
    }
}
