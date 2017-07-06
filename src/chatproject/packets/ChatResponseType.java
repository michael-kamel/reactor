package chatproject.packets;

public enum ChatResponseType 
{
	MESSAGE_SENT((byte)0),DESTINATION_CLIENT_NOT_FOUND((byte)1), MESSAGE_NOT_SENT((byte)2),;

    private final byte value;
    private ChatResponseType(byte value) 
    {
        this.value = value;
    }
    public byte getValue() 
    {
        return value;
    }
}