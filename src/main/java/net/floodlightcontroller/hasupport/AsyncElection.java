package net.floodlightcontroller.hasupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	public ZMQNode network;
	
	public AsyncElection(String serverPort, String clientPort, String controllerID){
		this.network = new ZMQNode(serverPort,clientPort,controllerID);
	}
	
	/**
	 * Receiving values from the other nodes during an election.
	 */
	private String IWon    = new String();
	
	/**
	 * Indicates who the current leader of the entire system is.
	 */
	private String leader  = new String();
	private String tempLeader = new String();
	
	private ElectionState currentState = ElectionState.CONNECT;
	
	/**
	 * The connectionDict gives us the status of the nodes which are currently ON/OFF,
	 * i.e. reachable by this node or unreachable.
	 */
	
	private Map<String, netState> connectionDict = new HashMap<String, netState>();
	
	/**
	 * Standardized sleep time for spinning in the rest state.
	 */
	
	private final Integer chill = new Integer(5);

	public String getLeader(){
		return this.leader;
	}
	
	private void sendHeartBeat(){
		// The Leader will send a HEARTBEAT message in the COORDINATE state
		// after the election and will expect a reply from a majority of
		// acceptors.
		HashSet<String> noSet = new HashSet<String>();
		try{
			
			for(HashMap.Entry<String, netState> entry: this.connectionDict.entrySet()){
				if( this.connectionDict.get(entry.getKey()).equals(netState.ON) ){
					
					// If the HeartBeat is rejected, populate the noSet.
					network.send( entry.getKey(), new String("HEARTBEAT ") + network.controllerID );
					String reply = network.recv(entry.getKey());
					
					if ( reply.equals(new String("NO")) ){
						noSet.add(reply);
					}
					// If we get an ACK, that's good.
					logger.info("[Election] Received HEARTBEAT ACK from "+entry.getKey().toString());
					
					if(noSet.size() >= network.majority){
						this.leader = null;
					}
				}
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
			for(HashMap.Entry<String, netState> entry: this.connectionDict.entrySet()){
				if( this.connectionDict.get(entry.getKey()).equals(netState.ON) ){
					
					network.send(entry.getKey(), new String("IWON"));
					String reply = network.recv(entry.getKey());
					logger.info("Received reply for IWON from: "+entry.getKey().toString());
					
				}
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
			
			for(HashMap.Entry<String, netState> entry: this.connectionDict.entrySet()){
				if( this.connectionDict.get(entry.getKey()).equals(netState.ON) ){
					
					network.send(entry.getKey(), new String("LEADER ") + network.controllerID );
					String reply = network.recv(entry.getKey());
					
					if ( reply.contains("LEADOK") ){
						String[] leadok = reply.split(" ");
						logger.info("[Election] Acceptor: "+leadok[1].toString());
						acceptors.add(leadok[1]);
					}
					
					if( acceptors.size() >= network.majority ){
						this.leader = network.controllerID;
						this.currentState = ElectionState.COORDINATE;
					} else {
						this.leader = null;
						this.currentState = ElectionState.ELECT;
					}
				}
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
			
			for(HashMap.Entry<String, netState> entry: this.connectionDict.entrySet()){
				if( this.connectionDict.get(entry.getKey()).equals(netState.ON) ){
					
					// If the leader is rejected, populate the noSet.
					network.send(entry.getKey(), new String("SETLEAD"));
					String reply = network.recv(entry.getKey());
					if ( reply.equals(new String("NO")) ){
						noSet.add(reply);
					}
					// If we get an ACK, that's good.
					logger.info("[Election] Received SETLEAD ACK from "+entry.getKey().toString());
					
					if(noSet.size() >= network.majority){
						this.leader = null;
					}
				}
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
			
			for(HashMap.Entry<String, netState> entry: this.connectionDict.entrySet()){
				if( this.connectionDict.get(entry.getKey()).equals(netState.ON) ){				
					
					network.send(entry.getKey(), new String("YOU?"));
					String reply = network.recv(entry.getKey());
					if (! reply.equals(new String("NO")) ){
						leaderSet.add(reply);
					} else if ( reply.equals(new String("NO")) ){
						logger.info("[Election] Check Leader: NO from "+entry.getKey().toString());
						continue;
					}
					
				}
			}
			
			logger.info("[Election] Leader Set: "+leaderSet.toString());
			
			// Remove blank objects from set, if any.
			if ( leaderSet.contains(new String("")) ){
				leaderSet.remove(new String(""));
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
				this.leader = null;
				logger.info("[Election] SPLIT BRAIN!!");
				logger.info("[Election] Current Leader is null");
			} else if ( leaderSet.size() < 1 ){
				this.leader = null;
				logger.info("[Election] Current Leader is null");
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
		String maxNode = new String("");
		
		// Generate list of total possible CIDs.
		for (Integer i = (network.totalRounds+1) ; i > 0 ; i--){
			nodes.add(i);
		}
		
		// Get the node whose CID is numerically greater && is ON.
		for (int i =0; i < nodes.size(); i++){
			if( this.connectionDict.get(nodes.get(i)).equals(netState.ON) ){
				maxNode = nodes.get(i).toString();
				break;
			}
		}
		
		// Edge case where you are the max node && you are ON.
		if( network.controllerID.compareTo(maxNode.toString()) > 0 ){
			if( this.connectionDict.get(maxNode).equals(netState.ON) ){
				maxNode = network.controllerID;
			}
		}
		
		// Check if Max Node is alive, and set it as leader if it is.
		try{
			if( network.controllerID.equals(maxNode) ){
				this.leader = maxNode;
			} else {
				for(int i=0; i < network.numberOfPulses; i++){
					network.send(network.netcontrollerID.get(maxNode), new String("PULSE"));
					String reply = network.recv(network.netcontrollerID.get(maxNode));
					if ( reply.equals(new String("ACK")) ){
						this.leader = maxNode;
					}
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
		if( this.connectionDict.size() < network.majority ){
			try {
				TimeUnit.SECONDS.sleep(this.chill);
			} catch (InterruptedException e) {
				logger.debug("[Election] Interrrupted in elect function!");
				e.printStackTrace();
			}
			return;
		}
		
		// Clear leader variables.
		this.tempLeader = null;
		this.leader	 	= null;
		
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
		if(! this.leader.equals(null) ){
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
		} else if ( this.leader.equals(null) ){
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
					this.connectionDict = network.getConnectDict();
					
					// Ensure that a majority of nodes have connected, otherwise demote state.
					if( this.connectionDict.size() < network.majority ){
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
					if( this.leader.equals(null) ){
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
					if( this.leader.equals(null) ){
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
			// TODO Auto-generated catch block
			logger.debug("[Election] Exception in cases!");
			ie.printStackTrace();
		} catch (Exception e) {
			logger.debug("[Election] Error in cases!");
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{
			Thread n1 = new Thread (network, "ZMQThread");
			
			n1.start();
			
			logger.info("[Election] Network majority: "+network.majority.toString());
			logger.info("[Election] Get netControllerIDStatic: "+network.getnetControllerIDStatic().toString());	
			this.cases();
			
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
