package chatproject.packets.servercompackets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.packets.JoinResponseType;
import chatproject.packets.Packet;

public class ServerJoinResponse extends Packet
{
	private JoinResponseType responseType;
	private int senderId;
	private int proposedSid;
	
	public ServerJoinResponse(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ServerJoinResponse(JoinResponseType responseType, int senderId, int proposedSID)
	{
		super(14, (byte)14);
		this.responseType = responseType;
		this.senderId = senderId;
		this.proposedSid = proposedSID;
	}
	
	public JoinResponseType getResponseType() 
	{
		return responseType;
	}
	public int getSenderId() 
	{
		return senderId;
	}
	public int getProposedSid() 
	{
		return proposedSid;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(14);
		intiateSerialization(buf);
		buf.put(responseType.getValue());
		buf.putInt(senderId);
		buf.putInt(proposedSid);
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
		{
			senderId = buf.getInt();
			proposedSid = buf.getInt();
		}
	}
}
