package net.floodlightcontroller.hasupport;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.python.modules.math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 * The Network Node
 * @author Bhargav Srinivasan
 */

public class ZMQNode implements NetworkInterface, Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(ZMQNode.class);
	
	private ZMQ.Context zmqcontext = ZMQ.context(10);
	
	public final String controllerID;
	public final String serverPort;
	public final String clientPort;
	
	/**
	 * The server list holds the server port IDs of all present 
	 * connections.
	 * The connectSet is a set of nodes defined in the serverlist 
	 * which are to be connected
	 */

	public LinkedList<String> serverList = new LinkedList<String>();
	public LinkedList<String> allServerList = new LinkedList<String>();
	public HashSet<String>    connectSet = new HashSet<String>();
	
	private final String pulse  = new String("PULSE");
	private final String ack    = new String("ACK");
	
	/**
	 * Holds the connection state/ socket object for each of the client
	 * connections.
	 */
	
	public HashMap<String, ZMQ.Socket>  socketDict             = new HashMap<String, ZMQ.Socket>();
	public HashMap<String, netState>    connectDict            = new HashMap<String, netState>();
	public HashMap<String, String>      controllerIDNetStatic  = new HashMap<String, String>();
	public HashMap<String, Integer>     netcontrollerIDStatic  = new HashMap<String, Integer>();
	
	/**
	 * Standardized sleep times for socket timeouts,
	 * number of pulses to send before expiring.
	 */
	
	// Decide the socket timeout value based on how fast you think the leader should
	// respond and also how far apart the actual nodes are placed. If you are trying
	// to communicate with servers far away, then anything upto 10s would be a good value.
	
	public final Integer socketTimeout 		      = new Integer(500);
	public final Integer numberOfPulses		      = new Integer(1);
	public final Integer chill				      = new Integer(5);
	
	/**
	 * Majority is a variable that holds what % of servers need to be
	 * active in order for the election to start happening. Total rounds is
	 * the number of expected failures which is set to len(serverList) beacuse
	 * we assume that any/every node can fail.
	 */
	public final Integer majority;
	public final Integer totalRounds;

	
	
	
	/**
	 * Constructor needs both the backend and frontend ports and the serverList
	 * file which specifies a port number for each connected client. 
	 * @param serverPort
	 * @param clientPort
	 * @param controllerID
	 */

	public ZMQNode(String serverPort, String clientPort, String controllerID){
		/**
		 * The port variables needed in order to start the
		 * backend and frontend of the queue device.
		 */
		this.serverPort = serverPort.toString();
		this.clientPort = clientPort.toString();
		this.controllerID = controllerID;
		preStart();
		this.totalRounds = new Integer(this.connectSet.size());
		logger.debug("Total Rounds: "+this.totalRounds.toString());
		if(this.totalRounds >= 2){
			this.majority = new Integer((int) math.ceil(new Double(0.51 * this.connectSet.size())));
		} else {
			this.majority = new Integer(1);
		}
		logger.debug("Other Servers: "+this.connectSet.toString()+"Majority: "+this.majority);
		
		
	}
	
	public void preStart(){
		String filename = "src/main/resources/server.config";
		
		try{
			FileReader configFile = new FileReader(filename);
			String line = null;
			BufferedReader br = new BufferedReader(configFile);
			
			Integer cidIter = new Integer(1);
			while((line = br.readLine()) != null){
				this.serverList.add(new String(line.trim()));
				this.allServerList.add(new String(line.trim()));
				this.netcontrollerIDStatic.put(new String(line.trim()), cidIter);
				this.controllerIDNetStatic.put(cidIter.toString(), new String(line.trim()) );
				cidIter += 1;
			}
			
			this.serverList.remove(this.clientPort);
			this.connectSet = new HashSet<String>(this.serverList);
			
			br.close();
			configFile.close();
			
		} catch (FileNotFoundException e){
			logger.debug("This file was not found! Please place the server config file in the right location.");	
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	

	
	@Override
	public Boolean send(String clientPort, String message) {
		// TODO Auto-generated method stub
		if( message.equals(null) ) {
			return Boolean.FALSE;
		}
		
		ZMQ.Socket clientSock = socketDict.get(clientPort);
		try{
			clientSock.send(message);
			return Boolean.TRUE;
		} catch(ZMQException ze){
			if(clientSock != null){
				clientSock.setLinger(0);
				clientSock.close();
			}
			logger.info("Send Failed: "+message+" not sent through port: "+clientPort.toString());
			ze.printStackTrace();
			return Boolean.FALSE;
		} catch(Exception e){
			if(clientSock != null){
				clientSock.setLinger(0);
				clientSock.close();
			}
			logger.info("Send Failed: "+message+" not sent through port: "+clientPort.toString());
			e.printStackTrace();
			return Boolean.FALSE;
		}
	}

	@Override
	public String recv(String receivingPort) {
		// TODO Auto-generated method stub
		ZMQ.Socket clientSock = socketDict.get(receivingPort);
		try{
			byte[] msg = clientSock.recv(0);
			return new String(msg).trim();
		} catch(ZMQException ze){
			if(clientSock != null){
				clientSock.setLinger(0);
				clientSock.close();
			}
			logger.info("Recv Failed on port: "+receivingPort.toString());
			ze.printStackTrace();
			return "";
		} catch (Exception e){
			if(clientSock != null){
				clientSock.setLinger(0);
				clientSock.close();
			}
			logger.info("Recv Failed on port: "+receivingPort.toString());
			e.printStackTrace();
			return "";
		}
		
	}
	
	public void doConnect(){
		
		HashSet<String> diffSet 		= new HashSet<String>();
		HashSet<String> connectedNodes  = new HashSet<String>();
		
		for(HashMap.Entry<String, ZMQ.Socket> entry: this.socketDict.entrySet()){
			connectedNodes.add(entry.getKey());
		}
		
		diffSet.addAll(this.connectSet);
		diffSet.removeAll(connectedNodes);
		
		logger.debug("[Node] New connections to look for (ConnectSet - Connected): "+diffSet.toString());
		
		
		// Try connecting to all nodes that are in the diffSet and store the 
		// successful ones in the  socketDict.
		for (String client: diffSet){
			ZMQ.Socket clientSock = zmqcontext.socket(ZMQ.REQ);
			try{
				clientSock.connect("tcp://"+client.toString());
				//Set the socket timeouts for the current socket.
				clientSock.setReceiveTimeOut(this.socketTimeout);
				clientSock.setSendTimeOut(this.socketTimeout);
				
				clientSock.send(pulse);
				byte[] rep = clientSock.recv();
				String reply = new String(rep);
				
				if( reply.equals(ack) ){
					logger.debug("[Node] Client: "+client.toString()+"Client Sock: "+clientSock.toString());
					if (!socketDict.containsKey(client)){
						socketDict.put(client, clientSock);
					} else {
						logger.debug("[Node] This socket already exists, refreshing: "+client.toString());
						this.socketDict.get(client).setLinger(0);
						this.socketDict.get(client).close();
						this.socketDict.remove(client);
						this.socketDict.put(client, clientSock);
					}
				} else {
					logger.debug("[Node] Received bad reply: "+client.toString());
					clientSock.setLinger(0);
					clientSock.close();
					logger.debug("[Node] Closed Socket"+client.toString());		
				}
				
			} catch(NullPointerException ne){
				logger.debug("[Node] ConnectClients: Reply had a null value from: "+client.toString());
				if(clientSock != null){
					clientSock.setLinger(0);
					clientSock.close();
				}
				//ne.printStackTrace();
			}  catch (ZMQException ze){
				if(clientSock != null){
					clientSock.setLinger(0);
					clientSock.close();
				}
				logger.debug("[Node] ConnectClients errored out: "+client.toString());
				ze.printStackTrace();
			} catch (Exception e){
				if(clientSock != null){
					clientSock.setLinger(0);
					clientSock.close();
				}
				logger.debug("[Node] ConnectClients errored out: "+client.toString());
				e.printStackTrace();
			} 
			
		}
		
		return;
		
	}

	@Override
	public Map<String, netState> connectClients() {
		// TODO Auto-generated method stub
		logger.debug("[Node] To Connect: "+this.connectSet.toString());
		logger.debug("[Node] Connected: "+this.socketDict.keySet().toString());
		
		doConnect();
		
		//Delete the already connected connections from the ToConnect Set.
		for(HashMap.Entry<String, ZMQ.Socket> entry: this.socketDict.entrySet()){
			if(this.connectSet.contains(entry.getKey())){
				this.connectSet.remove(entry.getKey());
				logger.debug("Discarding already connected client: "+entry.getKey().toString());
			}
		}	
		updateConnectDict();
		return (Map<String, netState>) Collections.unmodifiableMap(this.connectDict);
	}

	@Override
	public Map<String, netState> checkForNewConnections() {
		// TODO Auto-generated method stub
		expireOldConnections();
		
		this.connectSet = new HashSet<String> (this.serverList);
		
		doConnect();
		
		updateConnectDict();
		return (Map<String, netState>) Collections.unmodifiableMap(this.connectDict);
	}

	@Override
	public Map<String, netState> expireOldConnections() {
		// TODO Auto-generated method stub
		logger.debug("Expiring old connections...");
		HashMap<String, ZMQ.Socket> delmark = new HashMap<String, ZMQ.Socket>();
		
		for(HashMap.Entry<String, ZMQ.Socket> entry: this.socketDict.entrySet()){
			try{
				byte[] rep = null;
				for(int i=0; i <= this.numberOfPulses; i++){
					entry.getValue().send(pulse);
					rep = entry.getValue().recv();
				}
				String reply = new String(rep);
				
				if (! reply.equals(ack) ) {
					logger.debug("[Node] Closing stale connection: "+entry.getKey().toString());
					entry.getValue().setLinger(0);
					entry.getValue().close();
					delmark.put(entry.getKey(),entry.getValue());
				}
				
			} catch(NullPointerException ne){
				logger.debug("[Node] Expire: Reply had a null value: "+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				//ne.printStackTrace();
			} catch(ZMQException ze){
				logger.debug("[Node] Expire: ZMQ socket error: "+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				//ze.printStackTrace();
			} catch (Exception e){
				logger.debug("[Node] Expire: Exception! : "+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				e.printStackTrace();
			}
		}
		
		//Pop out all the expired connections from socketDict.
		try{
			for (HashMap.Entry<String, ZMQ.Socket> entry: delmark.entrySet()){
				this.socketDict.remove(entry.getKey());
				entry.getValue().setLinger(0);
				entry.getValue().close();
			}
		} catch (Exception e) {
			logger.debug("[ZMQNode] Error in expireOldConnections, while deleting socket");
			e.printStackTrace();
		}
		
		logger.debug("Expired old connections.");
		
		updateConnectDict();
		return (Map<String, netState>) Collections.unmodifiableMap(this.connectDict);
	}

	/**
	 * Will first expire all connections in the socketDict and keep spinning until,
	 * > majority % nodes from the connectSet get connected.
	 */
	@Override
	public ElectionState blockUntilConnected() {
		// TODO Auto-generated method stub
		this.connectSet = new HashSet<String> (this.serverList);
		HashMap<String, ZMQ.Socket> delmark = new HashMap<String, ZMQ.Socket>();
		
		for (HashMap.Entry<String,ZMQ.Socket> entry: this.socketDict.entrySet()){
			try{
				logger.debug("[Node] Closing connection: "+entry.getKey().toString());
				entry.getValue().setLinger(0);
				entry.getValue().close();
				delmark.put(entry.getKey(), entry.getValue());
				
			} catch(NullPointerException ne){
				logger.debug("[Node] BlockUntil: Reply had a null value"+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				//ne.printStackTrace();
			} catch (ZMQException ze){
				logger.debug("[Node] Error closing connection: "+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				//ze.printStackTrace();
			} catch (Exception e){
				logger.debug("[Node] Error closing connection: "+entry.getKey().toString());
				delmark.put(entry.getKey(),entry.getValue());
				e.printStackTrace();
			}
		}
		
		for (HashMap.Entry<String, ZMQ.Socket> entry: delmark.entrySet()){
			this.socketDict.remove(entry.getKey());
		}
		
		this.socketDict = new HashMap<String, ZMQ.Socket>();
		
		while (this.socketDict.size() < this.majority){
			try {
				logger.debug("[Node] BlockUntil: Trying to connect...");
				this.connectClients();
			} catch (Exception e){
				logger.debug("[Node] BlockUntil errored out: "+e.toString());
				e.printStackTrace();
			}
		}
		
		updateConnectDict();
		return ElectionState.ELECT;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{
			logger.debug("Server List: "+this.serverList.toString());
			Thread t1 = new Thread(new QueueDevice(this.serverPort,this.clientPort), "QueueDeviceThread");
			t1.start();
			t1.join();
		} catch (InterruptedException ie){
			logger.debug("Queue Device was interrupted! "+ie.toString());
			ie.printStackTrace();
		} catch (Exception e){
			logger.debug("Queue Device encountered an exception! "+e.toString());
			e.printStackTrace();
		}
	}
	
	@Override
	public Map<String, netState> getConnectDict(){
		return (Map<String, netState>) Collections.unmodifiableMap(this.connectDict);
	}
	
	public Map<String, Integer> getnetControllerIDStatic(){
		return (Map<String, Integer>) Collections.unmodifiableMap(this.netcontrollerIDStatic);
	}

	/**
	 * This function translates socketDict into connectDict, in order to 
	 * preserve the abstraction of the underlying network from the actual 
	 * election algorithm.
	 */
	
	@Override
	public void updateConnectDict() {
		// TODO Auto-generated method stub
		this.connectDict     = new HashMap<String, netState>();
		
		for (String seten: this.connectSet){
			this.connectDict.put(seten, netState.OFF);
		}
		
		for (HashMap.Entry<String, ZMQ.Socket> entry: this.socketDict.entrySet()){
			this.connectDict.put(entry.getKey(), netState.ON);
		}
		
		logger.debug("Connect Dict: "+this.connectDict.toString());
		logger.debug("Socket Dict: "+this.socketDict.toString());
		
		return;
		
		
	}

}
