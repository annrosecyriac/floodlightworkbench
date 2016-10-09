package net.floodlightcontroller.hasupport;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.python.modules.math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

public class ZMQNode implements NetworkInterface, Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(ZMQNode.class);
	
	public final String controllerID;
	public final Integer serverPort;
	public final Integer clientPort;
	
	/**
	 * The server list holds the server port IDs of all present 
	 * connections.
	 * The connectSet is a set of nodes defined in the serverlist 
	 * which are to be connected
	 */

	public LinkedList<Integer> serverList = new LinkedList<Integer>();
	public LinkedList<Integer> allServerList = new LinkedList<Integer>();
	public HashSet<Integer>    connectSet = new HashSet<Integer>();
	
	/**
	 * Holds the connection state/ socket object for each of the client
	 * connections.
	 */
	
	public HashMap<Integer, ZMQ.Socket> socketDict = new HashMap<Integer, ZMQ.Socket>();
	public HashMap<Integer, netState> connectDict = new HashMap<Integer, netState>();
	
	/**
	 * Receiving values from the other nodes during an election.
	 */
	
	String rcvdVal = new String();
	String IWon    = new String();
	
	/**
	 * Indicates who the current leader of the entire system is.
	 */
	
	String leader  = new String();
	String tempLeader = new String();
	
	/**
	 * Standardized sleep times for retry connections, socket timeouts,
	 * number of pulses to send before expiring.
	 */
	
	public final Integer retryConnectionLatency   = new Integer(0);
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

	public ZMQNode(Integer serverPort, Integer clientPort, String controllerID){
		/**
		 * The port variables needed in order to start the
		 * backend and frontend of the queue device.
		 */
		this.serverPort = serverPort;
		this.clientPort = clientPort;
		this.controllerID = controllerID;
		preStart();
		this.totalRounds = new Integer(this.connectSet.size());
		logger.info("Total Rounds: "+this.totalRounds.toString());
		if(this.totalRounds >= 2){
			this.majority = new Integer((int) math.ceil(new Double(0.51 * this.connectSet.size())));
		} else {
			this.majority = new Integer(1);
		}
		logger.info("Other Servers: "+this.connectSet.toString()+"Majority: "+this.majority);
		
	}
	
	public void preStart(){
		String filename = "src/main/resources/server2.config";
		
		try{
			FileReader configFile = new FileReader(filename);
			String line = null;
			BufferedReader br = new BufferedReader(configFile);
			
			while((line = br.readLine()) != null){
				System.out.println(line);
				this.serverList.add(new Integer(line.trim()));
				this.allServerList.add(new Integer(line.trim()));
			}
			
			this.serverList.remove(this.clientPort);
			this.connectSet = new HashSet<Integer>(this.serverList);
			
			br.close();
			configFile.close();
			
		} catch (FileNotFoundException e){
			logger.debug("This file was not found! Please place the server config file in the right location.");	
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public Boolean send(Integer clientPort, String message) {
		// TODO Auto-generated method stub
		ZMQ.Socket clientSock = socketDict.get(clientPort);
		try{
			clientSock.send(message);
			return Boolean.TRUE;
		} catch(ZMQException e){
			clientSock.setLinger(0);
			clientSock.close();
			logger.debug("Send Failed: "+message+" not sent through port: "+clientPort);
			e.printStackTrace();
			return Boolean.FALSE;
		}
		
	}

	@Override
	public String recv(Integer receivingPort) {
		// TODO Auto-generated method stub
		ZMQ.Socket clientSock = socketDict.get(receivingPort);
		try{
			byte[] msg = clientSock.recv(0);
			return new String(msg);
		} catch(ZMQException e){
			clientSock.setLinger(0);
			clientSock.close();
			logger.debug("Recv Failed on port: "+receivingPort);
			e.printStackTrace();
			return "";
		}
		
	}

	@Override
	public Map<Integer, netState> connectClients(Set<Integer> connectSet) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, netState> checkForNewConnections(Map<Integer, netState> connectDict) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, netState> expireOldConnections(Map<Integer, netState> connectDict) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void blockUntilConnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		logger.info("Server List: "+this.serverList.toString());
	}

}
