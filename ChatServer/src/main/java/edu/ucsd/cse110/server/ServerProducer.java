package edu.ucsd.cse110.server;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Class which handles server messages and sends them
 * to the clients.
 * @author jtbishop
 *
 */
public class ServerProducer implements Runnable {
	
	private String message;
	
	/**
	 * Set the message to be sent to clients.
	 * @param message
	 */
	public ServerProducer(String message) {
		this.message = message;
	}
	
	public void run() {
		System.out.println("Producer running");
        try {
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue(Constants.DEFAULTQUEUE);

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            // Create a messages
            //String text = "Hello world! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
            TextMessage message = session.createTextMessage(this.message);
            
            // Tell the producer to send the message
            System.out.println("Sent message: "+ message.hashCode() + " : " + Thread.currentThread().getName());
            producer.send(destination,message);

            // Clean up
            session.close();
            connection.close();
        }
        catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
	
	
	
	
	
	
	
	
}
