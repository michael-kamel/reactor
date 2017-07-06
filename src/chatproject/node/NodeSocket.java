package chatproject.node;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import chatproject.node.Node;
import chatproject.packets.Packet;

public abstract class NodeSocket
{
    private ExecutorService pool;
    public final SocketChannel socketChannel;
    private ByteBuffer input = ByteBuffer.allocateDirect(1024);
    private Node owner;
    private boolean hasPrivilege;
    public final SelectionKey selectionKey;
	
    public NodeSocket(Selector selector, SocketChannel sockChannel, ExecutorService pool) throws IOException
    {
    	this.pool = pool;
    	this.socketChannel = sockChannel;
    	sockChannel.configureBlocking(false);
        Runnable eventHandler = () -> run();
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ, eventHandler);
    }
    
    public Node getOwner()
    {
		return owner;
	}
	public void setOwner(Node owner) 
	{
		this.owner = owner;
	}
	public boolean isHasPrivilege() 
	{
		return hasPrivilege;
	}
	public void setHasPrivilege(boolean hasPrivilege)
	{
		this.hasPrivilege = hasPrivilege;
	}
	
	public void elevateBuffer()
	{
		input = ByteBuffer.allocateDirect(32768);
	}
	public void run()
    {
        try 
        {
           onRead();
        } 
        catch (IOException ex) 
        {
            ex.printStackTrace();
        }
    } 
    private void onRead() throws IOException 
    {
    	try
    	{
    		int readCount = socketChannel.read(input);
    		input.flip();
    		if (readCount > 0)
    		{
    			System.out.println("[Node Socket]Read bytes from stream len:" + readCount);
    			readProcess();
    		}
    	}
    	catch(IOException e)
    	{
    		if(socketChannel.isOpen())
    		{
    			selectionKey.cancel();
    			unexpectedShutdown();
    		}
    		socketChannel.close();
    	}
    }
    synchronized void readProcess()
    {
    	int len = input.getInt();
    	System.out.println("[Node Socket]Read packet len:" + len);
    	input.position(input.position() - 4);
        ByteBuffer packet = ByteBuffer.allocate(len);
        for(int i = 0; i < len ; i++)
        	packet.put(input.get());
        packet.flip();
        System.out.println("[Node Socket] ByteBuffer info: limit:" + packet.limit() + " Capacity:" + packet.capacity() + " Pos:" + packet.position());
        pool.execute(() ->
		{
			handle(packet);
		});
        if(input.hasRemaining())
        {
        	System.out.println("Handling Trailing Packet len:" + (input.limit() - input.position()));
			readProcess();
        }
        else
        	input.clear();
    }
    public synchronized void send(ByteBuffer buf) throws IOException
    {
    	try
    	{
    		System.out.println("[Node Socket] sending packet content:");
    		while(buf.hasRemaining())
    	    {
    	        System.out.print(buf.get() + " ");
    	    }
    		buf.flip();
    		System.out.println();
    		socketChannel.write(buf);
    	}
    	catch(IOException exc)
    	{
    		selectionKey.cancel();
    		socketChannel.close();
    		unexpectedShutdown();
    		throw exc;
    	}
    	finally 
    	{
    		buf.flip();
    	}
    }
    public void send(Packet packet) throws IOException
    {
    	send(packet.serialize());
    }
    protected abstract void handle(ByteBuffer pack);
    protected abstract void unexpectedShutdown();
}