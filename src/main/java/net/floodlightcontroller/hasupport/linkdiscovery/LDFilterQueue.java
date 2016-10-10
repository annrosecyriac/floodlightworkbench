package net.floodlightcontroller.hasupport.linkdiscovery;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.MappingChange.Map;

import net.floodlightcontroller.hasupport.IFilterQueue;

public class LDFilterQueue implements IFilterQueue {
	protected static Logger logger = LoggerFactory.getLogger(LDHAWorker.class);
	LinkedBlockingQueue<JSONObject> filterQueue;
	Map<String, JSONObject> myMap;
	

	@Override
	public boolean enqueueForward(JSONObject value) {
		// TODO Auto-generated method stub
		MessageDigest mdEnc;
		try {
			mdEnc = MessageDigest.getInstance("MD5");
			mdEnc.digest(value.toString().getBytes());
			String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
		} catch (NoSuchAlgorithmException nae) {
			// TODO Auto-generated catch block
			logger.debug("[FilterQ] No such algorithm MD5!");
			nae.printStackTrace();
		} catch (Exception e){
			logger.debug("[FilterQ] Exception: enqueueFwd!");
			e.printStackTrace();
		}		
		filterQueue.offer(value);	
		return false;
	}

	@Override
	public boolean dequeueForward(JSONObject value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean enqueueReverse(JSONObject value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean dequeueReverse(JSONObject value) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
