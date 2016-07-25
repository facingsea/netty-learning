package com.facingsea.rpc.sample.server;

import com.facingsea.rpc.sample.client.HelloService;
import com.facingsea.rpc.sample.client.Person;
import com.facingsea.rpc.server.RpcService;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
