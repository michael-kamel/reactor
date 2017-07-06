package chatproject.packets;

import java.io.*;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
//2
public class ChatRequest extends Packet
{
	private String message;
	private NodeUID destClient;
	private int messageLen;
	
	public ChatRequest(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public ChatRequest(NodeUID destClient, String message)
	{
		super(17 + (message.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH), (byte)2);
		this.message = message;
		this.destClient = destClient;
		this.messageLen = message.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH;
	}
	
	public String getMessage() 
	{
		return message;
	}
	public int getMessageLen() 
	{
		return messageLen;
	}
	public NodeUID getDestClient() 
	{
		return destClient;
	}

	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(17 + messageLen);
		intiateSerialization(buf);
		buf.putInt(destClient.SID);
		buf.putInt(destClient.ID);
		buf.putInt(messageLen);
		buf.put(message.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		destClient = new NodeUID(buf.getInt(), buf.getInt());
		messageLen = buf.getInt();
		byte[] messageBuf = new byte[messageLen];
		buf.get(messageBuf, 0, messageBuf.length);
		message = new String(messageBuf, Constants.DEFAULT_CHARSET);
	}
}