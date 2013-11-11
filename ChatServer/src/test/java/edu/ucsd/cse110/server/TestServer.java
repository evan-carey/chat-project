package edu.ucsd.cse110.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TestServer {
	
	private ExampleServer serverToTest;
	
	@Before
	public void init() {
		serverToTest = new ExampleServer();
	}
	
	@Test
	public void testServerNotNull() {
		assertNotNull(serverToTest);
	}
	
	@Test
	public void testValidateBad() {
		assertFalse("valid".equals(serverToTest.validate("invalidName", "testpass")));
		assertTrue("Account does not exist.".equals(serverToTest.validate("invalidName", "testpass")));
		assertTrue("Invalid username/password combination.".equals(serverToTest.validate("testname", "")));
	}
	
	@Test
	public void testValidateGood() {
		assertTrue("valid".equals(serverToTest.validate("testname", "testpass")));
	}

}