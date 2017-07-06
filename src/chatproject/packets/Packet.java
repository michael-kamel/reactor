package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import chatproject.exceptions.PacketLengthNotMatching;

public abstract class Packet
{
	private int length;
	private byte id;
	
	public Packet(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		deserialize(buf);
	}
	public Packet(int length, byte id)
	{
		this.length = length;
		this.id = id;
	}
	public int getLength()
	{
		return length;
	}
	protected void setLength(int length)
	{
		this.length = length;
	}
	public byte getId()
	{
		return id;
	}
	protected void setId(byte id)
	{
		this.id = id;
	}
	
	public abstract ByteBuffer serialize() throws IOException;
	public abstract void deserialize(ByteBuffer in) throws PacketLengthNotMatching, UnsupportedEncodingException;
	
	protected void intiateSerialization(ByteBuffer buf)
	{
		buf.putInt(getLength());
		buf.put(getId());
	}
	protected void intiateDeserialization(ByteBuffer buf) throws PacketLengthNotMatching
	{
		int len = buf.getInt();
		if(len != buf.capacity())
			throw new PacketLengthNotMatching(len, buf.limit());
		this.length = len;
		this.id = buf.get();
	}
}