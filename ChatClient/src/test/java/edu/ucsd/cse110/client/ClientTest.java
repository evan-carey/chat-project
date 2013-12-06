package edu.ucsd.cse110.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClientTest {
	

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	
	@Before
	public void setUpStreams(){
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}
	
	@After
	public void cleanUpStreams(){
		System.setOut(null);
		System.setErr(null);
	}

	@Test
	public void testClientExists() throws JMSException {
		Client FirstClient = new Client("username");
		assertNotNull(FirstClient);

	}

	@Test
	public void testEditAccount() throws JMSException {
		try{
		Client SecondClient = new Client("username");
		TextMessage txtMessage = new DummyTextMessage();
		txtMessage.setText("edited");
		SecondClient.onMessage(txtMessage);
		} catch (NullPointerException e){
			assertEquals("Account edited. " + "\n", outContent.toString());
		}
	}
	
	@Test
	public void testSetMultiCast() throws JMSException {
		try{
		Client SecondClient = new Client("username");
		TextMessage txtMessage = new DummyTextMessage();
		txtMessage.setJMSCorrelationID("setMulticastConsumer");
		txtMessage.setText("username.multicast");
		SecondClient.onMessage(txtMessage);
		} catch (NullPointerException e){
			assertEquals("You will be multicast by username." + "\n", outContent.toString());
		}
	}
	
	@Test
	public void testFailtoSetMultiCast() throws JMSException {
		Client SecondClient = new Client("username");
		TextMessage txtMessage = new DummyTextMessage();
		txtMessage.setJMSCorrelationID("failtosetmulticast");
		SecondClient.onMessage(txtMessage);
	}
	
	
	@Test
	public void testListServerCommands() throws JMSException {
		Client.listServerCommands();
		assertEquals("**********SERVER COMMANDS************" + "\n" + "-c [username]"
				+ "\n" + "   Establishes a private chat between current userand designated username."
				+ "\n" + "-d" + "\n" + "   Disconnects both peers from private chat"
				+ "\n" + "-g" + "\n" + "   Returns all online users." + "\n" + "-b" + "\n" + "   Sets broadcast mode."
				+ "\n" + "cancel broadcast" + "\n" + "   Cancels broadcast mode and returns user to server."
				+ "\n" + "-m [username1] [username2] ..." + "\n" + "   Sets multicast mode."
				+ "\n" + "cancel multicast" + "\n" + "   Cancels multicast mode and returns user to server."
				+ "\n" + "whereami" + "\n" + "   Displays the users current status." + "\n" + "c:enterroom"
				+ "\n" + "   Allows the user to enter existing chatroom." + "\n" + "c:listrooms"
				+ "\n" + "   Lists all the existing chatrooms." + "\n" + "c:create"
				+ "\n" + "   Allows the user to create a room, provided it doesn't alreay exist"
				+ "\n" + "editAccount" + "\n" + "   Allows the user to edit his/her account."
				+ "\n" + "logoff" + "\n" + "  Logs the user off, saving any/all changes made to their account."
				+ "\n" + "**************************" + "\n", outContent.toString());
	}
	
	

}