package edu.ucsd.cse110.client;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.Message;

public class Consumer {

	private Connection connection;
	private Session session;
	private Destination destination;

	public Consumer(Connection connection, Session session,
			Destination destination) {
		try {
			this.connection = connection;
			this.session = session;
			this.destination = destination;
			// MessageConsumer is used for actually receiving the Message
			MessageConsumer consumer = session.createConsumer(destination);

			// Here is what makes receiving the message actually possible
			Message message = (Message) consumer.receive();

			// There are many types of Message and TextMessage
			// is just one of them. Producer sent us a TextMessage
			// so we must cast to it to get access to its .getText()
			// method.
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				System.out.println("Message that was Received: "
						+ textMessage.getText() + " ");
			}
			connection.close();
		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}
	}


}
