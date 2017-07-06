package chatproject.packets.servercompackets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.packets.Packet;

public class ReserveNameResponse extends Packet 
{
	private String name;
	private byte reserved;
	
	public ReserveNameResponse(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ReserveNameResponse(String name, boolean reserved)
	{
		super(6 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH , (byte)13);
		this.name = name;
		this.reserved = (byte) (reserved ? -Constants.MAX_SERVERS : 1);
	}
	
	public String getName()
	{
		return name;
	}
	public byte getReserved() 
	{
		return reserved;
	}
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(6 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH);
		intiateSerialization(buf);
		buf.put(reserved);
		buf.put(name.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		reserved = buf.get();
		byte[] nameBuf = new byte[buf.capacity() - 6];
		buf.get(nameBuf, 0, nameBuf.length);
		name = new String(nameBuf, Constants.DEFAULT_CHARSET);
	}
}