package chatproject.node;

public class Client extends Node
{
	public Client(NodeUID uid)
	{
		super(uid);
	}
	private String name;
	
	public Client(String name, NodeUID uid)
	{
		this(uid);
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
}
