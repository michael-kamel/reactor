package chatproject.server;
import java.io.*;
import java.net.*;

@Deprecated
public class Server 
{
	private String serverName;
	private String ip;
	private String hostName;
	private int port;
	private Socket connectionSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	private boolean running;
	
	public Server(String serverName, String ip, String hostName, int port)
	{
		this.serverName = serverName;
		this.ip = ip;
		this.hostName = hostName;
		this.port = port;
		this.running = false;
	}

	public boolean isRunning()
	{
		return running;
	}
	public String getServerName() 
	{
		return serverName;
	}
	public String getIp() 
	{
		return ip;
	}
	public String getHostName() 
	{
		return hostName;
	}
	public int getPort()
	{
		return port;
	}
	public void intiateConnection() throws IOException
	{
		ServerSocket welcomeSocket = new ServerSocket(getPort());
		connectionSocket = welcomeSocket.accept();
		welcomeSocket.close();
		running = true;
	}
	public void startListening() throws IOException
	{
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		Thread t = new Thread(() ->
		{
	        try
	        {
	        	runServer();
	        	connectionSocket.close();
	        }
	        catch(IOException e)
	        {
	        	e.printStackTrace();
	        }
	        finally
	        {
	        	Thread.currentThread().interrupt();
	        }
		});
		t.start();
	}
	private void runServer() throws IOException
	{
		String clientSentence = "";
		while((!(clientSentence.equalsIgnoreCase("BYE") || clientSentence.equalsIgnoreCase("QUIT"))) && connectionSocket.isConnected())
		{
			clientSentence = inFromClient.readLine();
			displayMsg(clientSentence);
		}
		System.out.println("Disconnected");
		running = false;
	}
	private void displayMsg(String Message)
	{
		System.out.println("[Client]: " + Message);
	}
	public void writeMsg(String Message) throws IOException
	{
		outToClient.writeBytes(Message + "\n");
	}
}