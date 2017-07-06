package chatproject.node;

import java.util.concurrent.ConcurrentHashMap;

public class Server extends Node
{
	private ConcurrentHashMap<Integer, String> clients;
	public Server(NodeUID uid)
	{
		super(uid);
		this.clients = new ConcurrentHashMap<Integer, String>();
	}
	
	public ConcurrentHashMap<Integer, String> getClients() 
	{
		return clients;
	}
	public boolean reservesName(String name)
	{
		for(String rname : clients.values())
		{
			if(name.equals(rname))
				return true;
		}
		return false;
	}
}
