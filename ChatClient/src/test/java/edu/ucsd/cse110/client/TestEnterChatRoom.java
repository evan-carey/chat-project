package edu.ucsd.cse110.client;

import static org.junit.Assert.*;
import org.junit.Test;
import javax.jms.JMSException;

import org.junit.Test;

public class TestEnterChatRoom {

	
	
	@Test
	public void testHelpScreen() throws JMSException {
		try{
			EnterChatRoom roomToTest = new EnterChatRoom("testname");
			boolean helpScreen = roomToTest.listChatroomCommands();
			assertTrue(helpScreen);
		}catch(JMSException e){
			System.err.println(e.getMessage());
		}
	}
	
	@Test
	public void testlogoffInRoom() throws JMSException {
		EnterChatRoom roomToTest = new EnterChatRoom("testname");
		boolean logoffError = roomToTest.logoff();
		assertTrue(logoffError);
	}
	
	
	@Test
	public void testInChatRoom() throws JMSException {
		EnterChatRoom roomToTest = new EnterChatRoom("testname");
	}
	

}
