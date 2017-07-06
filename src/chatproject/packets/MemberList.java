package chatproject.packets;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.misc.Pair;
import chatproject.node.NodeUID;
//7
public class MemberList extends Packet
{
	private List<Pair<NodeUID, String>> members;
	private byte update;
	private int membersLen;
	private int membersSize;
	
	public MemberList(ByteBuffer buf) throws PacketLengthNotMatching, UnsupportedEncodingException
	{
		super(buf);
	}
	public MemberList(List<Pair<NodeUID, String>> members, boolean update)
	{
		super(10, (byte)7);
		if(update)
			this.update = 1;
		else
			this.update = 0;
		this.members = members;
		this.membersLen = calculatePacketSize();
		this.setLength(10 + membersLen);
		this.membersSize = members.size();
	}
	
	public int getMembersSize() 
	{
		return membersSize;
	}
	public List<Pair<NodeUID, String>> getMembers() 
	{
		return members;
	}
	public boolean isUpdate()
	{
		return update == 1;
	}
	
	private int calculatePacketSize()
	{
		int ret = 0;
		for(Pair<NodeUID, String> pair : members)
			ret += 8 + (isUpdate() ? 4 +pair.getRight().length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH : 0);
		return ret;
	}
	
	public ByteBuffer serialize() throws IOException 
	{
		ByteBuffer buf = ByteBuffer.allocate(10 + membersLen);
		intiateSerialization(buf);
		buf.put(update);
		buf.putInt(membersSize);
		for(Pair<NodeUID, String> pair : members)
		{
			buf.putInt(pair.getLeft().SID);
			buf.putInt(pair.getLeft().ID);
			if(isUpdate())
			{
				buf.putInt(pair.getRight().length() * Constants.DEFAULT_CHARSET_CHAR_LENGTH);
				buf.put(pair.getRight().getBytes(Constants.DEFAULT_CHARSET));
			}
		}
		buf.flip();
		return buf;
	}
	public void deserialize(ByteBuffer buf) throws UnsupportedEncodingException, PacketLengthNotMatching
	{
		members = new ArrayList<Pair<NodeUID,String>>();
		intiateDeserialization(buf);
		this.update = buf.get();
		this.membersSize = buf.getInt();
		String name = null;
		for(int i = 0; i < membersSize; i++)
		{
			NodeUID uid = new NodeUID(buf.getInt(), buf.getInt());
			if(isUpdate())
			{
				byte[] nameBuf = new byte[buf.getInt()];
				buf.get(nameBuf, 0, nameBuf.length);
				name = new String(nameBuf, Constants.DEFAULT_CHARSET);
			}
			Pair<NodeUID, String> pair = new Pair<NodeUID, String>(uid, name);
			members.add(pair);
		}
		
	}
}