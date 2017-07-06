package chatproject.server.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import chatproject.constants.Constants;
import chatproject.exceptions.PacketLengthNotMatching;
import chatproject.misc.Pair;
import chatproject.node.Client;
import chatproject.node.NodeSocket;
import chatproject.node.NodeUID;
import chatproject.node.Server;
import chatproject.packets.*;
import chatproject.packets.servercompackets.*;

public class ReactorServer
{
    final Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private ConcurrentHashMap<Integer, NodeSocket> clientSockets;
    private ConcurrentHashMap<Integer, NodeSocket> networkServers;
    private List<String> reservedNames;
    private ReentrantLock reservedNamesLock;
    private List<Pair<String, Integer>> pendingNameRequests;
    private ReentrantLock pendingNameRequestsLock;
    private ConcurrentLinkedQueue<NodeSocket> crashedServers;
    private ConcurrentLinkedQueue<NodeSocket> crashedClients;
    private ScheduledExecutorService patrolExecutor;
    private int serverID;
    private int cidCounter;
    private int sidCounter;
    private ExecutorService threadPool;
    private int serverCount;
    private boolean ready;
    private ArrayList<Pair<String , Integer>> otherServers;
    private int stampCount;
    private int syncCount;
    
    public ReactorServer(int port, ArrayList<Pair<String , Integer>> otherServers) throws IOException
    {
    	sidCounter = 0;
    	syncCount = 0;
    	stampCount = 0;
    	ready = false;
    	this.otherServers = otherServers;
    	serverID = generateSID();
    	cidCounter = 1;
    	crashedServers = new ConcurrentLinkedQueue<NodeSocket>();
    	crashedClients = new ConcurrentLinkedQueue<NodeSocket>();
    	patrolExecutor = Executors.newScheduledThreadPool(2);
    	clientSockets = new ConcurrentHashMap<Integer, NodeSocket>();
    	networkServers = new ConcurrentHashMap<Integer, NodeSocket>();
    	reservedNames = new ArrayList<String>();
    	pendingNameRequests = new ArrayList<Pair<String,Integer>>();
    	reservedNamesLock = new ReentrantLock();
    	pendingNameRequestsLock = new ReentrantLock();
    	threadPool = Executors.newCachedThreadPool();
        selector = Selector.open();
    	serverSocketChannel = ServerSocketChannel.open();
	    serverSocketChannel.socket().bind(new InetSocketAddress(port));
	    serverSocketChannel.configureBlocking(false);
	    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());
    }
    
    public List<String> getReservedNames() 
    {
		return reservedNames;
	}
	public int getCidCounter()
    {
    	return cidCounter;
    }
    public Selector getSelector()
    {
		return selector;
	}
	public ServerSocketChannel getServerSocketChannel()
	{
		return serverSocketChannel;
	}
	public ConcurrentHashMap<Integer, NodeSocket> getClientSockets()
	{
		return clientSockets;
	}
	public ConcurrentHashMap<Integer, NodeSocket> getNetworkServers()
	{
		return networkServers;
	}
	public int getServerID() 
	{
		return serverID;
	}
	
	private ArrayList<NodeSocket> bindToServers()
	{
		ArrayList<NodeSocket> ret = new ArrayList<NodeSocket>();
		try
		{
			for(Pair<String, Integer> liveServer : otherServers)
			{
				SocketChannel socketChannel = SocketChannel.open();
				NodeSocket comNode = new NodeSocket(selector, socketChannel, threadPool)
                {
                	protected void handle(ByteBuffer pack) 
                	{
						HandlePacket(pack, this);
					}
					protected void unexpectedShutdown()
					{
						onUnexpectedShutdown(this);
					}
                };
                comNode.elevateBuffer();
                comNode.setOwner(new Server(null));
                comNode.setHasPrivilege(true);
                comNode.getOwner().setReadyForCommunication(true);
                socketChannel.connect(new InetSocketAddress(liveServer.getLeft(), liveServer.getRight()));
				ret.add(comNode);
			}
			return ret;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	private void tryConnectToLiveServers(ArrayList<NodeSocket> serverSockets)
	{
		try
		{
			for(NodeSocket comNode : serverSockets)
			{
				try
				{
                    JoinRequest pack = new JoinRequest(Constants.SERVER_TOKEN, (byte)1);
                    System.out.println("Waiting for server connection");
                    long startTime = System.currentTimeMillis();
                    while(!comNode.socketChannel.finishConnect() && System.currentTimeMillis() < startTime + Constants.TIMEOUT)
                    {
                    	continue;
                    }
                    if(comNode.socketChannel.isConnected())
                    {
                    	comNode.send(pack);
                    	stampCount--;
                    	syncCount--;
                    }
                    else
                    	System.out.println("Connection to live server:" + comNode.socketChannel.getRemoteAddress() + " failed.");
                    
				}
				catch(IOException e)
				{
					System.out.println("Server not available");
					continue;
				}
			}
			System.out.println("Waiting for server stamps");
			while(stampCount < 0)
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			System.out.println("Acquired server stamps");
			FinalizeServerJoin packet = new FinalizeServerJoin(serverID);
			for(NodeSocket server : networkServers.values())
			{
				server.send(packet);
			}
			System.out.println("Waiting for server client lists");
			while(syncCount < 0)
			{
				continue;
			}
			System.out.println("acquired server client lists");
			ready = true;
			patrolExecutor.scheduleAtFixedRate(this::clientRemover, 0, Constants.PATROL_INTERVAL, TimeUnit.MILLISECONDS);
			patrolExecutor.scheduleAtFixedRate(this::serverRemover, 0, Constants.PATROL_INTERVAL, TimeUnit.MILLISECONDS);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			ready = false;
		}
	}
	public void run()
    {
		ArrayList<NodeSocket> serverSocks = bindToServers();
		if(serverSocks != null)
			threadPool.execute(() -> tryConnectToLiveServers(serverSocks));
		else
			System.out.println("Failed to connect to other servers");
		System.out.println("Server listening to port:" + serverSocketChannel.socket().getLocalPort());
        try
        {
            while (!Thread.interrupted())
            {
                selector.select();
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                while (it.hasNext())
                {
                	 Runnable runnable = (Runnable)(it.next().attachment());
                     if (runnable != null)
                     	runnable.run();
                }
                selected.clear();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    class Acceptor implements Runnable
    {
        public void run()
        {
            try
            {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null)
                {
                	new NodeSocket(selector, socketChannel, threadPool)
                    {
                    	protected void handle(ByteBuffer pack) 
                    	{
							HandlePacket(pack, this);
						}
						protected void unexpectedShutdown()
						{
							onUnexpectedShutdown(this);
						}
                    };
                }
                System.out.println("Connection Accepted by ReactorServer");
            } 
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    private int	generateID()
    {
    	return cidCounter++;
    }
    private int generateSID()
    {
    	return sidCounter++;
    }
    
    private void onUnexpectedShutdown(NodeSocket nodeSocket)
    {
    	if(nodeSocket.getOwner() == null || !nodeSocket.getOwner().isReadyForCommunication())
    		return;
    	if(nodeSocket.isHasPrivilege())
    	{
    		unlinkServerInfo(nodeSocket);
    		crashedServers.add(nodeSocket);
    	}
    	else
    	{
    		unlinkClientInfo(nodeSocket);
    		crashedClients.add(nodeSocket);
    	}
    }
    private void sendServerMembersList(NodeSocket reciever) throws IOException
    {
    	MemberList toSend;
		List<Pair<NodeUID, String>> membersList = new ArrayList<Pair<NodeUID,String>>();
		for(NodeSocket sock : clientSockets.values())
			membersList.add(new Pair<NodeUID, String>(sock.getOwner().getUid(), ((Client)sock.getOwner()).getName()));
		toSend = new MemberList(membersList, true);
		reciever.send(toSend);
    }
    private void clientRemover()
    {
    	if(crashedClients.size() < 1)
    		return;
    	List<Pair<NodeUID, String>> toSendList = new ArrayList<Pair<NodeUID, String>>();
    	for(int i = 0; i < crashedClients.size(); i++)
    	{
    		NodeSocket clientSocket = crashedClients.remove();
    		Pair<NodeUID, String> pair = new Pair<NodeUID, String>(clientSocket.getOwner().getUid(), null);
    		toSendList.add(pair);
    	}
    	MemberList toSend = new MemberList(toSendList, false);
    	broadcastToClients(toSend);
    	broadcastToServers(toSend);
    }
    private void serverRemover()
    {
    	if(crashedServers.size() < 1)
    		return;
    	for(int i = 0; i < crashedServers.size(); i++)
    	{
    		NodeSocket serverSocket = crashedClients.remove();
    		removeServerLink(serverSocket, false);
    	}
    }
    private void unlinkClientInfo(NodeSocket clientSocket)
    {
    	clientSockets.remove(clientSocket.getOwner().getUid().ID);
    	reservedNamesLock.lock();
    	try
    	{
    		reservedNames.removeIf(s -> s.equals(((Client)clientSocket.getOwner()).getName()));
    	}
    	finally
    	{
    		reservedNamesLock.unlock();
    	}
    }
    private void unlinkServerInfo(NodeSocket serverSocket)
    {
    	networkServers.remove(serverSocket.getOwner().getUid().SID);
    	serverCount--;
    }
    private void removeClientFromServer(NodeSocket clientNode)
    {
    	ClientLeave pack = new ClientLeave(clientNode.getOwner().getUid());
    	clientSockets.remove(clientNode.getOwner().getUid().ID);
    	broadcastToClients(pack);
    	broadcastToServers(pack);
    	reservedNamesLock.lock();
    	try
    	{
    		reservedNames.remove(((Client)clientNode.getOwner()).getName());
    	}
    	finally
    	{
    		reservedNamesLock.unlock();
    	}
    	System.out.println("local client count record: " + clientSockets.size());
    }
    private void removeServerLink(NodeSocket serverSocket, boolean removeLink)
    {
    	Server serverNode = (Server) serverSocket.getOwner();
    	List<Pair<NodeUID, String>> toSendList = new ArrayList<Pair<NodeUID, String>>();
    	for(Entry<Integer, String> client : serverNode.getClients().entrySet())
    	{
    		Pair<NodeUID, String> pair = new Pair<NodeUID, String>(new NodeUID(serverSocket.getOwner().getUid().SID, client.getKey()), null);
    		toSendList.add(pair);
    	}
    	MemberList toSend = new MemberList(toSendList, false);
    	broadcastToClients(toSend);
    	if(removeLink)
    		unlinkServerInfo(serverSocket);
    }
    private void routeToServer(int serverID, Packet packet) throws IOException
    {
    	networkServers.get(serverID).send(packet);
    }
    private void broadcastToServers(Packet pack)
    {
    	for(NodeSocket server : networkServers.values())
    	{
    		try
    		{
    			System.out.println("Broadcasting to server:" + server.getOwner().getUid().SID);
    			server.send(pack);
    		}
    		catch(IOException e)
    		{
    			continue;
    		}
    	}
    }
    private void broadcastToClients(ByteBuffer pack, boolean excludeSender, int senderID)
    {
    	for(NodeSocket client : clientSockets.values())
    	{
    		try
    		{
    			if(client.getOwner() != null && excludeSender && client.getOwner().getUid().ID == senderID)
    				continue;
    			System.out.println("Broadcasting to client:" + client.getOwner().getUid().SID + ":" + client.getOwner().getUid().ID);
    			client.send(pack);
    		}
    		catch(IOException e)
    		{
    			continue;
    		}
    	}
    }
    private void broadcastToClients(Packet pack)
    {
    	try 
    	{
			broadcastToClients(pack.serialize(), false, 0);
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    }
	private void HandlePacket(ByteBuffer pack, NodeSocket sender)
	{
		byte id = pack.get(4);
		System.out.println("[Reactor Server] packet content:");
		while(pack.hasRemaining())
	    {
	        System.out.print(pack.get() + " ");
	    }
		pack.flip();
	    System.out.println();
		System.out.println("[Reactor Server]Packet recieved ID:" + id);
        System.out.println("[Reactor Server] ByteBuffer info: limit:" + pack.limit() + " Capacity:" + pack.capacity() + " Pos:" + pack.position());
		try 
		{
			if(sender.isHasPrivilege())
			{
				switch(id)
				{
					case 15: //finalize join
					{
						sender.elevateBuffer();
						FinalizeServerJoin packet = new FinalizeServerJoin(pack);
						sender.setOwner(new Server(new NodeUID(packet.getSid(), 0)));
						sendServerMembersList(sender);
						sender.getOwner().setReadyForCommunication(true);
						networkServers.put(packet.getSid(), sender);
						sidCounter = packet.getSid() + 1;
						serverCount++;
					} break;
					case 14: //server join response
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ServerJoinResponse packet = new ServerJoinResponse(pack);
						if(packet.getResponseType() == JoinResponseType.Accepted)
							System.out.println("Server ID:" + packet.getSenderId() + "at:" + sender.socketChannel.getRemoteAddress().toString() + " accepted your join request and proposed SID:" + packet.getProposedSid());
						else
							System.out.println("Server ID:" + packet.getSenderId() + sender.socketChannel.getRemoteAddress().toString() + " denied your join req");
						sender.getOwner().setUid(new NodeUID(packet.getSenderId(), 0));
						if(packet.getProposedSid() > serverID)
							serverID = packet.getProposedSid();
						stampCount++;
						networkServers.put(packet.getSenderId(), sender);
						serverCount++;
						
					} break;
					case 3: //ChatMediator(Server to Server)
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ChatMediator packet = new ChatMediator(pack);
						if(packet.getTtl() == 0)
						{
							ChatResponse response = new ChatResponse(ChatResponseType.MESSAGE_NOT_SENT, packet.getSender(), packet.getReciever());
							if(packet.getSender().SID == serverID)
								sender.send(response);
							else
							{
								routeToServer(packet.getSender().SID, response);
							}
						}
						else
						{
							Chat chatPacket = new Chat(packet.getSender(), packet.getMessage());
							try
							{
								if(packet.getReciever().SID == serverID)
								{
									clientSockets.get(packet.getReciever().ID).send(chatPacket);
									sender.send(new ChatResponse(ChatResponseType.MESSAGE_SENT, packet.getSender(), packet.getReciever()));
								}
								else
								{
									routeToServer(packet.getSender().SID, new ChatResponse(ChatResponseType.MESSAGE_NOT_SENT, packet.getSender(), packet.getReciever()));
								}
							}
							catch(IOException e)
							{
								sender.send(new ChatResponse(ChatResponseType.MESSAGE_NOT_SENT, packet.getSender(), packet.getReciever()));
							}
						}
					} break;
					case 5: //chatresponse
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ChatResponse packet = new ChatResponse(pack);
						if(packet.getRecieverUid().SID == serverID)
						{
							clientSockets.get(packet.getRecieverUid().ID).send(packet);
						}
					} break;
					case 7: //member list
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						MemberList packet = new MemberList(pack);
						System.out.println("Recieved member list from server with:" + packet.getMembers().size() + " members.");
						Server senderServer = (Server)(sender.getOwner());
						Iterator<Pair<NodeUID, String>> it = packet.getMembers().iterator();
						if(packet.isUpdate())
							while(it.hasNext())
							{
								Pair<NodeUID, String> next = it.next();
								senderServer.getClients().put(next.getLeft().ID, next.getRight());
							}
						else
						{
							while(it.hasNext())
								senderServer.getClients().remove(it.next().getLeft().ID);
							pack.flip();
							broadcastToClients(pack, false, 0);
						}
						syncCount++;
					} break;
					case 8://Acknowledgement
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						Acknowledgement packet = new Acknowledgement(pack);
						System.out.println("Server Requested Members List");
						switch(packet.getType())
						{
							case GET_MEMBERS:
							{
								sendServerMembersList(sender);
							} break;
							case QUIT:
							{
								removeServerLink(sender, true);
								sender.selectionKey.cancel();
								sender.socketChannel.close();
							} break;
						}
					} break;
					case 10://client leave (Server <-> Server, Server -> Client)
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ClientLeave packet = new ClientLeave(pack);
						System.out.println("Server:" + sender.getOwner().getUid().SID + " has declared that member:" + packet.getClientUID().SID+":"+packet.getClientUID().ID +" has left" );
						Server sendingServer = (Server) sender.getOwner();
						sendingServer.getClients().remove(packet.getClientUID().ID);
						pack.flip();
						broadcastToClients(pack, false, 0);
					} break;
					case 11://client join
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ClientJoin packet = new ClientJoin(pack);
						System.out.println("Server:" + sender.getOwner().getUid().SID + " has declared that member:" + packet.getClientUID().SID+":"+packet.getClientUID().ID + ":" + packet.getName() + " has joined" );
						Server sendingServer = (Server) sender.getOwner();
						sendingServer.getClients().put(packet.getClientUID().ID, packet.getName());
						pack.flip();
						broadcastToClients(pack, false, 0);
					} break;
					case 12:
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ReserveNameRequest packet = new ReserveNameRequest(pack);
						System.out.println("Server:" + sender.getOwner().getUid().SID + " requested name:" + packet.getName());
						ReserveNameResponse response;
						reservedNamesLock.lock();
						try
						{
							for(String s : reservedNames)
								if(s.equals(packet.getName()))
								{
									response = new ReserveNameResponse(packet.getName(), true);
									sender.send(response);
									System.out.println("denied request cause: name in local reservedlist");
									return;
								}
						}
						finally
						{
							reservedNamesLock.unlock();
						}
						pendingNameRequestsLock.lock();
						try
						{
							for(Pair<String, Integer> s : pendingNameRequests)
							{
								if(s.getLeft().equals(packet.getName()))
								{
									response = new ReserveNameResponse(packet.getName(), true);
									sender.send(response);
									System.out.println("denied request cause: name in local pendinglist");
									return;
								}
							}
						}
						finally
						{
							pendingNameRequestsLock.unlock();
						}
						response = new ReserveNameResponse(packet.getName(), false);
						sender.send(response);
					} break;
					case 13://reserve name response
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						ReserveNameResponse packet = new ReserveNameResponse(pack);
						pendingNameRequestsLock.lock();
						try
						{
							System.out.println("Server:" + sender.getOwner().getUid().SID + " responded with:" + (int)packet.getReserved() + " to name:" + packet.getName()  + "  Total:" + pendingNameRequests.size());
							for(Pair<String, Integer> request : pendingNameRequests)
								if(request.getLeft().equals(packet.getName()))
								{
									Integer nI = new Integer(request.getRight() + packet.getReserved());
									request.setR(nI);
									System.out.println("Updated request " + request.getLeft() + " with val:" + request.getRight());
									break;
								}
						}
						finally
						{
							pendingNameRequestsLock.unlock();	
						}
					} break;
				}
			}
			else
			{
				if(!ready)
					return;
				switch(id)
				{
					case 1: //JoinRequest
					{
						System.out.println("Incoming join request:");
						JoinRequest packet = new JoinRequest(pack);
						if(sender.getOwner() != null && sender.getOwner().isReadyForCommunication())
							return;
						if(packet.getRequestPrivilege() == 0)
						{
							System.out.println("A client request to join from:" + sender.socketChannel.getRemoteAddress() + " with name:" + packet.getRequestToken() + " has been recieved");
							JoinResponse response = new JoinResponse(JoinResponseType.NAME_TAKEN, null);
							System.out.println("Checking reserved names by local server");
							reservedNamesLock.lock();
							try
							{
								Iterator<String> it = reservedNames.iterator();
								while(it.hasNext())
								{
									if(it.next().equals(packet.getRequestToken()))
									{
										sender.send(response);
										return;
									}
								}
							}
							finally
							{
								reservedNamesLock.unlock();
							}
							System.out.println("local server names check passed");
							System.out.println("checking pending requests by local server");
							Pair<String, Integer> request;
							pendingNameRequestsLock.lock();
							try
							{
								Iterator<Pair<String, Integer>> pending = pendingNameRequests.iterator();
								while(pending.hasNext())
								{
									if(pending.next().getLeft().equals(packet.getRequestToken()))
									{
										sender.send(response);
										return;
									}
								}
								System.out.println("local pending requests check passed");
								System.out.println("Sending request to reserve name to other servers");
								request = new Pair<String, Integer>(packet.getRequestToken(), 0);
								pendingNameRequests.add(request);
								ReserveNameRequest reserveReq = new ReserveNameRequest(packet.getRequestToken());
								broadcastToServers(reserveReq);
							}
							finally
							{
								pendingNameRequestsLock.unlock();
							}
							int currentOtherServers = serverCount;
							System.out.println("Request sent");
							System.out.println("Waiting for other servers request server count: " + serverCount);
							boolean accepted = false;
							long start = System.currentTimeMillis();
							try
							{
								do
								{
									pendingNameRequestsLock.lock();
									try
									{
										System.out.println("Current val of name request:" + request.getRight());
										if(request.getRight() >= currentOtherServers)
										{
											accepted = true;
											reservedNamesLock.lock();
											try
											{
												reservedNames.add(packet.getRequestToken());
											}
											finally
											{
												reservedNamesLock.unlock();
											}
											break;
										}
										if(request.getRight() < 0)
										{
											sender.send(response);
											return;
										}
										pendingNameRequestsLock.unlock();
										Thread.sleep(50);
									} 
									catch (InterruptedException e)
									{
										response = new JoinResponse(JoinResponseType.DENIED, null);
										sender.send(response);
										return;
									}
									finally
									{
										if(pendingNameRequestsLock.isHeldByCurrentThread())
											pendingNameRequestsLock.unlock();
									}
								}
								while((System.currentTimeMillis() - start) / 1000 < 10);
								
							}
							finally
							{
								pendingNameRequestsLock.lock();
								try
								{
									pendingNameRequests.remove(request);
								}
								finally
								{
									pendingNameRequestsLock.unlock();
								}
							}
							System.out.println("Finished waiting for other servers. Response:" + accepted);
							if(accepted)
							{
								NodeUID uid = new NodeUID(serverID, generateID());
								JoinResponse aresponse = new JoinResponse(JoinResponseType.Accepted, uid);
								sender.setOwner(new Client(packet.getRequestToken(), uid));
								sender.getOwner().setReadyForCommunication(true);
								clientSockets.put(uid.ID, sender);
								sender.send(aresponse);
								ClientJoin joinPack = new ClientJoin(uid, packet.getRequestToken());
								broadcastToServers(joinPack);
								broadcastToClients(joinPack.serialize(), true, uid.ID);
							}
							else
							{
								response = new JoinResponse(JoinResponseType.DENIED, null);
								sender.send(response);
							}
						}
						else
						{
							ServerJoinResponse responsePack;
							System.out.println("A server is trying to obtain privileges with token:" + packet.getRequestToken());
							if(packet.getRequestToken().equals(Constants.SERVER_TOKEN))
							{
								responsePack = new ServerJoinResponse(JoinResponseType.Accepted, serverID, generateSID());
								sender.setHasPrivilege(true);
								System.out.println("Accepted server request with local sid:" + serverID + " proposed id:" + responsePack.getProposedSid());
							}
							else
							{
								System.out.println("Denied Request");
								responsePack = new ServerJoinResponse(JoinResponseType.DENIED, 0, 0);
							}
							sender.send(responsePack);
						}
						
					} break;
					case 2: //ChatRequest
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
								return;
						ChatRequest packet = new ChatRequest(pack);
						try
						{
							ChatResponse response;
							if(packet.getDestClient().SID == serverID)
							{
								NodeSocket receiver = clientSockets.get(packet.getDestClient().ID);
								System.out.println("Message from " + sender.getOwner().getUid().ID + " to " + packet.getDestClient().ID);
								if(receiver != null && packet.getDestClient().ID != sender.getOwner().getUid().ID)
								{
									Chat forward = new Chat(sender.getOwner().getUid(), packet.getMessage());
									response = new ChatResponse(ChatResponseType.MESSAGE_SENT, sender.getOwner().getUid(), packet.getDestClient());
									receiver.send(forward);
								}
								else
									response = new ChatResponse(ChatResponseType.DESTINATION_CLIENT_NOT_FOUND, sender.getOwner().getUid(), packet.getDestClient());
								sender.send(response);
							}
							else
							{
								ChatMediator chatmediator = new ChatMediator(sender.getOwner().getUid(), packet.getDestClient(), Constants.DEFAULT_TTL, packet.getMessage());
								routeToServer(packet.getDestClient().SID, chatmediator);
							}
						}
						catch(IOException e)
						{
							sender.send(new ChatResponse(ChatResponseType.DESTINATION_CLIENT_NOT_FOUND, sender.getOwner().getUid(), packet.getDestClient()));
						}
					} break;
					case 8://Acknowledgement
					{
						if(sender.getOwner() == null || !sender.getOwner().isReadyForCommunication())
							return;
						Acknowledgement packet = new Acknowledgement(pack);
						switch(packet.getType())
						{
							case GET_MEMBERS:
							{
								MemberList toSend;
								List<Pair<NodeUID, String>> membersList = new ArrayList<Pair<NodeUID,String>>();
								if(packet.getParam1() == 0)
								{
									for(NodeSocket sock : clientSockets.values())
									{
										if(!sock.getOwner().getUid().equals(sender.getOwner().getUid()))
											membersList.add(new Pair<NodeUID, String>(sock.getOwner().getUid(), ((Client)sock.getOwner()).getName()));
									}
									for(NodeSocket serverSocket : networkServers.values())
									{
										Server server = (Server) serverSocket.getOwner();
										for(Entry<Integer, String> entry : server.getClients().entrySet())
											membersList.add(new Pair<NodeUID, String>(new NodeUID(server.getUid().SID, entry.getKey()), entry.getValue()));
									}
								}
								else 
								{
									int requestServerId = packet.getParam2();
									if(requestServerId == serverID)
										for(NodeSocket sock : clientSockets.values())
										{
											if(!sock.getOwner().getUid().equals(sender.getOwner().getUid()))
												membersList.add(new Pair<NodeUID, String>(sock.getOwner().getUid(), ((Client)sock.getOwner()).getName()));
										}
									else
										for(NodeSocket serverSocket : networkServers.values())
										{
											Server server = (Server) serverSocket.getOwner();
											if(server.getUid().SID == requestServerId)
											{
												for(Entry<Integer, String> entry : server.getClients().entrySet())
													membersList.add(new Pair<NodeUID, String>(new NodeUID(server.getUid().SID, entry.getKey()), entry.getValue()));
												break;
											}
										}
								}
								toSend = new MemberList(membersList, true);
								sender.send(toSend);
							} break;
							case QUIT:
							{
								removeClientFromServer(sender);
								sender.selectionKey.cancel();
								sender.socketChannel.close();
							} break;
						}
					} break;
					default: System.out.println("Unknown Packet recieve ID:" + id);
				}
			}
		}
		catch (PacketLengthNotMatching | IOException e) 
		{
			System.out.println(e.getMessage());
			System.out.println("=============================");
			e.printStackTrace();
		}
	}
}