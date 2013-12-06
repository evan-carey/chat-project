package gui;

import java.io.FileNotFoundException;
import java.util.Vector;
	

public class Chatwindow {
	
	public static final ClientContainer clientContainer = new ClientContainer();
	public static final Vector<String> ChatRoomList = new Vector<String>();
	public static Server server;
	
	public static void main(String[] args) throws FileNotFoundException{
		new MainMenu();	
		server=new Server();
	}
	
}