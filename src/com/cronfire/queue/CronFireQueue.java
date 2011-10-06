package com.cronfire.queue;

import java.util.Iterator;
import java.util.concurrent.DelayQueue;

import com.cronfire.endpoint.EndpointUrl;
import com.cronfire.http.UrlLoaderThread;
import com.cronfire.load_manager.LoadManager;

public class CronFireQueue {
	static private CronFireQueue instance;
	
	private boolean isPaused = false;
	private DelayQueue<EndpointUrl> queue = new DelayQueue<EndpointUrl>();
	private LoadManager loadManager = LoadManager.getInstance();
	
	static public CronFireQueue getInstance() {
		if(null == instance) {
			instance = new CronFireQueue();
		}
		return instance;
	}
	
	private CronFireQueue() { /* No instantiation */ }
	
	public void add(EndpointUrl endpoint) {
		queue.add(endpoint);
	}
	
	public Iterator<EndpointUrl> iterator() {
		return queue.iterator();
	}
	
	public void pause(boolean b) {
		isPaused = b;
	}
	
	public void empty() {
		queue.clear();
	}
	
	public void start() {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						if(isPaused) {
							//Thread.currentThread();
							Thread.sleep(500L);
							continue;
						}
						
						EndpointUrl endpoint = queue.take();
						
						// [TODO] Check the current load
						double loadAvg = loadManager.getCurrentLoad();
						//System.out.println("Load Check: " + loadAvg);

						// [TODO] Reschedule (if load high or non parallel)
						if(loadAvg > 1.0) {
							//System.out.println("Load too high, rescheduling");
							
							endpoint.delayBySecs(30);
							queue.add(endpoint);

						// Process it
						} else {
							new UrlLoaderThread().pingUrl(endpoint);
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}