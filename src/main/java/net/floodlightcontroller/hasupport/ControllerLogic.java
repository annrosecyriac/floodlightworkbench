package net.floodlightcontroller.hasupport;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.hasupport.linkdiscovery.LDHAWorker;

public class ControllerLogic implements Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(ControllerLogic.class);
	
	private AsyncElection ael;
	// registerService()
	private final String none = new String("none");
	private final String controllerID;
	
	public static final LDHAWorker ldworker = new LDHAWorker();

	public ControllerLogic (AsyncElection ae, String cID ) {
		this.ael = ae;
		this.controllerID = cID;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
		// 1. First get the leader
			long start = System.nanoTime();
			int timeout = 50000;
			while( timeout > 0 ){
				if(! ael.getLeader().toString().equals(none) ) {
					Long duration = (long) ((System.nanoTime() - start) / 1000000.000) ;
					logger.info("[HAController MEASURE] Got Leader: "+ael.getLeader().toString() + "Elapsed :"+ duration.toString());
					break;
				} else {
					timeout = timeout - 1;
					TimeUnit.SECONDS.sleep(1);
				}
			}
			while (!Thread.currentThread().isInterrupted()) {	
				if ( ael.getLeader().toString().equals(this.controllerID) ) {
					// LEADER initiates publish and subscribe
					logger.info("[LEADER] Calling Hooks...");
					
					// 2. Then publish after 1
						ael.publish();
					// 3. Then Subscribe 5 s after 3
						ael.subscribe(new String ("C" + this.controllerID));
						TimeUnit.SECONDS.sleep(5);
				}
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
