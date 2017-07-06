package chatproject.packets;

public enum AcknowledgementType 
{
	GET_MEMBERS((byte)0), QUIT((byte)1);

    private final byte value;
    private AcknowledgementType(byte value) 
    {
        this.value = value;
    }
    public byte getValue() 
    {
        return value;
    }
}
