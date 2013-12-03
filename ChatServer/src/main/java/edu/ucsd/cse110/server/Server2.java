package edu.ucsd.cse110.server;

/**
 * An attempt refactor the Server class and get Spring playing 
 * nice with it. Doesn't work yet.
 * @author Evan Carey
 */
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
	//private static JmsResourceHolder resourceHolder;
	private MessageProducer producer;

	private Destination adminQueue, adminQueue_2;

	private ServerRunChatRoom serverrunchatroom;
	private Accounts accounts;
	private Map<String, Destination> loggedOn; // map of online users and their
												// Destinations
	
	private HashMap<String,String[]> multicastContainer=new HashMap<String,String[]>();
	private Set<String> privateChatContainer=new HashSet<String>();
	private boolean multicastFlag = false;

	public Server2() {
		accounts = new Accounts();
		loggedOn = new HashMap<String, Destination>();
		ShutdownHook.attachShutdownHook(this);
		//resourceHolder.addSession(new Session());
		try {
			serverrunchatroom = new ServerRunChatRoom();
			setupMessageQueueConsumer();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void setupMessageQueueConsumer() throws JMSException {
		//adminQueue = resourceHolder.getSession().createQueue(ServerConstants.produceTopicName);
		//adminQueue_2 = resourceHolder.getSession().createQueue("templistchatroomqueue");
		// producer = jmsTemplate.createProducer(resourceHolder.getSession(),adminQueue);
	}
	
	private void handleDashCommand(Message tmp) throws JMSException {
		String text = ((TextMessage) tmp).getText();

		if (text.length() < 2)
			return;
		switch (text.charAt(1)) {
		case 'g':
			reportOnlineUsers(tmp.getJMSReplyTo());
			break;
		case 'c':
			setChat(tmp);
			break;
		case 'b':
			setBroadcast(tmp.getJMSReplyTo());
			break;
		case 'm':
			setMulticast(tmp);
			break;
		default:
			break;
		}
	}
	
	private void reportOnlineUsers(Destination dest) throws JMSException {
		if (loggedOn.isEmpty()) {
			jmsTemplate.convertAndSend(dest, "No users found");
			return;
		}
		String users = "\nOnline Users: \n";
		for (String s : loggedOn.keySet())
			users += "    " + s + "\n";

		System.out.println(users);
		jmsTemplate.convertAndSend(dest, users);
	}
	
	private void setChat(final Message message) throws JMSException {
		String[] msg = ((TextMessage) message).getText().split(" ");
		if (msg.length < 2) return;
		final String user2 = msg[1];
		
		// private chat errors
		if (!loggedOn.containsKey(user2)) {
			jmsTemplate.convertAndSend(message.getJMSReplyTo(), "User " + user2 + " is not logged on.");
			return;
		}
		if (privateChatContainer.contains(message.getJMSCorrelationID())) {
			jmsTemplate.convertAndSend(message.getJMSReplyTo(), "You are already in a private chat. Please disconnect first.");
			return;
		}
		if (privateChatContainer.contains(user2)) {
			jmsTemplate.convertAndSend(message.getJMSReplyTo(), "User " + user2 + " is already in a private chat.");
			return;
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
		jmsTemplate.send(message.getJMSReplyTo(), mc1);
		
		// set new Destination for user1 and send response
//		TextMessage tm1 = resourceHolder.getSession().createTextMessage();
//		tm1.setJMSCorrelationID(user2);
//		tm1.setText("connect");
//		tm1.setJMSReplyTo(loggedOn.get(user2));
//		jmsTemplate.convertAndSend(message.getJMSReplyTo(), tm1);
		
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
		jmsTemplate.send(loggedOn.get(user2), mc2);
		
		// set new Destination for user2 and send response
//		TextMessage tm2 = resourceHolder.getSession().createTextMessage();
//		tm2.setJMSCorrelationID(message.getJMSCorrelationID());
//		tm2.setText("connect");
//		tm2.setJMSReplyTo(message.getJMSReplyTo());
//		jmsTemplate.convertAndSend(loggedOn.get(user2), tm2);
	}
	
	private void setBroadcast(Destination dest) {
		jmsTemplate.convertAndSend(dest, "setbroadcast");
	}
	
	private void setMulticast(Message tmp) throws JMSException {
		Map<String, Destination> recipients = new HashMap<String, Destination>();
		String[] users = ((TextMessage) tmp).getText().split(" ");
		for (int i = 1; i < users.length; ++i) {
			if (loggedOn.containsKey(users[i])) {
				recipients.put(users[i], loggedOn.get(users));
			} else {
				// invalid user specified in multicast
				jmsTemplate.convertAndSend(tmp.getJMSReplyTo(), "failtosetmulticast");
				return;
			}
		}
		// reply to sender
		jmsTemplate.convertAndSend(tmp.getJMSReplyTo(), "setmulticast");
		final String multicastTopic = tmp.getJMSCorrelationID() + ".multicast";
		
		// reply to recipients
		Iterator<Entry<String, Destination>> it = recipients.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, Destination> recipient = it.next();
			MessageCreator mc = new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					TextMessage message1 = session.createTextMessage();
					message1.setText(multicastTopic);
					message1.setJMSCorrelationID("setMulticastConsumer");
					return message1;
				}
			};
			jmsTemplate.send(recipient.getValue(), mc);
			
//			TextMessage tm = resourceHolder.getSession().createTextMessage();
//			tm.setJMSCorrelationID("setMulticastConsumer");
//			tm.setText(multicastTopic);
//			jmsTemplate.convertAndSend(recipient.getValue(), tm);
		}
		
		multicastContainer.put(multicastTopic,users);
		multicastFlag=true;
	}
	
	private boolean scanID(Message message) throws JMSException {
		String msgID = message.getJMSCorrelationID().toLowerCase();
		switch(msgID) {
		case "createaccount":
			createAccount(message);
			return true;
		case "verifyaccount":
			verifyAccount(message);
			return true;
		case "editaccount":
			editAccount(message);
			return true;
		case "listchatrooms":
			listChatRooms(message);
			return true;
		case "logoff":
			removeUser(message);
			return true;
		case "createchatroom":
			createChatRoom(message);
			return true;
		case "listchatroomusers":
			listChatRoomUsers(message);
			return true;
		case "chatroomlogin":
			chatRoomLogIn(message);
			return true;
		case "chatroomlogout":
			chatRoomLogOut(message);
			return true;
		case "cancelmulticast":
			cancelMulticast(message);
			return true;
		default:
			return false;
		}
	}

	private void createAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 2) return;
		
		try {
			accounts.addAccount(info[0], info[1]);
		} catch(AccountException e) {
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), e.getMessage());
		}
		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "created");
	}
	
	private void verifyAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 2) return;
		if (loggedOn.containsKey(info[0])) {
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "Error: User " + info[0] + " is already logged on");
			return;
		}
		
		try {
			if (info[1].equals(accounts.getPassword(info[0])))
				jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "valid");
			else
				jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "Invalid username/password combination.");
		} catch (AccountException e) {
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), e.getMessage());
		}
	}
	
	private void editAccount(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		if (info.length < 3) return;
		
		try {
			accounts.setPassword(info[0], info[1], info[2]);
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "edited");
		} catch (AccountException e) {
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), e.getMessage());
		}
	}
	
	private void listChatRooms(Message msg) throws JMSException {
		String responseText = serverrunchatroom.transmitChatRoomList();
		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), responseText);
	}
	
	private void removeUser(Message msg) throws JMSException {
		String[] info = ((TextMessage) msg).getText().split(" ");
		loggedOn.remove(info[0]);
	}
	
	private void createChatRoom(Message msg) throws JMSException {
		String[] command = ((TextMessage) msg).getText().split(" ");
		if (command.length < 2) return;
		
		if (serverrunchatroom.roomExists(command[1])) {
			// creation unsuccessful
			jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "Room already exists.");
			return;
		}
		serverrunchatroom.createChatRoom(command[1]);
		// creation successful
		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), "Created new chatroom \"" + command[1] + "\"");
		
		//jmsTemplate.convertAndSend(msg.getJMSReplyTo(), serverrunchatroom.transmitChatRoomList()); 
	}
	
	private void listChatRoomUsers(Message msg) throws JMSException {
		String[] args = ((TextMessage) msg).getText().split(" ");
		if (args.length < 2) return;
		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), serverrunchatroom.transmitChatRoomUserList(args[1]));
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
			jmsTemplate.send(multidest, mc);
