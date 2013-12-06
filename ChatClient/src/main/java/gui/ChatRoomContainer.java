package gui;

import java.util.Vector;

import javax.swing.DefaultListModel;


public class ChatRoomContainer {
	private DefaultListModel<String> ChatRoomNames;
	private Vector<ChatRoom> ChatRooms;
	
	
	public ChatRoomContainer(){
		ChatRoomNames = new DefaultListModel<String>();
		ChatRooms = new Vector<ChatRoom>();
	}
	
	public boolean addChatRoom(ChatRoom c){
		if(c == null)
			return false;
		
		ChatRooms.add(c);
		ChatRoomNames.addElement(c.getName());
		return true;
	}
	
	public Vector<ChatRoom> getChatRooms(){
		return ChatRooms;
	}
	
	public DefaultListModel<String> getChatRoomNames(){
		return ChatRoomNames;
	}
	
	public ChatRoom getChatRoom(String name){
		if(ChatRoomNames.contains(name)){
			return ChatRooms.get(ChatRoomNames.indexOf(name));
		}
		
		return null;
	}
	
	public ChatRoom getChatRoom(int index){
		return ChatRooms.elementAt(index);
	}

	public int size() {
		// TODO Auto-generated method stub
		return ChatRooms.size();
	}

	public boolean contains(String room) {
		// TODO Auto-generated method stub
		return this.ChatRoomNames.contains(room);
	}

	public boolean removeChatRoom(String name) {
		// TODO Auto-generated method stub
		int i = ChatRoomNames.indexOf(name);
		if (i >= 0){
			return (ChatRooms.remove(ChatRooms.get(i))) && (ChatRoomNames.removeElement(ChatRoomNames.getElementAt(i)));
		}
		return false;
	}
	
}

