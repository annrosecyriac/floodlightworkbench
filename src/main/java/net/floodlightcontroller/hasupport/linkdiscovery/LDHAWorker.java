package net.floodlightcontroller.hasupport.linkdiscovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.*;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.HARole;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.storage.StorageException;
import net.floodlightcontroller.threadpool.IThreadPoolService;


public class LDHAWorker implements IFloodlightModule, ILinkDiscoveryListener {
	protected static Logger logger = LoggerFactory.getLogger(LDHAWorker.class);
	protected static ILinkDiscoveryService linkserv;
	protected static IFloodlightProviderService floodlightProvider;
	public JSONObject myJson = new JSONObject();
	
	protected SingletonTask dummyTask;
	List<String> synLDUList = Collections.synchronizedList(new ArrayList<String>());
	protected static IThreadPoolService threadPoolService;

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
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
    	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IThreadPoolService.class);
		return l;
	}
	
	public void dummyfn(){
		logger.info("LDHAWorker is starting...");
		synchronized (synLDUList){
			logger.info("Printing Update {}: ",new Object[]{synLDUList});
			synLDUList.clear();
		}
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		linkserv = context.getServiceImpl(ILinkDiscoveryService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(LDHAWorker.class);
		linkserv.addListener(this);
		ScheduledExecutorService ses = threadPoolService.getScheduledExecutor();

		// To be started by the first switch connection
		dummyTask = new SingletonTask(ses, new Runnable() {
			@Override
			public void run() {
				try {
					dummyfn();
				} catch (Exception e) {
					logger.error("Exception in LDWorker.", e);
				} finally {
					dummyTask.reschedule(10, TimeUnit.SECONDS);	
				}
			}
		});
		dummyTask.reschedule(10, TimeUnit.SECONDS);
	}
}