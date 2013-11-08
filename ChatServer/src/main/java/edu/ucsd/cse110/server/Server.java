package edu.ucsd.cse110.server;

public class Server {
	
	public static boolean hasMessage = false;

	public void receive(String msg) {
		System.out.println(msg);
		hasMessage = true;
		//send("Received message: \"" + msg + "\"");
	}
	
	public void send(String msg) {
		new ServerProducer(msg).run();
	}
}
