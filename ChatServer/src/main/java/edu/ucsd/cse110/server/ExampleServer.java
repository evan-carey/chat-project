package edu.ucsd.cse110.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	//user accounts
	private Accounts accounts;
	private static final String USER_FILE = "src/main/java/edu/ucsd/cse110/server/accounts.txt";
	
    private static int ackMode;
    //private static String messageQueueName;
    private static String messageBrokerUrl;
    private static String messageTopicName;
    private static String messageLogin;
    // server-to-client topic
    private static String produceTopicName;
    private Destination adminQueue;
    private Destination produceQueue;
    
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProtocol messageProtocol;
    
    private Map<String, Destination> loggedOn;
    static {
        messageBrokerUrl = "tcp://localhost:61616";
        //messageQueueName = "client.messages";
        messageTopicName = "client.messages";
       produceTopicName = "server.messages";
        messageLogin = "client.login";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }
 
    public ExampleServer() {
        try {
        	accounts = new Accounts();
        	loggedOn = new HashMap<String, Destination>();
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
            suscribe(session);
            
            
            // set server-to-client topic
            produceQueue = this.session.createQueue(produceTopicName);
 
            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
 
            //Set up a consumer to consume messages off of the admin queue
            MessageConsumer consumer = this.session.createConsumer(produceQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            //Handle the exception appropriately
        }
    }
    
    private void suscribe(Session session) throws JMSException{
    	adminQueue = this.session.createTopic(messageTopicName);
    }
    //private boolean signedIn = false;
    public void onMessage(Message message) {
    	Destination userDest;
    	boolean f = false;
        //System.out.println("Message received by server");
    	try {
    		
    		if (message.getJMSCorrelationID() != null && message.getJMSCorrelationID().equals("verifyAccount")) {
				TextMessage response = this.session.createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				String[] account = ((TextMessage)message).getText().split(" ");
				String username = account[0];
				String password = account[1];
				String responseText = validate(username, password);
				response.setText(responseText);
				MessageProducer tempProducer = this.session.createProducer(message.getJMSReplyTo());
				tempProducer.send(message.getJMSReplyTo(), response);
				tempProducer.close();
				return;
			}
    		
    		
    		//respond message
    		TextMessage response = this.session.createTextMessage();
    		TextMessage response2 = this.session.createTextMessage();
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                String user = txtMsg.getJMSCorrelationID();
               
                //if user is not logged on, add it to the Map of logged on users
                if(!loggedOn.containsKey(user)){
                	userDest = message.getJMSReplyTo();
                	loggedOn.put(user, userDest);
                	System.out.println("map users: " + loggedOn.size());
                }
                if(messageText.equals("[testname]: chat")){
                	
                	response.setText("connect");
                	response.setJMSReplyTo(loggedOn.get("u"));
                	//this.replyProducer.send(message.getJMSReplyTo(),response);
                	System.out.println("connecting "+ user + " to u");
                	response2.setText("suscribe");
                	response2.setJMSReplyTo(message.getJMSReplyTo());
                	f = true;
                }
                
                
                else{
                 System.out.println(messageText);
                 response.setText(this.messageProtocol.handleProtocolMessage(messageText));
                 }
            //response.setJMSCorrelationID(message.getJMSCorrelationID());
			/*if( message.getJMSPriority() == 9) {
				if (validate(((TextMessage) message).getText())) {
					this.replyProducer.send(session.createTextMessage("valid"), DeliveryMode.NON_PERSISTENT, 9, Message.DEFAULT_TIME_TO_LIVE);
					//signedIn = true;
				} else {
					this.replyProducer.send(session.createTextMessage("invalid"), DeliveryMode.NON_PERSISTENT, 9, Message.DEFAULT_TIME_TO_LIVE);
					return;
				}*/
            this.replyProducer.send(message.getJMSReplyTo(),response);
            System.out.println("server reply to"+message.getJMSReplyTo().toString()
            		+ " change suscription" + response.getJMSReplyTo());
            
             
            if(f){
            this.replyProducer.send(loggedOn.get("u"), response2);
            System.out.println("destination back to u"+loggedOn.get("u").toString()
            		+ "set suscribe to temp" + response2.getJMSReplyTo());
            }
            }
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	/*if(signedIn){ //comment this out to have regular functionality
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
    	}*/
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
