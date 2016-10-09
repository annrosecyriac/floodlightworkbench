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
	
	public enum ElectionState{CONNECT,ELECT,COORDINATE,SPIN};
	
	public ElectionState currentState = ElectionState.CONNECT;
	
	public AsyncElection(){
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Thread n1 = new Thread (network, "ZMQThread");
		n1.start();
		logger.info("Network majority: "+network.majority.toString());		
	}

	
	
	

}
