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
		boolean validate = serverToTest.validate("testpass");
		assertFalse(validate);
	}
	
	@Test
	public void testValidateGood() {
		boolean validate = serverToTest.validate("testname testpass");
		assertTrue(validate);
	}

}