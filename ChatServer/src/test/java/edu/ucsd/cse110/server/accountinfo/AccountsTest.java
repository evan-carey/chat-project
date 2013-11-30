package edu.ucsd.cse110.server.accountinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class AccountsTest {
	
	private Accounts accounts;
	
	@Before
	public void init() {
		accounts = new Accounts();
	}

	@Test
	public void testSetPassword() {
		String newPass = "newpass";
		
		// valid
		try {
			accounts.setPassword("testname", "testpass", newPass);
			assertEquals(newPass, accounts.getPassword("testname"));
		} catch (AccountException e) {
			fail();
		}
		
		// invalid username
		try {
			accounts.setPassword("madeupname", "", "blahblahblah");
			fail();
		} catch (AccountException e) {
			// pass
		}
		
		// invalid password
		try {
			accounts.setPassword("testname", "incorrectPass", newPass);
			fail();
		} catch (AccountException e) {
			// pass
		}
		// ensure password hasn't been changed
		try {
			assertEquals(newPass, accounts.getPassword("testname"));
		} catch (AccountException e1) {
			fail();
		}
	}

	@Test
	public void testGetPassword() {
		try {
			assertEquals("testpass", accounts.getPassword("testname"));
		} catch (AccountException e) {
			fail();
		}
		try {
			accounts.getPassword("someMadeUpName");
			fail();
		} catch (AccountException e) {
			// pass
		}
	}

	@Test
	public void testAddAccount() {
		// valid
		try {
			accounts.addAccount("newUser", "newPass");
		} catch (AccountException e) {
			// pass
		}
		
		// invalid (account already exists)
		try {
			accounts.addAccount("newUser", "password");
			fail();
		} catch (AccountException e) {
			// pass
		}
	}

	@Test
	public void testHasUsername() {
		assertTrue(accounts.hasUsername("testname"));
		assertFalse(accounts.hasUsername("testpass"));
	}

}
