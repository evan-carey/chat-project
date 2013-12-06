package edu.ucsd.cse110.server;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

//@RunWith(SpringJUnit4ClassRunner.class)
public class TestServer {
	
	@Autowired
	private Server2 server = new Server2();
	
	@Before
	public void init() {
		//Server2.main(null);
		//server = new Server2();
	}
	
	@Test
	public void testServerNotNull() {
		assertNotNull(server);
	}
	
	@Test
	public void testValidateBad() {
//		assertFalse("valid".equals(server.validate("invalidName", "testpass")));
//		assertTrue("Account does not exist.".equals(server.validate("invalidName", "testpass")));
//		assertTrue("Invalid username/password combination.".equals(server.validate("testname", "")));
	}
	
	@Test
	public void testValidateGood() {
//		("valid".equals(server.validate("testname", "testpass")));
	}

}