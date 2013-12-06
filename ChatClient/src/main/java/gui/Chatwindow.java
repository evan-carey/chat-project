package gui;

import java.io.FileNotFoundException;
import java.util.Vector;
	

public class Chatwindow {
	
	public static final ClientContainer clientContainer = new ClientContainer();
	public static final Vector<String> ChatRoomList = new Vector<String>();
	public static Server server;
	
	public static void main(String[] args) throws FileNotFoundException{
		new MainMenu();	
		String arg0 = null;
		if (args.length > 0) arg0 = args[0];
		server = new Server(arg0);
	}
	
}