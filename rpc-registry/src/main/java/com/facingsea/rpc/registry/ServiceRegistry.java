package com.facingsea.rpc.registry;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRegistry {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);
	
	private CountDownLatch latch = new CountDownLatch(1);
	
	private String registryAddress;
	
	public ServiceRegistry(String registryAddress) {
		this.registryAddress = registryAddress;
	}
	
	public void register(String data){
		if(data != null ){
			ZooKeeper zk = connectServer();
			if(zk != null){
				log.debug("create new node : " + data);
				createNode(zk, data);
			}
		}
		
	}
	
	private ZooKeeper connectServer(){
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher(){

				@Override
				public void process(WatchedEvent event) {
					if(event.getState() == Event.KeeperState.SyncConnected){
						log.debug("zookeeper sync connected....");
						latch.countDown();
					}else{
						log.error("zookeeper cannot sync connected ... ");
					}
				}
				
			});
		} catch (IOException e) {
			log.error("error: ", e);
		}
		return zk;
	}
	
	private void createNode(ZooKeeper zk, String data) {
		try {
			
			// 判断zookeeper是否完成连接
			if(zk.getState() == States.CONNECTING){
				log.debug("wait for the zookeeper connected...");
				latch.await();
			}else{
				log.debug(zk.getState().name());
			}
			
			byte[] bytes = data.getBytes();
			String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			log.debug("create zookeeper: node ({} => {}) ", path, data);
		} catch (KeeperException | InterruptedException e) {
			log.error("error: " , e);
		} 
	}

}
