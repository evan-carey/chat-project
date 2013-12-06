package gui;

	import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Vector;
	








import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
	
	
	
	public class ChatRoom implements MessageListener {
	
		private String name;
		Vector<Client> usersIn;
		JTextArea InputBox;
		JList<String> UserList;
		JTextArea OutputBox;
		Client currentUser;
		JPanel contentPane;
		ScrollPane scroll;
		ScrollPane scroll2;
		JButton SendButton;
		JLabel lblOnlineUsers;
		JFrame frame;
		JButton btnInvite;
		JButton btnPrivateMessage;
		JButton broadcastButton;
		JButton multicastButton;
		ClientContainer clientContainer;
		MessageConsumer consumer;
		Session session;
		
		private static int  broadcastButtonFlag=0;
		private static int privateChatButton=0;
		private static int multicastButtonFlag=0;
		private String multicastTopic;
		private boolean broadcastFlag = false;
		private boolean multicastFlag = false;
		private boolean privateChat = false;
		private String privateObject;
		protected Destination producerQueue, consumerQueue, producerTopic, consumerTopic;
		private String[] clientStringArray;
		private MessageProducer chatRoomProducer;

		
		
		protected HashMap<String,MessageConsumer> topicConsumerContainer=new HashMap<String,MessageConsumer>();
		private Destination destination;
		
		public ChatRoom(){
			this.setName("Server");	
			this.usersIn = Chatwindow.clientContainer.getClients();
			this.UserList = new JList<String>(Chatwindow.clientContainer.getClientNames());	
		}
		
		public ChatRoom(Client client, String name){
			this.name = name;
			this.currentUser = client;
			session=currentUser.getSession();
			try {
				destination=session.createQueue(name);
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			multicastTopic=currentUser.getName()+".multicast";
			frame = new JFrame(client.getName() + " in " + name);	
			
			InputBox = new JTextArea();
			OutputBox = new JTextArea();
			SendButton = new JButton("Send");
			SendButton.setToolTipText("Send message.");
			lblOnlineUsers = new JLabel("Online Users");
			
			frame.setSize(new Dimension(800,350));
			
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			WindowListener exitListen = new WindowAdapter(){
				public void windowClosing(WindowEvent e){
						if(clientContainer.removeClient(currentUser)){
							dispose();
						}
						else
							System.out.println("Error exiting ChatRoom");
					};
				};
			
			frame.addWindowListener(exitListen);
			
			frame.setBounds(100, 100, 550, 350);
			contentPane = new JPanel();
			scroll = new ScrollPane();
			contentPane.setBackground(new Color(255, 255, 255));
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			frame.setContentPane(contentPane);
			contentPane.setLayout(null);
	
			InputBox.setBounds(5, 220, 316, 51);
			InputBox.setWrapStyleWord(true);
			InputBox.setBackground(new Color(255, 215, 0));
			InputBox.setLineWrap(true);
			InputBox.setWrapStyleWord(true);
			scroll2 = new ScrollPane();
			scroll2.add(InputBox);
			scroll2.setBounds(InputBox.getBounds());
			contentPane.add(scroll2);
			
			OutputBox.setEditable(false);
			OutputBox.setBackground(new Color(0, 191, 255));
			OutputBox.setLineWrap(true);
			OutputBox.setWrapStyleWord(true);
			OutputBox.setBounds(5, 0, 316, 211);
			scroll.setBounds(5, 0, 316, 211);
			scroll.add(OutputBox);
			contentPane.add(scroll);
			
	
			SendButton.setBounds(326, 248, 89, 23);
			SendButtonAction s = new SendButtonAction();
			SendButton.addActionListener(s);
			contentPane.add(SendButton);
			
			
			lblOnlineUsers.setBounds(331, 5, 89, 14);
			contentPane.add(lblOnlineUsers);
			
			
			multicastButton = new JButton("Multicast");
			multicastButton.setToolTipText("Multicast: select users");
			multicastButton.setBounds(425, 18, 89, 23);
			//InviteUser inv = new InviteUser();
			multicastButton.addActionListener(new multicastButtonListener());
			contentPane.add(multicastButton);
			
			btnPrivateMessage = new JButton("Private Message");
			btnPrivateMessage.setToolTipText("Send a private message to selected users.");
			btnPrivateMessage.setBounds(425, 46, 89, 23);
			contentPane.add(btnPrivateMessage);
			
			broadcastButton = new JButton("Broadcast");
			broadcastButton.setToolTipText("Press this button to set to broadcasting mode");
			broadcastButton.setBounds(425, 96, 89, 23);
			broadcastButton.addActionListener(new broadcastButtonListener());
			contentPane.add(broadcastButton);
			
			
			PrivateMessage p = new PrivateMessage();
			btnPrivateMessage.addActionListener(p);
	
			frame.setVisible(true);
	
	
		}
	
		public ChatRoom(Client client, String dest, ClientContainer temp) {
			// TODO Auto-generated constructor stub
			this(client, dest);
			this.currentUser = client;
			try {
				this.destination = client.getSession().createTopic(dest);
				this.consumer = client.getSession().createConsumer(destination);
				consumer.setMessageListener(this);
				chatRoomProducer = session.createProducer(destination);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.setClientContainer(temp);
			temp.addClient(client);
			UserList = new JList<String>(temp.getClientNames());
			UserList.setBackground(Color.GREEN);
			UserList.setValueIsAdjusting(true);
			UserList.setBounds(331, 21, 89, 190);
			contentPane.add(UserList);
		}
		
		public ChatRoom(Client client, ChatRoom chat, ClientContainer temp){
			this(client, chat.getName(), temp);
		}
	
		public String getName() {
			return this.name;
		}
	
		public void setName(String name) {
			this.name = name;
		}
		
		
	public class SendButtonAction implements ActionListener{
			public void actionPerformed(ActionEvent e) {
				sendMessage();
		}
	}
	
	
	public class broadcastButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (broadcastButtonFlag==0){

				broadcastButton.setLabel("Cancel Broadcast");
				broadcastButtonFlag=1;
	
				Destination destinationBroadcast = null;
				try {
					destinationBroadcast = session.createTopic("publicBroadcast");
				} catch (JMSException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				sendMessage("-b");
				sendMessage("\n");
				System.out.println(broadcastButtonFlag);
				currentUser.setProducer(destinationBroadcast);

			}
			else if(broadcastButtonFlag==1){

				broadcastButton.setLabel("Broadcast");
				broadcastButtonFlag=0;

				Destination destinationServer=null;
						try {
							destinationServer = session.createTopic("server.messages");
						} catch (JMSException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
			}
	}
}
	
	
	public class multicastButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (multicastButtonFlag==0){
			int[] indices = UserList.getSelectedIndices();
				multicastButton.setLabel("Cancel");
				multicastButtonFlag=1;
				String multicastCommand="-m";
				for (int i :indices){
					multicastCommand+=" "+clientStringArray[i];
				}
				
				
				

				try {
					
					
					TextMessage message = currentUser.getSession().createTextMessage(multicastCommand);
					message.setJMSReplyTo(currentUser.getDestination());
					message.setJMSCorrelationID(currentUser.getName());
					MessageProducer p = currentUser.getProducer();
					p.send(Chatwindow.server.getServerTopic(), message);
				} catch (JMSException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				Destination destinationMulticast = null;
				try {
					destinationMulticast = session.createTopic(currentUser.getName() + ".multicast");
				} catch (JMSException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				currentUser.setProducer(destinationMulticast);
				UserList.clearSelection();
			}
			else if(multicastButtonFlag==1){

				multicastButton.setLabel("multicast");
				multicastButtonFlag=0;

				
				if(multicastFlag) {
					currentUser.setProducer(Chatwindow.server.getServerTopic());
					MessageProducer p=currentUser.getProducer();

					try {
						TextMessage txtMessage = session.createTextMessage();
						txtMessage.setText(multicastTopic);
						txtMessage.setJMSCorrelationID("cancelmulticast");
						txtMessage.setJMSReplyTo(currentUser.getDestination());
						txtMessage.setJMSDestination(Chatwindow.server.getServerTopic());
						p.send(txtMessage);
					} catch (JMSException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					multicastFlag = false;
				}
			}
	}
}
	
	
	
	public class PrivateMessage implements ActionListener{
	
		@Override
		public void actionPerformed(ActionEvent e) {	
			if(privateChatButton==0){
				btnPrivateMessage.setLabel("Quit");
			try{		
				int[] indices = UserList.getSelectedIndices();
				int index=UserList.getSelectedIndex();
				if(indices.length > 0){
					TextMessage message = null;
					String c;
					MessageProducer p = currentUser.getProducer();
					int k = indices.length;
					boolean onlyMe = false;
					String pm = "";
					
					//for(int i = 0; i < k ; i++){
						c = clientStringArray[index];
						
						if(c.equalsIgnoreCase(currentUser.getName())){
							k--;
							onlyMe = true;
						}
						else{
							pm = "-c"+" "+c;
							message = currentUser.getSession().createTextMessage(pm);
							message.setJMSReplyTo(currentUser.getDestination());
							message.setJMSCorrelationID(currentUser.getName());
							p.send(Chatwindow.server.getServerTopic(), message);
						}
					//} 
					if(indices.length >= 0 && onlyMe){
						pm = "Send a message to someone other than youself!\n";
						sendMessage(pm);
					}
//					else{
//					message.setJMSReplyTo(currentUser.getDestination());
//					p.send(Chatwindow.server.getServerTopic(), message);
//					}
				}
				
				else
					System.out.println("Please select a user to send private message to.");
			}
			catch (Exception q){
				q.printStackTrace();
			}
			privateChatButton=1;
		}
		else if(privateChatButton==1){
			btnPrivateMessage.setLabel("Private");
			sendMessage("-d");
			privateChatButton=0;
			currentUser.setProducer(Chatwindow.server.getServerTopic());
		}
	}
	}
	
	public void setTopicConsumer(String queue) throws JMSException {
		Destination consumerTopic = session.createTopic(queue);
		MessageConsumer consumer_Topic = session.createConsumer(consumerTopic);
		consumer_Topic.setMessageListener(this);
		topicConsumerContainer.put(queue,consumer_Topic);//added by JW
	}
	
	public void removeTopicConsumer(String queue) throws JMSException {
		//if(topicConsumerContainer.containsKey(queue)) System.out.println("before deleting, there is a consumer confirmed"); // for test
		topicConsumerContainer.get(queue).close();
		topicConsumerContainer.remove(queue);
	}
	
	public void setTopicProducer(String queue) throws JMSException {
//		String queueName = queue == null || queue.equals("") ? ClientConstants.consumeTopicName : queue;
//		this.producerQueue = session.createQueue(queueName);
		Destination producer_Topic = session.createTopic(queue); //for the time being the producerTopic by default to server.broadcast
		currentUser.setProducer(session.createProducer(producer_Topic)); 
	}
	
	
	public void onMessage(Message message) {
		
		TextMessage txtMsg = (TextMessage) message;
		String messageText = null;
		try {
			messageText = txtMsg.getText();
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {

				if(message.getJMSCorrelationID()!=null && message.getJMSCorrelationID().equals("setMulticastConsumer")){
					String[] tempName=messageText.split("multicast");
					OutputBox.append("You will be multicast by "+ tempName[0]+"\n");
					setTopicConsumer(messageText);
					return;
				}
				
				
				if(message.getJMSCorrelationID()!=null && message.getJMSCorrelationID().equals("onlineuserspassed")){
					clientStringArray=messageText.split(" ");
					UserList.setListData(clientStringArray);
					return;
				}

				if (message.getJMSCorrelationID() != null && message.getJMSCorrelationID().equals("failtosetmulticast")) {
					OutputBox.append("Username Parameters not valid! Please reenter your command, this command will do nothing");
					return;
				}
				
				if (message.getJMSCorrelationID()!=null && message.getJMSCorrelationID().equals("removemulticastconsumer")){
					TextMessage temp=((TextMessage) message);
					String arg=temp.getText();
					//System.out.println("recieved "+message.getJMSCorrelationID()+" and to pass"+arg);// for test
					removeTopicConsumer(arg);
					
					String[] tempName=arg.split(".multicast");
					OutputBox.append(tempName[0]+" just cancelled multicasting to you");
					return;
				}

				System.out.println("[" + message.getJMSCorrelationID() + "]: " + messageText);

				if (messageText.equals("connect")) {
					privateChat = true;
					currentUser.setProducer(message.getJMSReplyTo());
					privateObject = message.getJMSCorrelationID();
				} else if (messageText.equals("-d")) {
					privateChat = false;
					currentUser.setProducer(Chatwindow.server.getServerTopic());
					TextMessage txtMessage = session.createTextMessage();
					txtMessage.setJMSCorrelationID(currentUser.getName());
					txtMessage.setText("-d");
					currentUser.getProducer().send(txtMessage);
					return;
				} else if (messageText.equals("setbroadcast")) {
					setTopicProducer("publicBroadcast");
					broadcastFlag = true;
				} else if (messageText.equals("setmulticast")) {
					setTopicProducer(multicastTopic);
					multicastFlag = true;
				}
		}		catch (Exception e) {
			// Handle the exception appropriately
			System.err.println("!!!!!");
		}
				
		String textDisplay = "";
		try{
			TextMessage tm = (TextMessage) message;
			textDisplay = tm.getText();
			if( OutputBox != null)
				OutputBox.append("[" + tm.getJMSCorrelationID() + "]: " +textDisplay+"\n");
		}
		catch (Exception e) {
			// Handle the exception appropriately
			System.err.println("Unable to get message");
		}
	}
	
	public Destination getDestination(){
		return this.destination;
	}
	
	public ClientContainer getClientContainer(){
		return this.clientContainer;
	}
	
	public void setClientContainer(ClientContainer c){
		this.clientContainer = c;
	}
	
	public void dispose() {
		// TODO Auto-generated method stub
		this.clientContainer.removeClient(currentUser);
		currentUser.getChatRoomContainer().removeChatRoom(this.getName());
		if(clientContainer.size() == 0){
			Chatwindow.server.getChatRoomContainer().removeChatRoom(this.getName());
		}
		try{
			String text = "User " + currentUser.getName() + " has left the ChatRoom.\n";
			sendMessage(text);
			this.frame.dispose();
		}
		catch(Exception e1){
			e1.printStackTrace();
		}
	}
	
	public boolean sendMessage(){
		try {
			String text =":"+InputBox.getText()+"\n" ;
			
//			MessageProducer p = currentUser.getProducer();
			TextMessage message = currentUser.getSession().createTextMessage(text);
			message.setJMSReplyTo(destination);
			message.setJMSCorrelationID(currentUser.getName());
			chatRoomProducer.send(destination,message);//Chatwindow.server.getServerTopic(),
			InputBox.setText("");
			//OutputBox.append(currentUser.getName()+":"+text);
			return true;
			
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	
		public boolean sendMessage(String text){
			try {	
				MessageProducer p = currentUser.getProducer();
				TextMessage message = currentUser.getSession().createTextMessage(text);
				message.setJMSReplyTo(getDestination());
				p.send(message);
				return true;
				
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}

		public boolean sendMessage(String JMScorrelationID, String text){
			try {	
				MessageProducer p = currentUser.getProducer();
				TextMessage message = currentUser.getSession().createTextMessage(text);
				message.setJMSCorrelationID(JMScorrelationID);
				//message.setJMSReplyTo(getDestination());
				p.send(message);
				return true;
				
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}

		public boolean sendMessage(String string, Destination destination) {
			// TODO Auto-generated method stub
			try {	
				MessageProducer p = currentUser.getProducer();
				TextMessage message = currentUser.getSession().createTextMessage(string);
				message.setJMSReplyTo(destination);
				p.send(destination, message);
				return true;
				
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}
		
//		public class InviteUser implements ActionListener{
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				// TODO Auto-generated method stub
//				String text = JOptionPane.showInputDialog(null, "Enter the name of the user you wish to invite.\n");
//
//				if(Chatwindow.clientContainer.contains(text)){
//					if(text.equals(currentUser.getName())){
//						JOptionPane.showMessageDialog(null, "Invite someone other than yourself!\n");
//						return;
//					}
//					else{
//						Client c = Chatwindow.clientContainer.getClient(text);
//						String t = c.joinRoom(currentUser, Chatwindow.server.getChatRoomContainer().getChatRoom(getName()));
//						if(!t.equals("")){
//							JOptionPane.showMessageDialog(null, t);
//						}
//					}
//				}
//				else
//					JOptionPane.showMessageDialog(null, "Please enter a valid username.\n");
//		}
//	}
}
