package chatproject.packets.servercompackets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.packets.Packet;

public class FinalizeServerJoin extends Packet
{
	private int sid;
	
	public FinalizeServerJoin(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public FinalizeServerJoin(int sid)
	{
		super(9, (byte)15);
		this.sid = sid;
	}
	
	public int getSid() 
	{
		return sid;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(9);
		intiateSerialization(buf);
		buf.putInt(sid);
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		sid = buf.getInt();
	}
}