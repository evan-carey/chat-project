package edu.ucsd.cse110.server;

import static org.junit.Assert.*;

import javax.jms.JMSException;

import org.junit.Test;

public class ServerRunChatRoomTest {

	// Test to ensure that a chat room was successfully created
	@Test
	public void testCreateRooms() {
		try {

			// Create a new ServerRunChatRoom
			ServerRunChatRoom customRoom = new ServerRunChatRoom();

			// Add some rooms
			customRoom.createChatRoom("Lobby");
			customRoom.createChatRoom("UCSD");
			customRoom.createChatRoom("CSE110");

			// Ensure they exist
			assertTrue(customRoom.roomExists("Lobby"));
			assertTrue(customRoom.roomExists("UCSD"));
			assertTrue(customRoom.roomExists("CSE110"));

			// Ensure that rooms we did not create do not exist
			assertFalse(customRoom.roomExists("Null"));
			assertFalse(customRoom.roomExists("Team6"));

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}
	}

	// Test to ensure that a chat room was successfully created
	@Test
	public void testRoomNumbers() {
		try {

			// Create a new ServerRunChatRoom
			ServerRunChatRoom customRoom = new ServerRunChatRoom();
			customRoom.createChatRoom("Lobby");
			customRoom.createChatRoom("UCSD");
			customRoom.createChatRoom("CSE110");

			assertEquals(5, customRoom.getNumberRooms());

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}

	}

	// Test to ensure that a room can be removed
	@Test
	public void testRemoveRooms() {
		try {

			// Create a new ServerRunChatRoom
			ServerRunChatRoom customRoom = new ServerRunChatRoom();
			customRoom.createChatRoom("Lobby");
			customRoom.createChatRoom("UCSD");
			customRoom.createChatRoom("CSE110");

			assertEquals(5, customRoom.getNumberRooms());

			customRoom.removeRoom("Lobby");
			assertFalse(customRoom.roomExists("Lobby"));
			assertEquals(4, customRoom.getNumberRooms());

			customRoom.removeRoom("UCSD");
			assertFalse(customRoom.roomExists("UCSD"));
			assertEquals(3, customRoom.getNumberRooms());

			customRoom.removeRoom("CSE110");
			assertFalse(customRoom.roomExists("CSE110"));
			assertEquals(2, customRoom.getNumberRooms());

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}

	}

	// Test to ensure that a user can't remove the default rooms
	@Test
	public void testRemoveDefault() {
		try {

			// Create a new ServerRunChatRoom
			ServerRunChatRoom customRoom = new ServerRunChatRoom();
			assertFalse(customRoom.removeRoom("CHATROOM"));
			assertEquals(2, customRoom.getNumberRooms());

			assertFalse(customRoom.removeRoom("CHATROOM_1"));
			assertEquals(2, customRoom.getNumberRooms());

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}

	}

	// Test to ensure that a user can't remove the default rooms
	@Test
	public void testtransmitChatRoomList() {
		try {

			// Create a new ServerRunChatRoom
			ServerRunChatRoom customRoom = new ServerRunChatRoom();
			customRoom.createChatRoom("Lobby");
			customRoom.createChatRoom("UCSD");
			customRoom.createChatRoom("CSE110");

			String allRooms = customRoom.transmitChatRoomList();
			String expectedRooms = "UCSD CHATROOM_1 CHATROOM Lobby CSE110 ";

			System.out.println(allRooms);
			
			// Need to check the order of the list
			assertEquals(allRooms, expectedRooms);

		} catch (JMSException e) {
			System.err.println(e.getMessage());
		}

	}

}
