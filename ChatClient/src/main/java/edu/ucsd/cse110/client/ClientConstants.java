package edu.ucsd.cse110.client;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;

public abstract class ClientConstants {

    public static String messageBrokerUrl;
	public static String clientTopicName;
	public static String broadcastTopic;
	public static int ackMode;
	
	// server-to-client topic
	public static String consumeTopicName;
	
	
	static {
		// clientQueueName = "client.messages";
		messageBrokerUrl = "tcp://localhost:61616";
		clientTopicName = "client.messages";
		ackMode = Session.AUTO_ACKNOWLEDGE;
		consumeTopicName = "server.messages";
        broadcastTopic = "client.broadcast";
	}
	
	
}
