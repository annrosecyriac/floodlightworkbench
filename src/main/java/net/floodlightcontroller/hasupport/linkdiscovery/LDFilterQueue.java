package net.floodlightcontroller.hasupport.linkdiscovery;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.MappingChange.Map;

import net.floodlightcontroller.hasupport.IFilterQueue;

public class LDFilterQueue implements IFilterQueue {
	
	protected static Logger logger = LoggerFactory.getLogger(LDFilterQueue.class);
	private static final LDSyncAdapter syncAdapter = new LDSyncAdapter();
	
	LinkedBlockingQueue<JSONObject> filterQueue;
	HashMap<String, JSONObject> myMap = new HashMap<String, JSONObject>();
	

	@Override
	public boolean enqueueForward(JSONObject value) {
		// TODO Auto-generated method stub
		MessageDigest mdEnc;
		try {
			mdEnc = MessageDigest.getInstance("MD5");
			mdEnc.digest(value.toString().getBytes());
			String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
			if( (!myMap.containsKey(md5)) && (!value.equals(null)) ){
				filterQueue.offer(value);
				myMap.put(md5, value);
			}
			return true;
		} catch (NoSuchAlgorithmException nae) {
			// TODO Auto-generated catch block
			logger.debug("[FilterQ] No such algorithm MD5!");
			nae.printStackTrace();
			return false;
		} catch (Exception e){
			logger.debug("[FilterQ] Exception: enqueueFwd!");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean dequeueForward() {
		// TODO Auto-generated method stub
		try {
			LinkedList<JSONObject> LDupds = new LinkedList<JSONObject>();
			filterQueue.drainTo(LDupds);
			syncAdapter.packJSON(LDupds);
		} catch (Exception e){
			logger.debug("[FilterQ] Dequeue Forward failed!");
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean enqueueReverse(JSONObject value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean dequeueReverse() {
		// TODO Auto-generated method stub
		return false;
	}
	

}
