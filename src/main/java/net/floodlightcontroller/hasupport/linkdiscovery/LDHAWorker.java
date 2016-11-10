package net.floodlightcontroller.hasupport.linkdiscovery;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.hasupport.IHAWorker;
import net.floodlightcontroller.hasupport.IHAWorkerService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

/**
 * This is the Worker class used to publish, subscribe updates to
 * and from the controller respectively
 * @author Om Kale
 *
 */
public class LDHAWorker implements IHAWorker, IFloodlightModule, ILinkDiscoveryListener {
	protected static Logger logger = LoggerFactory.getLogger(LDHAWorker.class);
	protected static ILinkDiscoveryService linkserv;
	protected static IFloodlightProviderService floodlightProvider;
	protected static IHAWorkerService haworker;
	
	protected Runnable dummyTask;
	List<String> synLDUList = Collections.synchronizedList(new ArrayList<String>());
	protected static IThreadPoolService threadPoolService;
	private static final LDFilterQueue myLDFilterQueue = new LDFilterQueue(); 
	
	public LDHAWorker(){};
	
	/**
	 * This function is used to assemble the LDupdates into
	 * a JSON string using JSON Jackson API
	 * @return JSON string
	 */
	
	@Override
	public List<String> assembleUpdate() {
		// TODO Auto-generated method stub
		List<String> jsonInString = new LinkedList<String>();
		LDHAUtils parser = new LDHAUtils();
		
		String preprocess = new String (synLDUList.toString());
		// Flatten the updates and strip off leading [
		
		if(preprocess.startsWith("[")){
			preprocess = preprocess.substring(1, preprocess.length());
		}
		
		String chunk = new String(preprocess.toString());
		
		if(! preprocess.startsWith("]") ) {
			jsonInString = parser.parseChunk(chunk);
		}

		logger.info("\n[Assemble Update] JSON String: {}", new Object[] {jsonInString});
		return jsonInString;
	}

    /**
     * This function is called in order to start pushing updates 
     * into the syncDB
     */
	public boolean publishHook() {
		// TODO Auto-generated method stub
		try{
			synchronized (synLDUList){
				logger.info("Printing Updates {}: ",new Object[]{synLDUList});
				List<String> updates = assembleUpdate();
				for(String update : updates){
					myLDFilterQueue.enqueueForward(update);
				}
				synLDUList.clear();
				myLDFilterQueue.dequeueForward();
			}
			return true;
		} catch (Exception e){
			logger.info("[LDHAWorker] An exception occoured!");
			return false;
		}
	}


	public boolean subscribeHook(String controllerID) {
		// TODO Auto-generated method stub
		try {
			List<String> updates = new ArrayList<String>();
			myLDFilterQueue.subscribe(controllerID);
			updates = myLDFilterQueue.dequeueReverse();
			logger.info("[Subscribe] ==================== THE UPDATES: ===================");
			for (String update: updates) {
				logger.info("Update: {}", new Object[]{update.toString()});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
		// TODO Auto-generated method stub
		synchronized(synLDUList){
			//synLDUList.clear();
			for (LDUpdate update: updateList){	
				synLDUList.add(update.toString());
			}
		}
		
	}
	
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
    	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
    	l.add(IHAWorkerService.class);
    	l.add(IThreadPoolService.class);
		return l;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		linkserv = context.getServiceImpl(ILinkDiscoveryService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		haworker = context.getServiceImpl(IHAWorkerService.class);
		logger.info("LDHAWorker is init...");
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(LDHAWorker.class);
		linkserv.addListener(this);
		haworker.registerService("LDHAWorker",this);
		logger.info("LDHAWorker is starting...");
		
		return;
	}
	
}
