package com.facingsea.rpc.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facingsea.rpc.common.RpcRequest;
import com.facingsea.rpc.common.RpcResponse;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

	private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);
	
	private final Map<String, Object> handlerMap ;
	
	public RpcHandler(Map<String, Object> handlerMap) {
		this.handlerMap = handlerMap;
	}
	
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
		log.debug("服务端读取request： ");
		log.debug(msg.toString());
		RpcResponse resp = new RpcResponse();
		resp.setRequestId(msg.getRequestId());
		try {
			Object result = handler(msg);
			resp.setResult(result);
		} catch (Throwable e) {
			resp.setError(e);
		}
		ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
	}
	
	private Object handler(RpcRequest req) throws Throwable{
		
		String className = req.getClassName();
		Object serviceBean = handlerMap.get(className);
		
		Class<?> serviceClass = handlerMap.getClass();
		String methodName = req.getMethodName();
		Class<?>[] parameterTypes = req.getParameterTypes();
		Object[] parameters = req.getParameters();
		
		FastClass serviceFastClass = FastClass.create(serviceClass);
		FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
		return serviceFastMethod.invoke(serviceBean, parameters);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("server caught exception", cause);
        ctx.close();
	}
}
