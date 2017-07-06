package chatproject.packets;

import java.io.*;
import java.nio.ByteBuffer;
import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
//1
public class JoinRequest extends Packet
{
	private String requestToken;
	private byte requestPrivilege;
	
	public JoinRequest(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public JoinRequest(String requestToken, byte requestPrivilege)
	{
		super(6 + (requestToken.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH), (byte)1);
		this.requestToken = requestToken;
		this.requestPrivilege = requestPrivilege;
	}
	
	public String getRequestToken() 
	{
		return requestToken;
	}
	public byte getRequestPrivilege() 
	{
		return requestPrivilege;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(6 + requestToken.length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH);
		intiateSerialization(buf);
		buf.put(requestPrivilege);
		buf.put(requestToken.getBytes(Constants.DEFAULT_CHARSET));
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		requestPrivilege = buf.get();
		byte[] tokenBuf = new byte[buf.capacity() - 6];
		buf.get(tokenBuf, 0, tokenBuf.length);
		requestToken = new String(tokenBuf, Constants.DEFAULT_CHARSET);
	}
}