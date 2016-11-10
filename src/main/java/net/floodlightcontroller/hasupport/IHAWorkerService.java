package net.floodlightcontroller.hasupport;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IHAWorkerService extends IFloodlightService {
	
	public void registerService(String serviceName, IHAWorker haw);
	
	public IHAWorker getService(String serviceName);

}
