package com.facingsea.rpc.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.ThreadLocalRandom;

public class ServiceDiscovery {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceDiscovery.class);
	
	private CountDownLatch latch = new CountDownLatch(1);
	
	private volatile List<String> dataList = new ArrayList<>();
	
	private String addressRegistry;

	public ServiceDiscovery(String addressRegistry) {
		this.addressRegistry = addressRegistry;
		
		ZooKeeper zk = connectServer();
		if(zk != null){
			watchNode(zk);
		}
	}

	public String discovery(){
		String data = null;
		int size = dataList.size();
		if(size > 0){
			if(size == 1){
				data = dataList.get(0);
				log.debug("user the only data {} ", data);
			} else {
				data = dataList.get(ThreadLocalRandom.current().nextInt(size));
				log.debug("get the data: " + data);
			}
		}
		return data;
	}
	
	private void watchNode(ZooKeeper zk) {
		try {
			List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
				
				@Override
				public void process(WatchedEvent event) {
					if(event.getType() == Event.EventType.NodeChildrenChanged){
						watchNode(zk);
					}
				}
			});
			
			List<String> dataList = new ArrayList<>();
			for(String node : nodeList){
				byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
				dataList.add(new String(bytes));
			}
			
			log.debug("node data {} ", dataList);
			this.dataList = dataList;
		} catch (Exception e) {
			log.error("", e);
		}
	}

	private ZooKeeper connectServer() {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(addressRegistry, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
				
				@Override
				public void process(WatchedEvent event) {
					if(event.getState() == Event.KeeperState.SyncConnected){
						latch.countDown();
					}
				}
			});
			latch.await();
		} catch (Exception e) {
			log.error("", e);
		}
		return zk;
	}
	
	
}
