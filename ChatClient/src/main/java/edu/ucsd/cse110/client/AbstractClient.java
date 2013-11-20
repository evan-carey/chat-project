package edu.ucsd.cse110.client;

import java.util.Random;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

public abstract class AbstractClient implements MessageListener {

	protected Connection connection;
	protected Session session;
	protected Destination producerQueue, consumerQueue, producerTopic, consumerTopic;
	protected MessageProducer producer;
	protected MessageConsumer consumer;

	protected String username, password;

	public AbstractClient() {
		// initialize connection factory
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ClientConstants.messageBrokerUrl);
		// initialize shutdown hook
		ShutdownHook.attachShutdownHook(this);
		try {
			this.connection = connectionFactory.createConnection();
			this.connection.start();
			this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			setProducer("");
			setConsumer("");
			setTopicConsumer("publicBroadcast");
		} catch(JMSException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Methods to set the producer to a given queue, or "server.messages" as a default.
	 * @param queue The name of the queue the client should produce to
	 * @throws JMSException
	 */
	public void setProducer(String queue) throws JMSException {
		String queueName = queue == null || queue.equals("") ? ClientConstants.consumeTopicName : queue;
		this.producerQueue = session.createQueue(queueName);
		this.producerTopic = session.createTopic(ClientConstants.broadcastTopic); //for the time being the producerTopic by default to server.broadcast
		this.producer = session.createProducer(producerQueue);
		this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	}
	
	public void setTopicProducer(String queue) throws JMSException {
//		String queueName = queue == null || queue.equals("") ? ClientConstants.consumeTopicName : queue;
//		this.producerQueue = session.createQueue(queueName);
		Destination producer_Topic = session.createTopic(queue); //for the time being the producerTopic by default to server.broadcast
		this.producer = session.createProducer(producer_Topic);
		this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	}
	
	public void setTopicConsumer(String queue) throws JMSException {
		Destination consumerTopic = session.createTopic(queue);
		MessageConsumer consumer_Topic = session.createConsumer(consumerTopic);
		consumer_Topic.setMessageListener(this);
	}
	
	public void setProducer(Destination dest) throws JMSException {
		if (dest instanceof Queue) {
			Destination queue = (Queue) dest;
			this.producer = session.createProducer(queue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
		if(dest instanceof Topic){
			Destination topic = (Topic) dest;
			this.producer = session.createProducer(topic);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
	}
	
	
	/**
	 * Set the consumer to a given queue, or a random queue as a default.
	 * @param queue The name of the queue the client should consume on
	 * @throws JMSException
	 */
	public void setConsumer(String queue) throws JMSException {
		String queueName = queue == null || queue.equals("") ? createRandomString() : queue;
		this.consumerQueue = session.createQueue(queueName);
		this.consumer = session.createConsumer(consumerQueue);
		this.consumer.setMessageListener(this);
	}
	public void setConsumer(Destination dest) throws JMSException {
		if (dest instanceof Queue) {
			Destination queue = (Queue) dest;
			this.consumer = session.createConsumer(queue);
			this.consumer.setMessageListener(this);
		}
		if(dest instanceof Topic){
			Destination topic = (Topic) dest;
			this.consumer = session.createConsumer(topic);
			this.consumer.setMessageListener(this);
		}
	}
	
	public String getUsername() {
		return this.username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return this.password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	private String createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}
	
	/**
	 * Method to validate input.
	 * Input may only contain alphanumeric characters and underscores
	 * @param text The string to be validated
	 * @return True if text is valid, false otherwise
	 */
	protected boolean isValidInput(String text) {
		return text.matches("^[a-zA-z0-9_]+$");
	}
	
	/**
	 * Shutdown Hook class to close connections when client logs off.
	 */
	private static class ShutdownHook {
		AbstractClient client;

		private ShutdownHook(AbstractClient client) {
			this.client = client;
		}

		public static void attachShutdownHook(final AbstractClient client) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.print("Logging off...");
					try {
						client.producer.close();
						client.session.close();
						client.connection.close();
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
