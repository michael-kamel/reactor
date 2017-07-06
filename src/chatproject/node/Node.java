package chatproject.node;

public class Node
{
	private NodeUID uid;
	private boolean readyForCommunication;
	
	public Node(NodeUID uid) 
	{
		this.uid = uid;
	}
	public NodeUID getUid() 
	{
		return uid;
	}
	public void setUid(NodeUID uid) 
	{
		this.uid = uid;
	}
	public boolean isReadyForCommunication() 
	{
		return readyForCommunication;
	}
	public void setReadyForCommunication(boolean readyForCommunication) 
	{
		this.readyForCommunication = readyForCommunication;
	}
	
}