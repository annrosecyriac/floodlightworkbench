package net.floodlightcontroller.hasupport;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMQQueue;

/**
 * The Queue Device
 * @author Bhargav Srinivasan
 */

public class QueueDevice implements Runnable{

	private static Logger logger = LoggerFactory.getLogger(QueueDevice.class);
	
	public final String serverPort;
	public final String clientPort;
	
	public QueueDevice(String servePort, String clienPort) {
		// TODO Auto-generated constructor stub
		// Chops "127.0.0.1:" to return the server and client port.
		this.serverPort = servePort.substring(10);
		this.clientPort = clienPort.substring(10);
	}
	
	
	public void startQueue() {
		
		try{
			/**
			 * Number of I/O threads assigned to the queue device.
			 */
			ZMQ.Context zmqcontext = ZMQ.context(10);
			
			/** 
			 * Connection facing the outside, where other nodes can connect 
			 * to this node. (frontend)
			 */
			ZMQ.Socket clientSide = zmqcontext.socket(ZMQ.ROUTER);
			clientSide.bind("tcp://0.0.0.0:"+this.clientPort.toString());
			
			
			/**
			 * The backend of the load balancing queue and the server 
			 * which handles all the incoming requests from the frontend.
			 * (backend)
			 */
			ZMQ.Socket serverSide = zmqcontext.socket(ZMQ.DEALER);
			serverSide.bind("tcp://0.0.0.0:"+this.serverPort.toString());
			
			logger.info("Starting ZMQueue device...");
			
			/**
			 * This is an infinite loop to run the QueueDevice!
			 */
			ZMQQueue queue = new ZMQQueue(zmqcontext,clientSide,serverSide);
			queue.run();
			
			queue.close();
			clientSide.close();
			serverSide.close();
			zmqcontext.term();
			
		} catch (ZMQException ze){		
			logger.debug("Zero MQ Exception occoured "+ze.toString());
			ze.printStackTrace();	
		} catch (IOException ie){
			logger.debug("I/O exception occoured while trying to close QueueDevice "+ie.toString());
			ie.printStackTrace();
		} catch (Exception e){
			logger.debug("Exception occoured while trying to close QueueDevice "+e.toString());
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		startQueue();
		
	}

}
