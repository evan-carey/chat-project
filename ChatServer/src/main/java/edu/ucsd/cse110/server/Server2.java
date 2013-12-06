package edu.ucsd.cse110.server;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

import edu.ucsd.cse110.server.accountinfo.AccountException;
import edu.ucsd.cse110.server.accountinfo.Accounts;

@Configuration
@ComponentScan
public class Server2 {

	private static JmsTemplate jmsTemplate;

	private ServerRunChatRoom serverrunchatroom;
	private Accounts accounts;
	private Map<String, Destination> loggedOn; // online users
	
	private HashMap<String,String[]> multicastContainer=new HashMap<String,String[]>();
	private Set<String> privateChatContainer=new HashSet<String>();
	private boolean multicastFlag = false;

	public Server2() {
		accounts = new Accounts();
		loggedOn = new HashMap<String, Destination>();
		ShutdownHook.attachShutdownHook(this);
		try {
			serverrunchatroom = new ServerRunChatRoom();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	

	public String getStringLoggedOn(){
		String[] stringArray=loggedOn.keySet().toArray(new String[0]);
		String returnString="";
		for(String i: stringArray){
			returnString+=i+" ";
		}
		return returnString;
	}
	/**
	 * Parses a user's command and calls the appropriate method.
	 * @param msg The Message object that contains the user's command.
	 * @throws JMSException
	 */
	private void handleDashCommand(Message msg) throws JMSException {
		String text = ((TextMessage) msg).getText();
		Destination replyTo = msg.getJMSReplyTo();

		if (text.length() < 2)
			return;
		switch (text.charAt(1)) {
		case 'g':
			sendMessage(replyTo, reportOnlineUsers());
			break;
		case 'c':
			setChat(msg);
			break;
		case 'b':
			setBroadcast(replyTo);
			break;
		case 'm':
			setMulticast(msg);
			break;
		case 'd':
			privateChatContainer.remove(msg.getJMSCorrelationID());
			break;
		default:
			break;
		}
	}
	
	public MessageCreator reportOnlineUsers() throws JMSException {
		if (loggedOn.isEmpty()) {
			// no users online
			return new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message = session.createTextMessage();
					message.setText("No users found");
					return message;
				}
			};
		}
		
		// create user list
		MessageCreator mc1 = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				String users = "";
				for (String s : loggedOn.keySet())
					users += "\n\t" + s;
				TextMessage message1 = session.createTextMessage();
				message1.setText(users);
				message1.setJMSCorrelationID("Online Users");
				//message1.setJMSReplyTo(loggedOn.get(user2));
				return message1;
			}
		};
		// return list
		return mc1;
	}
	
	private boolean setChat(final Message message) throws JMSException {
		String[] msg = ((TextMessage) message).getText().split(" ");
		if (msg.length < 2) return false;
		final String user2 = msg[1];
		
		// private chat errors
		if (!loggedOn.containsKey(user2)) {
			sendMessage(message.getJMSReplyTo(), "User " + user2 + " is not logged on.");
			return false;
		}
		if (privateChatContainer.contains(message.getJMSCorrelationID())) {
			sendMessage(message.getJMSReplyTo(), "You are already in a private chat. Please disconnect first.");
			return false;
		}
		if (privateChatContainer.contains(user2)) {
			sendMessage(message.getJMSReplyTo(), "User " + user2 + " is already in a private chat.");
			return false;
		}
		
		// start chat
		System.out.println("Initialized chat session between: " + message.getJMSCorrelationID() + " and " + user2);
		privateChatContainer.add(message.getJMSCorrelationID());
		privateChatContainer.add(user2);
		
		// send response to user1
		MessageCreator mc1 = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage message1 = session.createTextMessage();
				message1.setText("connect");
				message1.setJMSCorrelationID(user2);
				message1.setJMSReplyTo(loggedOn.get(user2));
				return message1;
			}
		};
		sendMessage(message.getJMSReplyTo(), mc1);
		
		// send responses to user2
		MessageCreator mc2 = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage message2 = session.createTextMessage();
				message2.setText("connect");
				message2.setJMSCorrelationID(message.getJMSCorrelationID());
				message2.setJMSReplyTo(message.getJMSReplyTo());
				return message2;
			}
		};
		sendMessage(loggedOn.get(user2), mc2);
		return true;
	}
	
	private void setBroadcast(Destination dest) {
		sendMessage(dest, "setbroadcast");
	}
	
	private boolean setMulticast(Message tmp) throws JMSException {
		Map<String, Destination> recipients = new HashMap<String, Destination>();
		String[] users = ((TextMessage) tmp).getText().split(" ");
		for (int i = 1; i < users.length; ++i) {
			if (loggedOn.containsKey(users[i])) {
				recipients.put(users[i], loggedOn.get(users[i]));
			} else {
				// invalid user specified in multicast
				sendMessage(tmp.getJMSReplyTo(), "failtosetmulticast");
				return false;
			}
		}
		// reply to sender
		sendMessage(tmp.getJMSReplyTo(), "setmulticast");
		final String multicastTopic = tmp.getJMSCorrelationID() + ".multicast";
		
		// reply to recipients
		Iterator<Entry<String, Destination>> it = recipients.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Destination> recipient = it.next();
			System.out.println("multicast destinations: " + recipient.getValue());/////////
			MessageCreator mc = new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message1 = session.createTextMessage();
					message1.setText(multicastTopic);
					message1.setJMSCorrelationID("setMulticastConsumer");
					System.out.println("set multicast command");/////////
					return message1;
				}
			};
			sendMessage(recipient.getValue(), mc);
		}
		
		multicastContainer.put(multicastTopic,users);
		multicastFlag=true;
		return true;
	}
	
	private boolean scanID(Message message) throws JMSException {
		if(message.getJMSCorrelationID()==null){
			return false;
		}
		String msgID = message.getJMSCorrelationID().toLowerCase();
		String response = null;
		switch(msgID) {
		case "createaccount":
			response = createAccount(message);
			break;
		case "verifyaccount":
			response = verifyAccount(message);
			break;
		case "editaccount":
			response = editAccount(message);
			break;
		case "listchatrooms":;
			response = listChatRooms(message);
			break;
		case "logoff":
			removeUser(message);
			break;
		case "createchatroom":
			response = createChatRoom(message);
			break;
		case "listchatroomusers":
			response = listChatRoomUsers(message);
			break;
		case "chatroomlogin":
			chatRoomLogIn(message);
			break;
		case "chatroomlogout":
			chatRoomLogOut(message);
			break;
		case "cancelmulticast":
			cancelMulticast(message);
			break;
		default:
			return false;
		}
		

		Destination replyTo = message.getJMSReplyTo();
		
	
		if (response != null) sendMessage(replyTo, response);
		return true;
	}

	private String createAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 2) return null;
		
		try {
			accounts.addAccount(info[0], info[1]);
		} catch (AccountException e) {
			return e.getMessage();
		}
		return "created";
	}
	
	private String verifyAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 2) return null;
		if (loggedOn.containsKey(info[0])) {
			return "Error: User " + info[0] + " is already logged on";
		}
		
		try {
			if (info[1].equals(accounts.getPassword(info[0])))
				return "valid";
			else
				return "Invalid username/password combination.";
		} catch (AccountException e) {
			return e.getMessage();
		}
	}
	
	private String editAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 3) return null;
		
		try {
			accounts.setPassword(info[0], info[1], info[2]);
			return "edited";
		} catch (AccountException e) {
			return e.getMessage();
		}
	}
	
	private String listChatRooms(Message msg) throws JMSException {
		return serverrunchatroom.transmitChatRoomList();
	}
	
	private void removeUser(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		loggedOn.remove(info[0]);
		
		
		MessageCreator mc_onlineuser = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage message1 = session.createTextMessage();
				message1.setText(getStringLoggedOn());
				message1.setJMSCorrelationID("onlineuserspassed");
				//message1.setJMSDestination();
				return message1;
			}
		};
		
		Collection<Destination> loggedOnDestinations=loggedOn.values();
		
		for (Destination i:loggedOnDestinations){
			sendMessage(i,mc_onlineuser);
		}

		
	
	}
	
	private String createChatRoom(Message msg) throws JMSException {
		String[] command = ((TextMessage) msg).getText().split(" ");
		if (command.length < 2) return null;
		
		if (serverrunchatroom.roomExists(command[1])) {
			return "Room already exists";
		}
		serverrunchatroom.createChatRoom(command[1]);
		return "Created new chatroom \"" + command[1] + "\"";
	}
	
	private String listChatRoomUsers(Message msg) throws JMSException {
		String[] args = ((TextMessage) msg).getText().split(" ");
		if (args.length < 2) return null;
		return serverrunchatroom.transmitChatRoomUserList(args[1]);
	}
	
	private void chatRoomLogIn(Message msg) throws JMSException {
		String[] args = ((TextMessage) msg).getText().split(" ");
		if (args.length < 2) return;
		serverrunchatroom.addUser(args[0], args[1]);
	}
	
	private void chatRoomLogOut(Message msg) throws JMSException {
		String[] args = ((TextMessage) msg).getText().split(" ");
		if (args.length < 2) return;
		serverrunchatroom.removeUser(args[0], args[1]);
	}
	
	private void cancelMulticast(Message msg) throws JMSException {
		final String tempTopicName = ((TextMessage) msg).getText();
		for (String i : multicastContainer.get(tempTopicName)) {
			Destination multidest = loggedOn.get(i);
			MessageCreator mc = new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message1 = session.createTextMessage();
					message1.setText(tempTopicName);
					message1.setJMSCorrelationID("removemulticastconsumer");
					return message1;
				}
			};
			sendMessage(multidest, mc);
		}
		multicastContainer.remove(tempTopicName);
	}	
	
	public void onMessage(final TextMessage msg) throws JMSException {
		final String text = ((TextMessage) msg).getText();
		if (text.length() <= 0) return;
		
		// check for dash commands
		if (text.charAt(0) == '-')
			handleDashCommand(msg);
		
		// check for other commands
		if (scanID(msg))
			return;
		
		// check if message is a multicast command
		if (multicastFlag) {
			multicastFlag = false;
			return;
		}
		
		// check if user is not logged on and add them
		String user = msg.getJMSCorrelationID();
		Destination dest = msg.getJMSReplyTo();
		if (msg.getJMSCorrelationID()!=null && !loggedOn.containsKey(user)) {
			loggedOn.put(user, dest);
			
			MessageCreator mc_onlineuser = new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message1 = session.createTextMessage();
					message1.setText(getStringLoggedOn());
					message1.setJMSCorrelationID("onlineuserspassed");
					//message1.setJMSDestination();
					return message1;
				}
			};
			
			Collection<Destination> loggedOnDestinations=loggedOn.values();
			
			for (Destination i:loggedOnDestinations){
				sendMessage(i,mc_onlineuser);
			}
			
			MessageCreator mc = new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message1 = session.createTextMessage();
					message1.setText(msg.getJMSCorrelationID());
					message1.setJMSCorrelationID("---Welcome---");
					return message1;
				}
			};
			sendMessage(msg.getJMSReplyTo(), mc);
		}
		
		// Regular message handling
		/*MessageCreator mc = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage message1 = session.createTextMessage();
				message1.setText(text);
				message1.setJMSCorrelationID(msg.getJMSCorrelationID());
				return message1;
			}
		}; 
		try{
		
			jmsTemplate.send(msg.getJMSReplyTo(), mc);
	
		}catch(UnsupportedOperationException e){
			return;
		}*/
		System.out.println(msg.getJMSCorrelationID()+" >> " + text);