//			TextMessage tm = resourceHolder.getSession().createTextMessage();
//			tm.setJMSCorrelationID("removemulticastconsumer");
//			tm.setText(tempTopicName);
//			jmsTemplate.convertAndSend(multidest, tm);
		}
		multicastContainer.remove(tempTopicName);
	}	
	
	public void receive(Message msg) throws JMSException {
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
		if (!loggedOn.containsKey(user)) {
			loggedOn.put(user, dest);
		}
		
		// Regular message handling
		MessageCreator mc = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage message1 = session.createTextMessage();
				message1.setText(text);
				message1.setJMSCorrelationID("removemulticastconsumer");
				return message1;
			}
		};
		jmsTemplate.send(msg.getJMSReplyTo(), mc);
		
		System.out.println(msg.getJMSCorrelationID()+" >> " + text);
//		TextMessage tm = resourceHolder.getSession().createTextMessage();
//		tm.setJMSCorrelationID(msg.getJMSCorrelationID());
//		tm.setText(text);
//		jmsTemplate.convertAndSend(msg.getJMSReplyTo(), tm);
		
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
				setDefaultListenerMethod("receive");
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
			//resourceHolder = context.getBean(JmsResourceHolder.class);

//			MessageCreator messageCreator = new MessageCreator() {
//				public Message createMessage(Session session)
//						throws JMSException {
//					return session.createTextMessage("Server Initialized!");
//				}
//			};
			System.out.println("Sending a new message:");
			//jmsTemplate.send(ServerConstants.produceTopicName, messageCreator);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Fatal error, aborting...");
		}

	}

}
