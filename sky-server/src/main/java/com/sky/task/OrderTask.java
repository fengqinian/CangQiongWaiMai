package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时处理超时未付款的订单
     */
    @Scheduled(cron = "0 * * * * *") //每分钟触发一次
    public void processTimeoutOrder() {
        log.info("处理订单超时: {}", LocalDateTime.now());

        //查询是否有符合的订单
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);

        List<Orders> orderList = orderMapper.getByStatusWithOrderTime(Orders.PENDING_PAYMENT, localDateTime);

        //如果有符合的订单就修改状态为已取消
        if (orderList != null && !orderList.isEmpty()) {
            for (Orders orders : orderList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("订单超时, 自动取消");

                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时处理派送中一直未完成订单
     */
    @Scheduled(cron = "0 0 1 * * *") //每天一点触发
    public void processDeliveryOrder() {
        log.info("处理一直派送中订单: {}", LocalDateTime.now());

        //查询是否有符合的订单
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-60);

        List<Orders> orderList = orderMapper.getByStatusWithOrderTime(Orders.DELIVERY_IN_PROGRESS, localDateTime);

        //如果有符合的订单就修改状态为已取消
        if (orderList != null && !orderList.isEmpty()) {
            for (Orders orders : orderList) {
                orders.setStatus(Orders.COMPLETED);

                orderMapper.update(orders);
            }
        }
    }
}
