package com.facingsea.rpc.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.facingsea.rpc.common.RpcDecoder;
import com.facingsea.rpc.common.RpcEncoder;
import com.facingsea.rpc.common.RpcRequest;
import com.facingsea.rpc.common.RpcResponse;
import com.facingsea.rpc.registry.ServiceRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RpcServer implements ApplicationContextAware, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(RpcServer.class);
	
	private String serverAddress;
	
	private ServiceRegistry serviceRegistry;
	
	private Map<String, Object> handlerMap = new HashMap<>(); // 存放接口名与服务对象之间的映射关系
	
	public RpcServer(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
		this.serverAddress = serverAddress;
		this.serviceRegistry = serviceRegistry;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		EventLoopGroup boss = new NioEventLoopGroup();
		EventLoopGroup worker = new NioEventLoopGroup();
		try {
			log.debug("server side netty...");
			ServerBootstrap b = new ServerBootstrap();
			b.group(boss, worker).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline()
							.addLast(new RpcDecoder(RpcRequest.class))
							.addLast(new RpcEncoder(RpcResponse.class))
							.addLast(new RpcHandler(handlerMap))
							;
					}
				});
			
			b.option(ChannelOption.SO_BACKLOG, 128)
				.option(ChannelOption.SO_KEEPALIVE, true);
			
			String[] arrs = serverAddress.split(":");
			String host = arrs[0];
			int port = Integer.parseInt(arrs[1]);
			
			ChannelFuture f = b.bind(host, port).sync();
			log.debug("server started on port {} " + port);
			
			if(serviceRegistry != null ){
				serviceRegistry.register(serverAddress); // 注册服务地址
			}
			
			f.channel().closeFuture().sync();
		} finally {
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class); // 获取所有带RPCService的Bean
		if(MapUtils.isNotEmpty(serviceBeanMap)){
			for(Object serviceBean : serviceBeanMap.values()){
				String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
				handlerMap.put(interfaceName, serviceBean);
			}
		}
	}

}
