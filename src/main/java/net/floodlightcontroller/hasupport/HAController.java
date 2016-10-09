package net.floodlightcontroller.hasupport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMQQueue;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HAController implements IFloodlightModule {

	private static Logger logger = LoggerFactory.getLogger(HAController.class);
	
	
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
		Collection<Class<? extends IFloodlightService>> l = 
				new ArrayList<Class <? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(HAController.class);
		setSysPath();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		//startServer();
		Thread e1 = new Thread (new AsyncElection(), "ElectionThread");
		e1.start();
	}
	
	public static void startServer(){
		
		// Start: Simple ZMQ server
		ZMQ.Context zmqcontext = ZMQ.context(1);
				
		ZMQ.Socket responder = zmqcontext.socket(ZMQ.REP);
		responder.bind("tcp://*:5555");
		
		logger.info(new String("Starting server on :5555..."));
		
		try{
			while(Boolean.TRUE){
				byte[] resp = responder.recv(0); 
				logger.info(new String(resp));
				String reply = "server response";
				responder.send(reply.getBytes(),0);
			}
		} catch (Exception e){
			logger.info(e.toString());
			responder.close();
			zmqcontext.term();		
		}
		
	}
	
}
