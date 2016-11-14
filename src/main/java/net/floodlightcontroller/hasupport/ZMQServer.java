package net.floodlightcontroller.hasupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ZMQServer implements Runnable{
	
	
	private static Logger logger = LoggerFactory.getLogger(ZMQServer.class);
	private String serverPort = new String();
	
	private final AsyncElection aelection;
	private final String controllerID;
	
	private ZMQ.Context zmqcontext = ZMQ.context(10);
	
	/**
	 * Possible outgoing server messages
	 */
	
	private final String ack      = new String ("ACK");
	private final String no       = new String ("NO");
	private final String lead     = new String ("LEADOK");
	private final String dc       = new String ("DONTCARE");
	
	// Decide the socket timeout value based on how fast you think the leader should
	// respond and also how far apart the actual nodes are placed. If you are trying
	// to communicate with servers far away, then anything upto 10s would be a good value.
	
	public final Integer socketTimeout = new Integer(500);
	
	/**
	 * 
	 * @param serverPort
	 */

	public ZMQServer(String serverPort, AsyncElection ae, String controllerID ) {
		// TODO Auto-generated constructor stub
		this.serverPort 		  = serverPort;
		this.aelection  		  = ae;
		this.controllerID		  = controllerID;
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ZMQ.Socket serverSocket = zmqcontext.socket(ZMQ.REP);
		
		logger.info("Starting ZMQ Server on port: "+ this.serverPort.toString());
		
		serverSocket.connect("tcp://"+this.serverPort.toString());
	    serverSocket.setReceiveTimeOut(this.socketTimeout);
		serverSocket.setSendTimeOut(this.socketTimeout);
		
		while(! Thread.currentThread().isInterrupted() ) {
			try{
				byte[] rep = serverSocket.recv();
				String stg = new String(rep);
				String reply = processServerMessage(stg);
				serverSocket.send(reply);
			} catch (ZMQException ze){
				// ze.printStackTrace();
				//logger.info("[ZMQServer] ZMQ Exception in server (nothing received) !");
			} catch (Exception e){
				//e.printStackTrace();
				//logger.info("[ZMQServer] Exception in server!");
			}
		}
		
		return;
	}
	
	/**
	 * All processServerMessage functions that use the aelection objects
	 * MUST be read only. Do not write to this object.
	 * @param mssg
	 * @return
	 */

	private String processServerMessage(String mssg) {
		// Let's optimize the string comparision time, in order to 
		// get the best perf: 
		// 1) using first 1 chars of 'stg' to find out what
		// message it was.
		// 2) Is substring the most optimal way to get first char
		// of string? Should we do string or char comparisons?
		
		char cmp = mssg.charAt(0);
		
		
		try{
			if(cmp == 'I') {
				
				logger.debug("[ZMQServer] Received IWon message: " + mssg.toString());
				String iw = String.valueOf(mssg.charAt(5));
				this.aelection.setTempLeader(iw);
				return ack;
				
			} else if (cmp == 'L') {
				
				logger.debug("[ZMQServer] Received LEADER message: " + mssg.toString());
				
				String le = String.valueOf(mssg.charAt(7));
				
				logger.debug("[ZMQServer] Get tempLeader: "+this.aelection.gettempLeader()+" "+le);
				
				if( this.aelection.gettempLeader().equals(le) ) {
					return lead;
				} else {
					return no;
				}
				
			} else if (cmp == 'S') {
				
				logger.debug("[ZMQServer] Received SETLEAD message: " + mssg.toString());
				
				String setl = String.valueOf(mssg.charAt(8));
				
				logger.debug("[ZMQServer] Get Leader: "+this.aelection.getLeader()+" "+setl);
				
				if(! this.aelection.gettempLeader().equals(this.controllerID) ) {
					if ( this.aelection.gettempLeader().equals(setl) ) {
						this.aelection.setLeader(setl);
						return ack;
					} else {
						return no;
					}
				} else {	
					return no;
				}
				
			} else if (cmp == 'Y'){
				
				logger.debug("[ZMQServer] Received YOU? message: " + mssg.toString());
				
				if( this.aelection.getLeader().equals(this.controllerID) ) {
					return this.controllerID;
				} else {
					return no;
				}
				
			} else if (cmp == 'H') {
				
				logger.debug("[ZMQServer] Received HEARTBEAT message: " + mssg.toString());
				
				String hb = String.valueOf(mssg.charAt(10));
				if ( this.aelection.getLeader().equals(hb) ) {
					return ack;
				} else {
					return no;
				}
				
			} else if (cmp == 'P') {
				
				logger.debug("[ZMQServer] Received PULSE message: " + mssg.toString());
				return ack;
			
			} else if (cmp == 'B') {
				
				logger.info("[ZMQServer] Received PUBLISH message: " + mssg.toString());
				AsyncElection.haworker.getService("LDHAWorker").publishHook();
				return ack;
				
			} else if (cmp == 'K') {
				
				logger.info("[ZMQServer] Received SUBSCRIBE message: " + mssg.toString());
				String cr = new String ("C" + String.valueOf(mssg.charAt(12)) ) ;
				AsyncElection.haworker.getService("LDHAWorker").subscribeHook(cr);
				return ack;
				
			}
		} catch (StringIndexOutOfBoundsException si) {
			si.printStackTrace();
	    } catch (Exception e){
			e.printStackTrace();
		}
		
		return dc;
	}

}
