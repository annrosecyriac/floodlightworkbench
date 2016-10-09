package net.floodlightcontroller.hasupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

/**
 * The Election class 
 * @author bsriniva
 *
 */

public class AsyncElection implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(AsyncElection.class);
	public ZMQNode network = new ZMQNode(4242,5252,"1");
	
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
	
	public enum ElectionState{CONNECT,ELECT,COORDINATE,SPIN};
	public ElectionState currentState = ElectionState.CONNECT;
	
	public AsyncElection(){
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{
			Thread n1 = new Thread (network, "ZMQThread");
			n1.start();
			logger.info("Network majority: "+network.majority.toString());
			network.getConnectDict();
			network.blockUntilConnected();
			n1.join();
		} catch (InterruptedException ie){
			logger.info("[Election] Was interrrupted! "+ie.toString());
			ie.printStackTrace();
		} catch (Exception e){
			logger.info("[Election] Was interrrupted! "+e.toString());
			e.printStackTrace();
		}
	}

}
