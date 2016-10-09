package net.floodlightcontroller.hasupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.zeromq.ZMQ;

/**
 * The Election class 
 * @author bsriniva
 *
 */

public class AsyncElection {
	
	public ZMQNode network = new ZMQNode(4242,5252,"1");
	
	public enum ElectionState{CONNECT,ELECT,COORDINATE,SPIN};
	
	public ElectionState currentState = ElectionState.CONNECT;
	
	public AsyncElection(){
		
	}

	
	
	

}
