package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
//6
public class JoinResponse extends Packet
{
	private JoinResponseType responseType;
	private NodeUID uid;
	
	public JoinResponse(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public JoinResponse(JoinResponseType responseType, NodeUID uid)
	{
		super(14, (byte)6);
		this.responseType = responseType;
		this.uid = uid;
	}
	
	public JoinResponseType getResponseType() 
	{
		return responseType;
	}
	public NodeUID getUid() 
	{
		return uid;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(14);
		intiateSerialization(buf);
		buf.put(responseType.getValue());
		if(uid != null)
		{
			buf.putInt(uid.SID);
			buf.putInt(uid.ID);
		}
		else
		{
			buf.putInt(0);
			buf.putInt(0);
		}
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		switch(buf.get())
		{
			case 0 : responseType = JoinResponseType.Accepted; break;
			case 1 : responseType = JoinResponseType.NAME_TAKEN; break;
			case 2 : responseType = JoinResponseType.DENIED; break;
		}
		if(responseType == JoinResponseType.Accepted)
			uid = new NodeUID(buf.getInt(), buf.getInt());
	}
}
