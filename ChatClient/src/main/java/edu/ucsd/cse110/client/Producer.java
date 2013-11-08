package edu.ucsd.cse110.client;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;


public class Producer {
	
	private Connection connection;
	private Session session;
	private Destination destination;
	
	public Producer (Connection connection, Session session, Destination destination, String stringMessage) {
		this.connection = connection;
		this.session = session;
		this.destination = destination;
		
		try {
			//Use MessageProducer to send messages
			MessageProducer producer = session.createProducer(destination);
			
			//Create a message object
			TextMessage message = session.createTextMessage(stringMessage);
			
			//Actually send the message to the defaultQueue
			producer.send(message);
			System.out.println("The following should be sent to the default Queue: " +
					message.getText() + "  ");
			
			//close the connection
			connection.close();
			
		} catch(JMSException exp) {
				System.err.println(exp.getMessage());
		}
		
	}

	
}
