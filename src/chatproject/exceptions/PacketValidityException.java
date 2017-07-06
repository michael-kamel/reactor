package chatproject.exceptions;

public class PacketValidityException extends Exception
{
	private static final long serialVersionUID = 1L;
	public PacketValidityException() 
	{
		super();
	}
	public PacketValidityException(String message) 
	{
		super(message);
	}
}