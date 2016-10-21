package net.floodlightcontroller.hasupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ZMQServer implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(ZMQServer.class);
	private String serverPort = new String();
	private ZMQ.Context zmqcontext = ZMQ.context(10);
	
	/**
	 * Possible server messages
	 */
	private final String iwon     = new String ("I");
	private final String leader   = new String ("L");
	private final String setlead  = new String ("S");
	private final String you      = new String ("Y");
	private final String heartb   = new String ("H");
	private final String pulse    = new String ("P");
	
	private final String ack      = new String ("ACK");
	private final String no      = new String ("NO");
	
	
	public final Integer socketTimeout = new Integer(500);
	
	/**
	 * 
	 * @param serverPort
	 */

	public ZMQServer(String serverPort) {
		// TODO Auto-generated constructor stub
		this.serverPort = serverPort;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ZMQ.Socket serverSocket = zmqcontext.socket(ZMQ.REP);
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
	

	private String processServerMessage(String stg) {
		// TODO Auto-generated method stub
		// Let's optimize the string comparision time, in order to 
		// get the best perf: 
		// 1) using first 1 chars of 'stg' to find out what
		// message it was.
		// 2) Is substring the most optimal way to get first char
		// of string? Should we do string or char comparisons?
		
		String cmp = stg.substring(0,1);
		
		logger.info("[ZMQServer] "+cmp.toString());
		
		if(cmp.equals(iwon)){
			return no;
			
		} else if (cmp.equals(leader)) {
			return no;
			
		} else if (cmp.equals(setlead)) {
			return no;
			
		} else if (cmp.equals(you)){
			return no;
			
		} else if (cmp.equals(you)) {
			return no;
			
		} else if (cmp.equals(heartb)) {
			return no;
			
		} else if (cmp.equals(pulse)) {
			return ack;
		}
		
		return new String("DONTCARE");
	}

}
