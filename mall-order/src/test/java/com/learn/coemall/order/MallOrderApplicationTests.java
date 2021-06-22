package com.learn.coemall.order;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    void sendMessageTest(){
        //发送消息(可以发送对象)
        rabbitTemplate.convertAndSend("hello-java-exchange","hello.java","hello world!");
    }

    @Test
    void creatExchange() {
        //创建交换机
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);

    }

    @Test
    void creatQueue(){
        //创建的队列
        Queue queue = new Queue("hello-java-queue", true,false,false);
        amqpAdmin.declareQueue(queue);
    }

    @Test
    void creatBinding(){
        //创建绑定
        //String destination,目的地
        // DestinationType destinationType,目的地类型
        // String exchange,交换机
        // routingKey,路由键
        //Map<String, Object> arguments 自定义参数
        //将指定的交换机和目的地进行绑定
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",null);
        amqpAdmin.declareBinding(binding);
    }

}
