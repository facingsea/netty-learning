package com.facingsea.rpc.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

/**
 * 使用Objenesis实现序列化和反序列化
 * @author wangzhf
 *
 */
public class SerializationUtil {
	
	private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();
	
	private static Objenesis objenesis = new ObjenesisStd(true);
	
	public SerializationUtil() {
	}

	@SuppressWarnings("unchecked")
	private static <T> Schema<T> getSchema(Class<T> cls){
		Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
		if(schema == null){
			schema = RuntimeSchema.createFrom(cls);
			if(schema != null){
				cachedSchema.put(cls, schema);
			}
		}
		return schema;
	}
	
	public static <T> byte[] serialize(T obj){
		Class<T> cls = (Class<T>) obj.getClass();
		LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
		try {
			Schema<T> schema = getSchema(cls);
			return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			buffer.clear();
		}
		return null;
	}
	
	public static <T> T deserialize(byte[] data, Class<T> cls){
		try {
			T msg = objenesis.newInstance(cls);
			System.out.println(msg);
			Schema<T> schema = getSchema(cls);
			ProtostuffIOUtil.mergeFrom(data, msg, schema);
			return msg;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
