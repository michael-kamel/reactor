package chatproject.server;

import java.util.List;

import chatproject.misc.Pair;
import chatproject.node.NodeUID;

@Deprecated
public interface IServer 
{
	boolean joinResponse();
	List<Pair<NodeUID, String>> memberListResponse();
	void route (String message, int destinationID);
}
