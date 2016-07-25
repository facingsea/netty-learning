package com.facingsea.rpc.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class RpcDecoder extends ByteToMessageDecoder {

	private static final Logger log = LoggerFactory.getLogger(RpcDecoder.class);
	
	private Class<?> genericClass;
	
	public RpcDecoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if(in.readableBytes() < 0){
			return ;
		}
		in.markReaderIndex();
		if(in instanceof EmptyByteBuf){
			return ;
		}
		if(in.isReadable()){
			log.debug("in can read....");
		}
		log.debug(in.toString());
		int dataLength = in.readInt();
		if(dataLength < 0){
			ctx.close();
		}
		if(in.readableBytes() < dataLength){
			in.resetReaderIndex();
			return ;
		}
		byte[] data = new byte[dataLength];
		
		Object obj = SerializationUtil.deserialize(data, genericClass);
		System.out.println("===============: the data: " + data);
		out.add(obj);
	}
	
	

}
