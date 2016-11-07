package net.floodlightcontroller.hasupport;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.hasupport.linkdiscovery.ILDHAWorkerService;
import net.floodlightcontroller.hasupport.linkdiscovery.LDHAWorker;

public class ControllerLogic implements Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(ControllerLogic.class);
	
	private AsyncElection ael;
	protected static ILDHAWorkerService ldHAService;
	private final String none = new String("none");
	private final String controllerID;
	
	public static final LDHAWorker ldworker = new LDHAWorker();

	public ControllerLogic (AsyncElection ae, String cID, ILDHAWorkerService ldHAService ) {
		this.ael = ae;
		this.controllerID = new String("C" + cID);
		ControllerLogic.ldHAService = ldHAService;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
		// 1. First get the leader
			long start = System.nanoTime();
			int timeout = 2;
			while( timeout > 0 ){
				if(! ael.getLeader().toString().equals(none ) ) {
					Long duration = (long) ((System.nanoTime() - start) / 1000000.000) ;
					logger.info("[HAController MEASURE] Got Leader: "+ael.getLeader().toString() + "Elapsed :"+ duration.toString());
					break;
				} else {
					timeout = timeout - 1;
					TimeUnit.SECONDS.sleep(1);
				}
			}
			
			while (!Thread.currentThread().isInterrupted()) {
			// 2. Then publish after 1
				ldHAService.publishHook();
			// 3. Then Subscribe 5 s after 3
				ldHAService.subscribeHook(this.controllerID);
				TimeUnit.SECONDS.sleep(5);
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
