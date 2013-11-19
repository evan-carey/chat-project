package edu.ucsd.cse110.server;

import javax.jms.Session;

public abstract class ServerConstants {

    public static int ackMode;
    
    //private static String messageQueueName;
    public static String messageBrokerUrl;
    public static String messageTopicName;
    public static String publicBroadcast;
    // server-to-client topic
    public static String produceTopicName;
	
	
    //create all the constants necessary to 
    //setup Message Queue Consumer
    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageTopicName = "client.messages";
        produceTopicName = "server.messages";
        publicBroadcast = "server.broadcast";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }
    
    
	
	
	
	
}
