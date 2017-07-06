package chatproject.packets.servercompackets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.packets.Packet;

public class ReserveNameRequest extends Packet 
{
	private String name;
	
	public ReserveNameRequest(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ReserveNameRequest(String name)
	{
		super(5 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH , (byte)12);
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(5 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH);
		intiateSerialization(buf);
		buf.put(name.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		byte[] nameBuf = new byte[buf.capacity() - 5];
		buf.get(nameBuf, 0, nameBuf.length);
		name = new String(nameBuf, Constants.DEFAULT_CHARSET);
	}
}
