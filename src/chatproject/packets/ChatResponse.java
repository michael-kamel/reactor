package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
//5
public class ChatResponse extends Packet
{
	private ChatResponseType responseType;
	private NodeUID recieverUid;//sent chat request
	private NodeUID senderUid;//responds to chat
	
	public ChatResponse(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ChatResponse(ChatResponseType responseType, NodeUID recieverUid, NodeUID senderUid)
	{
		super(22, (byte)5);
		this.responseType = responseType;
		this.recieverUid = recieverUid;
		this.senderUid = senderUid;
	}
	
	public ChatResponseType getErrorType() 
	{
		return responseType;
	}
	public NodeUID getRecieverUid() 
	{
		return recieverUid;
	}
	public NodeUID getSenderUid() 
	{
		return senderUid;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(22);
		intiateSerialization(buf);
		buf.put(responseType.getValue());
		buf.putInt(recieverUid.SID);
		buf.putInt(recieverUid.ID);
		buf.putInt(senderUid.SID);
		buf.putInt(senderUid.ID);
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		switch(buf.get())
		{
			case 0 : responseType = ChatResponseType.MESSAGE_SENT; break;
			case 1 : responseType = ChatResponseType.DESTINATION_CLIENT_NOT_FOUND; break;
			case 2 : responseType = ChatResponseType.MESSAGE_NOT_SENT; break;
		}
		recieverUid = new NodeUID(buf.getInt(), buf.getInt());
		senderUid = new NodeUID(buf.getInt(), buf.getInt());
	}
}