//		TextMessage tm = resourceHolder.getSession().createTextMessage();
//		tm.setJMSCorrelationID(msg.getJMSCorrelationID());
//		tm.setText(text);
//		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), tm);
		
	}
	
	private void sendMessage(Destination dest, String msg) {
		if (dest == null || msg == null) return;
		try {
			jmsTemplate.convertAndSend(dest, msg);
		} catch (JmsException e) {
			return;
		} catch (UnsupportedOperationException e) {
			return;
		}
	}
	
	private void sendMessage(Destination dest, Message msg) {
		if (dest == null || msg == null) return;
		try {
			jmsTemplate.convertAndSend(dest, msg);
		} catch (JmsException e) {
			return;
		} catch (UnsupportedOperationException e) {
			return;
		}
	}
	
	private void sendMessage(Destination dest, MessageCreator mc) {
		if (dest == null || mc == null) return;
		try {
			jmsTemplate.send(dest, mc);
		} catch (JmsException e) {
			return;
		} catch (UnsupportedOperationException e) {
			return;
		}
	}

	@Bean
	ConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(new ActiveMQConnectionFactory(
				ServerConstants.messageBrokerUrl));
	}

	@Bean
	MessageListenerAdapter receiver() {
		return new MessageListenerAdapter(new Server2()) {
			{
				setMessageConverter(null);
				setDefaultListenerMethod("onMessage");
			}
		};
	}

	@Bean
	SimpleMessageListenerContainer container(
			final MessageListenerAdapter messageListener,
			final ConnectionFactory connectionFactory) {
		return new SimpleMessageListenerContainer() {
			{
				setMessageListener(messageListener);
				setConnectionFactory(connectionFactory);
				setDestinationName(ServerConstants.produceTopicName);
			}
		};
	}

	@Bean
	JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	/**
	 * Shutdown Hook class to handle saving account information when program
	 * terminates.
	 */
	private static class ShutdownHook {
		Server2 server;

		private ShutdownHook(Server2 server) {
			this.server = server;
		}

		public static void attachShutdownHook(final Server2 server2) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.println("Saving account information to file...");
					server2.accounts.writeToFile();
					System.out.println("Done!");
				}
			});
		}
	}

	public static void main(String[] args) {
		try {
			BrokerService broker = new BrokerService();
			broker.addConnector(ServerConstants.messageBrokerUrl);
			broker.setPersistent(false);
			broker.start();
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Server2.class);

			jmsTemplate = context.getBean(JmsTemplate.class);

			// Gracefully terminate server by hitting ENTER
			System.out.println("Server initialized. To terminate, press ENTER in the console.");
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			context.close();
			System.out.println("Terminating server.");
			System.exit(0);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Fatal error, aborting...");
		}

	}

}
