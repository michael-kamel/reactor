package chatproject.exceptions;

public class PacketLengthNotMatching extends PacketValidityException
{
	private static final long serialVersionUID = 1L;

	public PacketLengthNotMatching(int expected, int retrieved)
	{
		super("Packet length expected[" + expected +  "]. Packet length retrieved [" + retrieved + "].");
	}
	
}
