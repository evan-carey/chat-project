package edu.ucsd.cse110.server;

import static org.junit.Assert.*;

import javax.jms.JMSException;

import org.junit.Before;
import org.junit.Test;

public class TestServerRunChatRoomJunit {
	ServerRunChatRoom testchatroom;
	@Before
	public void init() throws JMSException{
		testchatroom=new ServerRunChatRoom();
	}
	
	@Test
	public void test() throws JMSException {

		String[] test;
		test=testchatroom.displayChatRoomList();
		//System.out.println("display");
		System.out.println(test[0]);
		assertEquals("CHATROOM_1",test[0]);
	}
	
	@Test
	public void test2() throws JMSException {
		String[] test;
		testchatroom.createChatRoom("New_Chatroom");
		test=testchatroom.displayChatRoomList();
		System.out.println(test[0]);
		System.out.println(test[1]);
		//assertEquals("CHATROOM",test[0]);
		//assertEquals("New_Chatroom",test[1]);
		System.out.println(testchatroom.transmitChatRoomList());
	}

}
