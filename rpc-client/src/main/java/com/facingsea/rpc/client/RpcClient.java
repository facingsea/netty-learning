package com.facingsea.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facingsea.rpc.common.RpcDecoder;
import com.facingsea.rpc.common.RpcEncoder;
import com.facingsea.rpc.common.RpcRequest;
import com.facingsea.rpc.common.RpcResponse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

	private static final Logger log = LoggerFactory.getLogger(RpcClient.class);
	
	private String host;
	
	private int port;
	
	private RpcResponse rpcResponse;
	
	private final Object obj = new Object();
	
	public RpcClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
		this.rpcResponse = response;
		synchronized (obj) {
			obj.notifyAll(); // 收到响应，唤醒线程
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("client caught exception. ", cause);
		ctx.close();
	}

	public RpcResponse send(RpcRequest request){
		System.out.println("发送前：");
		System.out.println(request);
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class)
							.handler(new ChannelInitializer<SocketChannel>() {

								@Override
								protected void initChannel(SocketChannel ch) throws Exception {
									ch.pipeline()
										.addLast(new RpcEncoder(RpcRequest.class))
										.addLast(new RpcDecoder(RpcResponse.class))
										.addLast(RpcClient.this);
								}
							}).option(ChannelOption.SO_KEEPALIVE, true);
			
			ChannelFuture f = b.connect(host, port).sync();
			f.channel().writeAndFlush(request).sync();
			
			synchronized (obj) {
				obj.wait(); // 未收到响应，使线程等待
			}
			
			if(rpcResponse != null){
				f.channel().closeFuture().sync();
			}
			return rpcResponse;
		} catch (InterruptedException e) {
			log.error("", e);
			group.shutdownGracefully();
		} finally {
			group.shutdownGracefully();
		}
		return null;
	}
	
}
