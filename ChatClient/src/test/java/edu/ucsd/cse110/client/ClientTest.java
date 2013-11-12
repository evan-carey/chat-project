package edu.ucsd.cse110.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClientTest {

	@Test
	public void testClientExists() {
		ExampleClient FirstClient = new ExampleClient("username");
		assertNotNull(FirstClient);

	}

	@Test
	public void testMessageSent() {
		ExampleClient SecondClient = new ExampleClient("username");
		boolean CheckSent = SecondClient.enterServer();
		assertTrue(CheckSent);
	}

}