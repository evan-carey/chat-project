package gui;

import java.util.Vector;

import javax.swing.DefaultListModel;


public class ClientContainer {

	private DefaultListModel<String> ClientNames;
	private Vector<Client> Clients;
	
	public ClientContainer(){
		ClientNames = new DefaultListModel<String>();
		Clients = new Vector<Client>();
	}
	
	public boolean addClient(Client c){
		if(c == null)
			return false;
		
		Clients.add(c);
		ClientNames.addElement(c.getName());
		return true;
	}
	
	public Vector<Client> getClients(){
		return Clients;
	}
	
	public DefaultListModel<String> getClientNames(){
		return ClientNames;
	}
	
	public Client getClient(String name){
		if(ClientNames.contains(name)){
			return Clients.get(ClientNames.indexOf(name));
		}
		
		return null;
	}
	
	public Client getClient(int index){
		return Clients.elementAt(index);
	}
	
	public boolean removeClient(Client c){
		if(ClientNames.removeElement(c.getName()) && Clients.remove(c))
				return true;
		return false;
	}

	public int size() {
		// TODO Auto-generated method stub
		return ClientNames.getSize();
	}

	public boolean contains(String text) {
		// TODO Auto-generated method stub
		return this.ClientNames.contains(text);
	}
}
