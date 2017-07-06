package chatproject.server.main;

import java.io.IOException;
import java.util.ArrayList;

import chatproject.misc.Pair;
import chatproject.server.reactor.ReactorServer;

public class ServerMain 
{	
	public static void main(String[] args) throws IOException
	{
			ArrayList<Pair<String , Integer>> otherServers = new ArrayList<Pair<String,Integer>>();
			otherServers.add(new Pair<String, Integer>("127.0.0.1", 6000));
			//otherServers.add(new Pair<String, Integer>("127.0.0.1", 6001));
			//otherServers.add(new Pair<String, Integer>("127.0.0.1", 6002));
			ReactorServer server = new ReactorServer(6001, otherServers);
			Thread T = new Thread(server::run);
			T.start();
	}
}