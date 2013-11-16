package edu.ucsd.cse110.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClientTest {

	@Test
	public void testClientExists() {
		Client FirstClient = new Client("username");
		assertNotNull(FirstClient);

	}

	@Test
	public void testMessageSent() {
		Client SecondClient = new Client("username");
//		boolean CheckSent = SecondClient.enterServer();
//		assertTrue(CheckSent);
	}

}