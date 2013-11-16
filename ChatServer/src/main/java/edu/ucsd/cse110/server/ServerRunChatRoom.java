package edu.ucsd.cse110.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class ServerRunChatRoom {
    private Connection connection;
    private Session session;
    private boolean transacted = false;
	private MessageProtocol messageProtocol;
	private MessageProducer replyProducer;
	public static final String DEFAULTROOM = "CHATROOM";
	public static final String DEFAULTROOM_1 = "CHATROOM_1";
	private static Set<Destination> CHATROOMLIST;
	private static Set<String> CHATROOMSTINGLIST;
	private int numberOfRooms = 2;
	
	public ServerRunChatRoom() throws JMSException{ //Setup Default chatroom and get an initilized chatroom list
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ServerConstants.messageBrokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        //above could be commented out, or add connection.close();
        this.session = connection.createSession(this.transacted, ServerConstants.ackMode);
        this.messageProtocol = new MessageProtocol();

        
//        //operates on server commands queue
//        this.session = connection.createSession(this.transacted, ServerConstants.ackMode);
//        Destination adminQueue = this.session.createTopic(ServerConstants.messageTopicName);
//        Destination produceTopic = this.session.createTopic(ServerConstants.produceTopicName);
//        this.replyProducer = this.session.createProducer(produceTopic);
//        this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//        MessageConsumer consumer = this.session.createConsumer(adminQueue);
//        consumer.setMessageListener(this);
        
        
        Destination defaultroom = this.session.createTopic(DEFAULTROOM);
        Destination defaultroom_1 = this.session.createTopic(DEFAULTROOM_1);
		CHATROOMLIST=new HashSet();
		CHATROOMSTINGLIST=new HashSet();
		CHATROOMLIST.add(defaultroom);
		CHATROOMSTINGLIST.add(DEFAULTROOM);
		CHATROOMLIST.add(defaultroom_1);
		CHATROOMSTINGLIST.add(DEFAULTROOM_1);
	}
	
	
	public void createChatRoom(String chatroomname) throws JMSException{
        Destination addroom = this.session.createTopic(chatroomname);
        CHATROOMLIST.add(addroom);
        CHATROOMSTINGLIST.add(chatroomname);
	}
	
	
	public Set<Destination> returnChatRoomList(){
		return CHATROOMLIST;
		
	}
	
	public Set<String> returnChatRoomStringList(){
		return CHATROOMSTINGLIST;
		
	}
	
	public String[] displayChatRoomList(){
		return CHATROOMSTINGLIST.toArray(new String[0]);
		
	}
	
	public String transmitChatRoomList(){

	    	String messageText="";
			for (String i:this.displayChatRoomList())
			    messageText = messageText+i+" ";

			return messageText;
		}

	/**
	 * Accessor method that finds a room
	 * @parameter 	String: Room to search for
	 * @return 		Boolean: True if room exists, false otherwise
	 * 
	 */
	public boolean roomExists(String roomToFind) {
		for (String i:this.displayChatRoomList()){
			if(i.equals(roomToFind)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Accessor method that returns the number of rooms that exist
	 * @parameter 	None
	 * @return 		integer: The number of rooms that exist
	 * 
	 */
	public int getNumberRooms() {
		return numberOfRooms;
	}
	
	
	/**
	 * Removes the specified room
	 * @parameter 	String: room to remove
	 * @return 		true is the room was removed, false otherwise
	 * 
	 */
	public boolean removeRoom(String roomToRemove) {
		
		//Stop user from removing default rooms
		if(roomToRemove.equals("CHATROOM") || roomToRemove.equals("CHATROOM_1")){
			return false;
		}
		
		for (String i:this.displayChatRoomList()){
			if(i.equals(roomToRemove)){
				//Remove room from both lists
				CHATROOMLIST.remove(roomToRemove);
				CHATROOMSTINGLIST.remove(roomToRemove);
				
				//decrement the number of rooms
				numberOfRooms--;
				return true;
			}
		}
		return false;
	}
}


