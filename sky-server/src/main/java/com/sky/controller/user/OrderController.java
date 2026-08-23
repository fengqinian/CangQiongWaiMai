package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "订单接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> orderSubmit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单数据: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.orderSubmit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> queryHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("查询历史订单: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.queryHistoryOrdersWithOrderDetail(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查看订单详细详细
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查看订单详细详细")
    public Result<OrderVO> queryOrderDetail(@PathVariable Long id) {
        log.info("查看订单详细详细,订单id为: {}", id);
        OrderVO orderVO = orderService.queryOrderWithOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancelOrder(@PathVariable Long id) {
        log.info("取消订单id为: {}", id);
        orderService.cancelOrder(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetitionOrder(@PathVariable Long id) {
        log.info("再来一单:");
        orderService.repetitionOrder(id);
        return Result.success();
    }
}
