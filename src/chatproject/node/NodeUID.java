package chatproject.node;

public class NodeUID
{
	public int SID;
	public int ID;
	
	public NodeUID(int SID, int ID)
	{
		this.SID = SID;
		this.ID = ID;
	}
	
	public boolean equals(Object other)
	{
		if(other == null || !  NodeUID.class.isAssignableFrom(other.getClass()))
			return false;
		 NodeUID otherNode = (NodeUID) other;
		if(SID == otherNode.SID && ID == otherNode.ID)
			return true;
		return false;
	}
	public String toString()
	{
		return SID + ":" + ID;
	}
	public int hashCode()
	{
		return (SID << 24) | ID;
	}
}