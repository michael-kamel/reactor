package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
import chatproject.packets.Packet;

public class ClientJoin extends Packet
{
	private NodeUID clientUID;
	private String name;
	private int nameLen;
	
	public ClientJoin(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ClientJoin(NodeUID clientUID, String name)
	{
		super(17 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH , (byte)11);
		this.clientUID = clientUID;
		this.name = name;
		this.nameLen = name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH;
	}
	
	public NodeUID getClientUID() 
	{
		return clientUID;
	}
	public String getName() 
	{
		return name;
	}
	public int getNameLen() 
	{
		return nameLen;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(17 + name.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH);
		intiateSerialization(buf);
		buf.putInt(clientUID.SID);
		buf.putInt(clientUID.ID);
		buf.putInt(nameLen);
		buf.put(name.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws  UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		clientUID = new NodeUID(buf.getInt(), buf.getInt());
		nameLen = buf.getInt();
		byte[] nameBuf = new byte[nameLen];
		buf.get(nameBuf, 0, nameLen);
		name = new String(nameBuf, Constants.DEFAULT_CHARSET);
	}
}