package net.floodlightcontroller.hasupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.hasupport.NetworkInterface.ElectionState;
import net.floodlightcontroller.hasupport.NetworkInterface.netState;

/**
 * The Election class 
 * 
 * TODO: LEADER shouldn't be null, should be enum UNSET
 * before election.
 * Pre-declare all messages as private final strings.
 * 
 * @author Bhargav Srinivasan
 */

public class AsyncElection implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(AsyncElection.class);
	private ZMQNode network;
	
	private final String serverPort;
	ArrayList<Thread> serverThreads = new ArrayList<Thread>();
	private final String controllerID;
	
	public AsyncElection(String serverPort, String clientPort, String controllerID){
		this.network       = new ZMQNode(serverPort,clientPort,controllerID);
		this.serverPort    = serverPort;
		this.controllerID  = controllerID;
	}
	
	/**
	 * Indicates who the current leader of the entire system is.
	 */
	private String leader             = new String("none");
	private String tempLeader         = new String("none");
	private final String none         = new String("none");
	private final String ack 		  = new String("ACK");
	
	private ElectionState currentState = ElectionState.CONNECT;

	
	/**
	 * Standardized sleep time for spinning in the rest state.
	 */
	
	private final Integer chill = new Integer(5);

	public String getLeader(){
		return this.leader;
	}
	
	public String gettempLeader(){
		return this.tempLeader;
	}
	
	public void setLeader(String leader){
		synchronized (this.leader) { 
			this.leader = leader;
		}
		return;
	}
	
	public void setTempLeader(String tempLeader){
		synchronized(this.tempLeader) {
			this.tempLeader = tempLeader;
		}
		return;
	}
	
	/**
	 * Server start
	 */
	public void joinServerThreads(){
		try{
			for (int i=0; i < serverThreads.size(); i++){
					serverThreads.get(i).join(); 
			}
		} catch (InterruptedException ie){
			logger.info("[Node] Was interrrupted! "+ie.toString());
			ie.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
			logger.info("[Node] Exception in joinServerThreads");
		}
		
	}
	
	public void startServers(){
		
		Integer noServers = new Integer(0);
		ZMQServer serverTh = new ZMQServer(this.serverPort, this, this.controllerID);
		
		if (network.totalRounds <= 1){
			noServers = 1;
		} else {
			// TODO CHANGE TO LOG10
			noServers = (network.totalRounds/3);
		}
		
		if(noServers <= 1){
			noServers =1;
		}
		
		try{
			for (Integer i=0; i < noServers; i++){
				Thread tx = new Thread (serverTh, "ZMQServers");
				logger.info("Starting server "+i.toString()+"...");
				tx.start();
				serverThreads.add(tx);
			}
		} catch (Exception e){
			logger.info("[Node] startServers was interrrupted! "+e.toString());
			e.printStackTrace();
		}
		
	}
	
	private void sendHeartBeat(){
		// The Leader will send a HEARTBEAT message in the COORDINATE state
		// after the election and will expect a reply from a majority of
		// acceptors.
		HashSet<String> noSet = new HashSet<String>();
		try{
			
			for(HashMap.Entry<String, netState> entry: network.connectDict.entrySet()){
				if( network.connectDict.get(entry.getKey()).equals(netState.ON) ){
					
					// If the HeartBeat is rejected, populate the noSet.
					network.send( entry.getKey(), new String("HEARTBEAT ") + network.controllerID );
					String reply = network.recv(entry.getKey());
					
					if ( reply.equals(new String("NO")) ){
						noSet.add(reply);
					}
					// If we get an ACK, that's good.
					logger.info("[Election] Received HEARTBEAT ACK from "+entry.getKey().toString());
				}
			}
			
			if(noSet.size() >= network.majority){
				this.leader = none;
			}
			
			return;
			
		} catch (Exception e){
			logger.debug("[Election] Error in sendHeartBeat!");
			e.printStackTrace();
		}
		
	}
	
	private void sendIWon(){
		// The winner of the election, or the largest node that is currently active
		// in the network sends an "IWON" message in order to initiate the three phase
		// commit to set itself as the leader of the network.
		try{
			Set<String> reply = new HashSet<String>();
			for(HashMap.Entry<String, netState> entry: network.connectDict.entrySet()){
				if( network.connectDict.get(entry.getKey()).equals(netState.ON) ){
					
					network.send(entry.getKey(), new String("IWON ") + this.controllerID );
					reply.add( network.recv(entry.getKey()) );
					logger.info("Received reply for IWON from: "+entry.getKey().toString() + reply.toString());
					
				}
			}
			
			if( reply.contains(ack) ) {
				setTempLeader(this.controllerID);
			}
			
			return;
					
		} catch (Exception e){
			logger.debug("[Election] Error in sendIWon!");
			e.printStackTrace();
		}
		
	}
	
	private void sendLeaderMsg(){
		// Send a "LEADER" message to all nodes and try to receive "LEADOK"
		// messages from them. If count("LEADOK") > majority, then you have
		// won the election and hence become the leader.
		// Phase 2 of the three phase commit.
		
		HashSet<String> acceptors = new HashSet<String>();
		try{
			
			for(HashMap.Entry<String, netState> entry: network.connectDict.entrySet()){
				if( network.connectDict.get(entry.getKey()).equals(netState.ON) ){
					
					network.send(entry.getKey(), new String("LEADER ") + network.controllerID );
					String reply = network.recv(entry.getKey());
					if( reply.equals(new String("LEADOK")) ){
						acceptors.add(entry.getKey());
					}
				}
				
			}
			
			if( acceptors.size() >= network.majority ){
				logger.info("[Election sendLeaderMsg] Accepted leader: "+this.controllerID+" Majority: "+network.majority+"Acceptors: "+acceptors.toString());
				this.leader = network.controllerID;
				this.currentState = ElectionState.COORDINATE;
			} else {
				logger.info("[Election sendLeaderMsg] Did not accept leader: "+this.controllerID+" Majority: "+network.majority+"Acceptors: "+acceptors.toString());
				this.leader = none;
				this.currentState = ElectionState.ELECT;
			}
			
			return;
			
		} catch (Exception e){
			logger.debug("[Election] Error in sendLeaderMsg!");
			e.printStackTrace();
		}
		
	}
	
	private void setAsLeader(){
		// The leader will set itself as leader during each COORDINATE
		// state loop, to ensure that all nodes see it as the leader.
		// Phase 3 of the three phase commit.
		
		HashSet<String> noSet = new HashSet<String>();
		try{
			
			for(HashMap.Entry<String, netState> entry: network.connectDict.entrySet()){
				if( network.connectDict.get(entry.getKey()).equals(netState.ON) ){
					
					// If the leader is rejected, populate the noSet.
					network.send(entry.getKey(), new String("SETLEAD ") + network.controllerID);
					String reply = network.recv(entry.getKey());
					
					if ( reply.equals(new String("NO")) ){
						noSet.add(reply);
					}
					
					// If we get an ACK, that's good.
					logger.info("[Election] Received SETLEAD ACK from "+entry.getKey().toString());
				}
			}
			
			if(noSet.size() >= network.majority){
				this.leader = none;
			}
			
			return;
			
		} catch (Exception e){
			logger.debug("[Election] Error in setAsLeader!");
			e.printStackTrace();
		}
		
	}
	
	private void checkForLeader(){
		// Ask each node if they are the leader, you should get an
		// ACK from only one of them, if not, then reset the leader.
		HashSet<String> leaderSet = new HashSet<String>();
		try{
			
			for(HashMap.Entry<String, netState> entry: network.connectDict.entrySet()){
				if( network.connectDict.get(entry.getKey()).equals(netState.ON) ){				
					
					network.send(entry.getKey(), new String("YOU?"));
					String reply = network.recv(entry.getKey());
					if (! reply.equals(new String("NO")) ){
						leaderSet.add(reply);
					} else if ( reply.equals(new String("NO")) ){
						logger.info("[Election] Check Leader: " + reply +" from "+entry.getKey().toString());
						continue;
					}
					
				}
			}
			
			logger.info("[Election checkForLeader] Leader Set: "+leaderSet.toString());
			
			// Remove blank objects from set, if any.
			if ( leaderSet.contains(new String("")) ){
				leaderSet.remove(new String(""));
			}
			
			// Remove none from set, if any.
			if ( leaderSet.contains(none) ){
				leaderSet.remove(none);
			}
			
			// Remove null objects from set, if any.
			if( leaderSet.contains(null) ){
				logger.info("[Election] Leader Set contains null");
				leaderSet.remove(null);
			}
			
			
			if( leaderSet.size() == 1 ){
				 this.leader = leaderSet.stream()
										.findFirst().get();
			} else if ( leaderSet.size() > 1 ){
				this.leader = none;
				logger.info("[Election checkForLeader] SPLIT BRAIN!!");
				logger.info("[Election checkForLeader] Current Leader is null");
			} else if ( leaderSet.size() < 1 ){
				this.leader = none;
				logger.info("[Election checkForLeader] Current Leader is null "+ this.leader.toString() );
			}

			
			return;
			
		} catch (Exception e){
			logger.debug("[Election] Error in CheckForLeader");
			e.printStackTrace();
		}
		
	}
	
	private void electionLogic(){
		// List of controllerIDs of all nodes.
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		
		// Generate list of total possible CIDs.
		for (Integer i = (network.totalRounds+1) ; i > 0 ; i--){
			nodes.add(i);
		}
		
		Integer maxNode = new Integer(Collections.max(nodes));
		
		logger.info(" +++++++++ [Election Logic] Nodes participating: "+nodes.toString());
		
		// TODO Something weird is going on here...
		
		// Edge case where you are the max node && you are ON.
		if( network.controllerID.compareTo(maxNode.toString()) == 0 ){
				this.leader = maxNode.toString();
				logger.info(" +++++++ [Election Logic] I am the Max Node, I am the leader: ");
				return;
		}
		
		// Get the node whose CID is numerically greater && is ON.
		// TODO clean this up!!
		try{
			for (int i =0; i < nodes.size(); i++){
				if( network.connectDict.get(  network.controllerIDNetStatic.get( nodes.get(i) )  ).equals(netState.ON) ){
					maxNode = nodes.get(i);
					logger.info(" +++++++ [Election Logic] Max Node: "+maxNode.toString());
					break;
				}
			}
		} catch (Exception e) {
			logger.info("I am the max node.");
		}
		
		String maxNodePort = network.controllerIDNetStatic.get(maxNode).toString();
		
		// Check if Max Node is alive, and set it as leader if it is.
		try{
			
			for(int i=0; i < network.numberOfPulses; i++){
				
				network.send(maxNodePort, new String("PULSE"));
				String reply = network.recv(maxNodePort);
				
				if ( reply.equals(new String("ACK")) ){
					this.leader = maxNode.toString();
				}
			}
			
		} catch (Exception e) {
			logger.debug("[Election] Error in electionLogic!");
			e.printStackTrace();
		}
		
		return;
		
	}
	
	private void elect(){
		// Max Set election:
		// All nodes will pick the max CID which they see in the network,
		// any scenario wherein two different leaders might be picked gets resolved
		// using the checkForLeader function.
		
		// Ensure that majority are still connected.
		if( network.connectDict.size() < network.majority ){
			try {
				TimeUnit.SECONDS.sleep(this.chill);
			} catch (InterruptedException e) {
				logger.debug("[Election] Interrrupted in elect function!");
				e.printStackTrace();
			}
			return;
		}
		
		// Clear leader variables.
		this.tempLeader = none;
		this.leader	 	= none;
		
		// Check if actually in elect state
		if (!(this.currentState == ElectionState.ELECT)){
			return;
		}
		
		// Node joins AFTER election:
		// To check if a node joined after election, i.e.
		// a leader is already present. Run the checkForLeader
		// function and if it returns a leader then accept the 
		// existing leader and go to the SPIN state.
		
		this.checkForLeader();
		
		// If a leader has already been set, exit election state 
		// and SPIN.
		if(! this.leader.equals(none) ){
			this.currentState = ElectionState.SPIN;
			return;
		}
		
		// End of Node joins AFTER election.
		
		// Actual election logic.
		this.electionLogic();
		
		if( this.leader.equals(network.controllerID) ){
			logger.info("[ELection] I WON THE ELECTION!");
			this.sendIWon();
			this.sendLeaderMsg();
			this.setAsLeader();
		} else if ( this.leader.equals(none) ){
			this.currentState = ElectionState.ELECT;
		} else {
			this.currentState = ElectionState.SPIN;
		}
		
		// End of Actual Election logic.
	}
	
	private void cases(){
		try {
		while(Boolean.TRUE){
			logger.info("Current State: "+currentState.toString());
			switch(currentState){
				
				case CONNECT:
					
					// Block until a majority of the servers have connected.
					this.currentState =  network.blockUntilConnected();
					
					// Majority of the servers have connected, moving on to elect.
					break;
					
				case ELECT:
					
					// Check for new nodes to connect to, and refresh the socket connections.
					network.checkForNewConnections();
					
					// Ensure that a majority of nodes have connected, otherwise demote state.
					if( network.connectDict.size() < network.majority ){
						this.currentState = ElectionState.CONNECT;
						break;
					}
					
					//Start the election if a majority of nodes have connected.
					this.elect();
					
					// Once the election is done and the leader is confirmed,
					// proceed to the COORDINATE or FOLLOW state.
					break;
					
				case SPIN:
					
					// This is the resting state after the election.
					network.checkForNewConnections();
					
					if( this.leader.equals(none) ){
						this.currentState = ElectionState.ELECT;
						break;
					}
					
					// This is the follower state, currently there is a leader in the network.
					logger.info("+++++++++++++++ [FOLLOWER] Leader is set to: "+this.leader.toString());
					
					// Check For Leader: This function ensures that there is only one leader set for
					// the entire network. None or multiple leaders causes it to set the currentState to ELECT.
					this.checkForLeader();
					TimeUnit.SECONDS.sleep(this.chill.intValue());
					
					break;
					
				case COORDINATE:
					
					// This is the resting state of the leader after the election.
					network.checkForNewConnections();
					
					if( this.leader.equals(none) ){
						this.currentState = ElectionState.ELECT;
						break;
					}
					
					// This is the follower state, currently I am the leader of the network.
					logger.info("+++++++++++++++ [LEADER] Leader is set to: "+this.leader.toString());
					
					// Keep the leader in coordinate state.
					this.sendIWon();
					this.sendLeaderMsg();
					this.setAsLeader();
					
					// Keep sending a heartbeat message, and receive a majority of acceptors,
					// otherwise go to the elect state.
					this.sendHeartBeat();
					
					TimeUnit.SECONDS.sleep(this.chill.intValue());
					
					break;
					
			}
			
		}
		
		} catch (InterruptedException ie) {
			logger.debug("[Election] Exception in cases!");
			ie.printStackTrace();
		} catch (Exception e) {
			logger.debug("[Election] Error in cases!");
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		try{
			Thread n1 = new Thread (network, "ZMQThread");
			
			n1.start();
			startServers();
			
			logger.info("[Election] Network majority: "+network.majority.toString());
			logger.info("[Election] Get netControllerIDStatic: "+network.getnetControllerIDStatic().toString());	
			this.cases();
			
			joinServerThreads();
			n1.join();
			
		} catch (InterruptedException ie){
			logger.info("[Network] Was interrrupted! "+ie.toString());
			ie.printStackTrace();
		} catch (Exception e){
			logger.info("[Network] Was interrrupted! "+e.toString());
			e.printStackTrace();
		}
	}

}
