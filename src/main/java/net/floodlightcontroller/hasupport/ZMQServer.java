package net.floodlightcontroller.hasupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ZMQServer implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(ZMQServer.class);
	private String serverPort = new String();
	private ZMQ.Context zmqcontext = ZMQ.context(10);
	public final Integer socketTimeout = new Integer(500);

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
		return new String("ACK");
	}

}
