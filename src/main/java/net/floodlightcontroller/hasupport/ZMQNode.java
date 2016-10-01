package net.floodlightcontroller.hasupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.zeromq.ZMQ;

public class ZMQNode implements NetworkInterface {
	
	public final String controllerID;
	public final Integer serverPort;
	public final Integer clientPort;
	
	public HashMap<Integer, ZMQ.Socket> socketDict = new HashMap<Integer, ZMQ.Socket>();
	
	public enum ElectionState{CONNECT,ELECT,COORDINATE,SPIN};
	
	public ElectionState currentState = ElectionState.CONNECT;
	
	public ZMQNode(Integer serverPort, Integer clientPort, String controllerID){
		this.serverPort = serverPort;
		this.clientPort = clientPort;
		this.controllerID = controllerID;	
	}
	
	@Override
	public Boolean send(Integer clientPort, String message) {
		// TODO Auto-generated method stub
		
		return null;
	}

	@Override
	public String recv(Integer receivingPort) {
		// TODO Auto-generated method stub
		return null;
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

}
