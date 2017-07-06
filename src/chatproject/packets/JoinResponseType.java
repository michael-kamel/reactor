package chatproject.packets;

public enum JoinResponseType
{
    Accepted((byte)0), NAME_TAKEN((byte)1), DENIED((byte)2);

    private final byte value;
    private JoinResponseType(byte value) 
    {
        this.value = value;
    }
    public byte getValue() 
    {
        return value;
    }
}
