package chatproject.packets.servercompackets;

import java.io.*;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
import chatproject.packets.Packet;
//3
public class ChatMediator extends Packet
{
	private String message;
	private NodeUID sender;
	private NodeUID reciever;
	private int ttl;
	private int messageLen;
	
	public ChatMediator(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ChatMediator(NodeUID sender, NodeUID reciever, int ttl, String message)
	{
		super(29 + (message.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH), (byte)3);
		this.message = message;
		this.sender = sender;
		this.reciever = reciever;
		this.messageLen = message.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH;
		this.ttl = ttl;
	}
	
	public String getMessage() 
	{
		return message;
	}
	public int getTtl()
	{
		return ttl;
	}
	public void decrementTtl()
	{
		ttl--;
	}
	public int getMessageLen() 
	{
		return messageLen;
	}
	public NodeUID getSender() 
	{
		return sender;
	}
	public NodeUID getReciever() 
	{
		return reciever;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(29 + messageLen);
		intiateSerialization(buf);
		buf.putInt(reciever.SID);
		buf.putInt(reciever.ID);
		buf.putInt(sender.SID);
		buf.putInt(sender.ID);
		buf.putInt(ttl);
		buf.putInt(messageLen);
		buf.put(message.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		reciever = new NodeUID(buf.getInt(), buf.getInt());
		sender = new NodeUID(buf.getInt(), buf.getInt());
		ttl = buf.getInt();
		messageLen = buf.getInt();
		byte[] messageBuf = new byte[messageLen];
		buf.get(messageBuf, 0, messageBuf.length);
		message = new String(messageBuf, Constants.DEFAULT_CHARSET);
	}
}