package net.floodlightcontroller.hasupport;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.threadpool.IThreadPoolService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HAController
 * @author Bhargav Srinivasan
 */

public class HAController implements IFloodlightModule {

	private static Logger logger = LoggerFactory.getLogger(HAController.class);
	protected static IThreadPoolService threadPoolService;
	protected static SingletonTask electionTask;
	private static Map<String, String> config = new HashMap<String, String>();
	private final String none = new String("none");

	
	public static void setSysPath(){
		try {
			final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
			usrPathsField.setAccessible(true);
			final String[] path = (String[]) usrPathsField.get(null);
			final String[] newPaths = Arrays.copyOf(path, path.length +2);
			newPaths[newPaths.length - 2] = "lib/";
			newPaths[newPaths.length - 1] = "lib/jzmq-3.1.0.jar";
			usrPathsField.set(null, newPaths);		
		} catch (NoSuchFieldException | SecurityException 
				|  IllegalArgumentException |  IllegalAccessException e) {
			logger.debug(new String(e.toString()));
		}
		
		return;
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
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(HAController.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		setSysPath();
		config = context.getConfigParams(this);
		logger.info("Configuration parameters: {} {} ", new Object[] {config.toString(), config.get("nodeid")});
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
		//Read config file and start the Election class with the right params.
		
		ScheduledExecutorService ses = threadPoolService.getScheduledExecutor();
		AsyncElection ael = new AsyncElection( config.get("serverPort") ,config.get("clientPort"), config.get("nodeid") );
		electionTask = new SingletonTask(ses, ael);		
		
		try{
			electionTask.reschedule(1, TimeUnit.NANOSECONDS);
			long start = System.nanoTime();
			while(! Thread.currentThread().isInterrupted()){
				if(! ael.getLeader().toString().equals(none) ) {
					Long duration = (long) ((System.nanoTime() - start) / 1000000.000) ;
					logger.info("[HAController MEASURE] Got Leader: "+ael.getLeader().toString() + "Elapsed :"+ duration.toString());
					break;
				}
			}
			
		} catch (Exception e){
			logger.info("[Election] Was interrrupted! "+e.toString());
			e.printStackTrace();
		}
		
		
	}
	
}
