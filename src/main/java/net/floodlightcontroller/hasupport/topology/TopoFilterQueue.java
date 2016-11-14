package net.floodlightcontroller.hasupport.topology;

import java.util.List;

import net.floodlightcontroller.hasupport.IFilterQueue;

/**
 * 
 * @author Om Kale
 *
 */


public class TopoFilterQueue implements IFilterQueue {

	@Override
	public boolean enqueueForward(String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean dequeueForward() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void subscribe(String controllerID) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean enqueueReverse(String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> dequeueReverse() {
		// TODO Auto-generated method stub
		return null;
	}

}
