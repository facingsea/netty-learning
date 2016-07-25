package com.facingsea.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facingsea.rpc.common.RpcRequest;
import com.facingsea.rpc.common.RpcResponse;
import com.facingsea.rpc.registry.ServiceDiscovery;

public class RpcProxy {
	
	private static final Logger log = LoggerFactory.getLogger(RpcProxy.class);
	
	private String serverAddress;
	
	private ServiceDiscovery serviceDiscovery;

	public RpcProxy(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public RpcProxy(ServiceDiscovery serviceDiscovery) {
		this.serviceDiscovery = serviceDiscovery;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T create(Class<?> interfaceClass){
		return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), 
				new Class<?>[]{interfaceClass}, 
				new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						
						RpcRequest request = new RpcRequest(); // 创建并初始化rpc请求
						
						request.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));
						request.setClassName(method.getDeclaringClass().getName());
						request.setMethodName(method.getName());
						request.setParameterTypes(method.getParameterTypes());
						request.setParameters(args);
						
						if(serviceDiscovery != null ){
							serverAddress = serviceDiscovery.discovery(); // 发现服务
							log.debug("get the server address from service discovery: " + serverAddress);
						}
						
						String[] arrs = serverAddress.split(":");
						String host = arrs[0];
						int port = Integer.parseInt(arrs[1]);
						
						RpcClient client = new RpcClient(host, port); // 初始化客户端
						RpcResponse resp = client.send(request); // 通过rpc客户端发送rpc请求并获取rpc响应
						if(resp.getError() != null){
							throw resp.getError();
						}
						return resp.getResult();
					}
				});
	}

}
