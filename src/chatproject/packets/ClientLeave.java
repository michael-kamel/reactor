package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
import chatproject.packets.Packet;

public class ClientLeave extends Packet
{
	private NodeUID clientUID;
	
	public ClientLeave(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ClientLeave(NodeUID clientUID)
	{
		super(13 , (byte)10);
		this.clientUID = clientUID;
	}
	
	public NodeUID getClientUID() 
	{
		return clientUID;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(13);
		intiateSerialization(buf);
		buf.putInt(clientUID.SID);
		buf.putInt(clientUID.ID);
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		clientUID = new NodeUID(buf.getInt(), buf.getInt());
	}
}
