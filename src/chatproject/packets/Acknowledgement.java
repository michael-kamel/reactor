package chatproject.packets;

import java.io.*;
import java.nio.ByteBuffer;
import chatproject.exceptions.PacketLengthNotMatching;
//8
public class Acknowledgement extends Packet
{
	private AcknowledgementType type;
	private byte param1;
	private int param2;
	
	public Acknowledgement(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public Acknowledgement(AcknowledgementType type, byte param1, int param2)
	{
		super(11, (byte)8);
		this.type = type;
		this.param1 = param1;
		this.param2 = param2;
	}
	
	public AcknowledgementType getType() 
	{
		return type;
	}
	public byte getParam1() 
	{
		return param1;
	}
	public int getParam2()
	{
		return param2;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(6);
		intiateSerialization(buf);
		buf.put(type.getValue());
		buf.put(param1);
		buf.putInt(param2);
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		intiateDeserialization(buf);
		switch(buf.get())
		{
			case 0: type = AcknowledgementType.GET_MEMBERS; break;
			case 1: type = AcknowledgementType.QUIT; break;
		}
		param1 = buf.get();
		param2 = buf.getInt();
	}
}
