package net.floodlightcontroller.hasupport.linkdiscovery;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ILDHAWorkerService extends IFloodlightService {
	
	public boolean publishHook();
	
	public boolean subscribeHook(String controllerID);
	
	

}
