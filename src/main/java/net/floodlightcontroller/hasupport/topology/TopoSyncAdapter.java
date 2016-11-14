package net.floodlightcontroller.hasupport.topology;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sdnplatform.sync.IStoreListener;
import org.sdnplatform.sync.internal.rpc.IRPCListener;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.hasupport.ISyncAdapter;

/**
 * 
 * @author Om Kale
 *
 */


public class TopoSyncAdapter implements ISyncAdapter, IFloodlightModule, IStoreListener<String>, IRPCListener {

	@Override
	public void disconnectedNode(Short nodeId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectedNode(Short nodeId) {
		// TODO Auto-generated method stub

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
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void packJSON(List<String> updates) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keysModified(Iterator<String> keys, org.sdnplatform.sync.IStoreListener.UpdateType type) {
		// TODO Auto-generated method stub
		
	}

}
