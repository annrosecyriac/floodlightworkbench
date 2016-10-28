package net.floodlightcontroller.hasupport;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.*;

public class ZMQServerTest {
	
	static AsyncElection ae;
	static TestClient    tc;
	static QueueDevice   qu;
	static Thread qD;
	static Thread servThread;
	static Thread ael;
	static String mockServerPort = new String("127.0.0.1:4242");
	static String mockClientPort = new String("127.0.0.1:5252");
	static String nodeID		  = new String("2");

	@BeforeClass
	public static void setUp() throws Exception {
		setSysPath();
		ae = new AsyncElection(mockServerPort, nodeID);
        tc = new TestClient(mockClientPort);
        qD = new Thread(new Runnable() {
        	public void run() {
        		startQueue();
        	}
        });
        qD.start();
        ZMQServer zserver = new ZMQServer(mockServerPort,ae,nodeID);
        servThread = new Thread(zserver);
		servThread.start();
	}
	
public static void startQueue() {
		
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
			clientSide.bind("tcp://0.0.0.0:5252");
			
			
			/**
			 * The backend of the load balancing queue and the server 
			 * which handles all the incoming requests from the frontend.
			 * (backend)
			 */
			ZMQ.Socket serverSide = zmqcontext.socket(ZMQ.DEALER);
			serverSide.bind("tcp://0.0.0.0:4242");
			
			System.out.println("Starting ZMQueue device...");
			
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
			ze.printStackTrace();	
		} catch (IOException ie){
			ie.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testZMQServer() {
		//no need for sockets
		String recv = new String();
		recv = tc.send("PULSE");
		assertEquals(recv,"ACK");
	}
	
	@Test
	public void testRun() {
		String recv = tc.send("LOL 2");
		assertEquals(recv,"NO");
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		try {
			servThread.interrupt();
			qD.interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setSysPath(){
		System.setProperty("java.library.path", "lib/");
		System.setProperty("java.class.path", "lib/zmq.jar");
		Field sysPathsField;
		try {
			sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
			sysPathsField.setAccessible(true);
		    sysPathsField.set(null, null);
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

}
