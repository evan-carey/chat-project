package edu.ucsd.cse110.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jms.JMSException;

import org.junit.Test;

public class ClientTest {

	@Test
	public void testClientExists() throws JMSException {
		Client FirstClient = new Client("username");
		assertNotNull(FirstClient);

	}

	@Test
	public void testMessageSent() throws JMSException {
		Client SecondClient = new Client("username");
//		boolean CheckSent = SecondClient.enterServer();
//		assertTrue(CheckSent);
	}

}