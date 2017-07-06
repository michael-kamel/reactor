package chatproject.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.node.NodeUID;
//4
public class Chat extends Packet
{
	private String message;
	private NodeUID sender;
	private int messageLen;
	
	public Chat(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public Chat(NodeUID sender, String message)
	{
		super(17 + (message.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH), (byte)4);
		this.message = message;
		this.sender = sender;
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
	public NodeUID getSender() 
	{
		return sender;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(17 + messageLen);
		intiateSerialization(buf);
		buf.putInt(sender.SID);
		buf.putInt(sender.ID);
		buf.putInt(messageLen);
		buf.put(message.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		sender = new NodeUID(buf.getInt(), buf.getInt());
		messageLen = buf.getInt();
		byte[] messageBuf = new byte[messageLen];
		buf.get(messageBuf, 0, messageBuf.length);
		message = new String(messageBuf, Constants.DEFAULT_CHARSET);
	}
}
