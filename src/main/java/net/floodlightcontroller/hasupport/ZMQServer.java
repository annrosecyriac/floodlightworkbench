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
	 * Possible incoming server messages
	 */
	private final String iwon     = new String ("I");
	private final String leader   = new String ("L");
	private final String setlead  = new String ("S");
	private final String you      = new String ("Y");
	private final String heartb   = new String ("H");
	private final String pulse    = new String ("P");
	
	/**
	 * Possible outgoing server messages
	 */
	
	private final String ack      = new String ("ACK");
	private final String no       = new String ("NO");
	private final String lead     = new String ("LEADER");
	private final String dc       = new String ("DONTCARE");
	
	
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
		
		logger.info("Server Port: "+ this.serverPort.toString());
		
		serverSocket.connect("tcp://"+this.serverPort.toString());
	    serverSocket.setReceiveTimeOut(this.socketTimeout);
		serverSocket.setSendTimeOut(this.socketTimeout);
		
		while(Boolean.TRUE){
			try{
				byte[] rep = serverSocket.recv();
				String stg = new String(rep);
				logger.info("Server received: "+stg.toString());
				String reply = processServerMessage(stg);
				serverSocket.send(reply);
			} catch (ZMQException ze){
				// ze.printStackTrace();
				logger.info("[ZMQServer] ZMQ Exception in server (nothing received) !");
			} catch (Exception e){
				//e.printStackTrace();
				logger.info("[ZMQServer] Exception in server!");
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
		// TODO Auto-generated method stub
		// Let's optimize the string comparision time, in order to 
		// get the best perf: 
		// 1) using first 1 chars of 'stg' to find out what
		// message it was.
		// 2) Is substring the most optimal way to get first char
		// of string? Should we do string or char comparisons?
		
		String cmp = mssg.substring(0,1);
		
		logger.info("[ZMQServer] "+cmp.toString());
		
		if(cmp.equals(iwon)) {
			
			logger.info("[ZMQServer] Received IWon message: " + mssg.toString());
			String [] iw = mssg.split(" ");
			this.aelection.setTempLeader(iw[1]);
			return ack;
			
		} else if (cmp.equals(leader)) {
			
			logger.info("[ZMQServer] Received LEADER message: " + mssg.toString());
			
			String [] le = mssg.split(" ");
			String l     = le[1];
			
			logger.info("[ZMQServer] Get tempLeader: "+this.aelection.gettempLeader()+" "+l);
			
			if( this.aelection.gettempLeader().equals(l) ) {
				return new String(this.lead + " " + l);
			} else {
				return no;
			}
			
		} else if (cmp.equals(setlead)) {
			
			logger.info("[ZMQServer] Received SETLEAD message: " + mssg.toString());
			
			String [] setl = mssg.split(" ");
			String port    = setl[1]; 
			
			logger.info("[ZMQServer] Get Leader: "+this.aelection.getLeader()+" "+port);
			
			if(! this.aelection.getLeader().equals(this.controllerID) ) {
				if ( this.aelection.gettempLeader().equals(port) ) {
					this.aelection.setLeader(port);
					return new String (ack);
				} else {
					return no;
				}
			} else {	
				return no;
			}
			
		} else if (cmp.equals(you)){
			
			logger.info("[ZMQServer] Received YOU? message: " + mssg.toString());
			
			if( this.aelection.getLeader().equals(this.controllerID) ) {
				return this.controllerID;
			} else {
				return no;
			}
			
		} else if (cmp.equals(heartb)) {
			
			logger.info("[ZMQServer] Received HEARTBEAT message: " + mssg.toString());
			
			String[] hb = mssg.split(" ");
			if ( this.aelection.getLeader().equals(hb[1]) ) {
				return ack;
			} else {
				return no;
			}
			
		} else if (cmp.equals(pulse)) {
			
			logger.info("[ZMQServer] Received PULSE message: " + mssg.toString());
			
			return ack;
		}
		
		return dc;
	}

}
