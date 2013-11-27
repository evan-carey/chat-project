package edu.ucsd.cse110.client;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;

public class ConnectingClientTest {

	ConnectingClient client;
	
	@Before
	public void init() {
		client = new ConnectingClient();
	}
	
	@Test
	public void testGetResponse() {
		ByteArrayInputStream in = new ByteArrayInputStream("My string".getBytes());
		System.setIn(in);
		client.getResponse();
		assertNotNull(client.returnResponse());
		assertEquals(client.returnResponse(), "My string");
		System.setIn(System.in);
	}
	
	@Test
	public void testConnectingClient() {
		fail("Not yet implemented");
	}

	@Test
	public void testOnMessage() {
		fail("Not yet implemented");
	}

}
