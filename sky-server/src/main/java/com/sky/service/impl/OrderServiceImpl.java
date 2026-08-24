package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.WebSocket.WebSocketServer;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO orderSubmit(OrdersSubmitDTO ordersSubmitDTO) {
        //进行业务数据校验(地址是否为空,购物车是否为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询是否超出配送范围
        checkOut(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() +addressBook.getDetail());

        //查询购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);

        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insert(orderDetailList);

        //清空购物车数据
        shoppingCartMapper.delete(userId);

        //封装数据到OrderSubmitVO返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        /*Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));*/

        paySuccess(ordersPaymentDTO.getOrderNumber());

        //return vo;
        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过WebSocket给客户端发送来单提醒
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "来单提醒" + ordersDB.getNumber());

        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult queryHistoryOrdersWithOrderDetail(OrdersPageQueryDTO ordersPageQueryDTO) {
        //查询订单表
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();

        if (page != null && page.getTotal() > 0) {
            //拷贝orders给orderVO
            page.getResult().forEach(orders -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVOList.add(orderVO);
            });

            //查询订单明细表
            List<Long> orderIds = page.getResult().stream()
                    .map(Orders::getId)
                    .collect(Collectors.toList());

            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderIds);

            //组装数据
            if (orderDetailList != null && !orderDetailList.isEmpty()) {
                Map<Long, List<OrderDetail>> map = orderDetailList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(OrderDetail::getOrderId));

                orderVOList.forEach(orderVO -> {
                    List<OrderDetail> detailList = map.get(orderVO.getId());
                    if (detailList != null) {
                        orderVO.setOrderDetailList(detailList);
                    }
                });
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 查看订单详细详细
     * @param id
     * @return
     */
    public OrderVO queryOrderWithOrderDetail(Long id) {
        //查看订单详细
        Orders orders = orderMapper.getById(id);

        //查看订单详细信息
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(ids);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancelOrder(Long id) {
        //查询订单数据
        Orders ordersDB = orderMapper.getById(id);

        //校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //如果订单状态为待接单, 取消订单需要给用户退钱
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            /*//调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额*/

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetitionOrder(Long id) {
        //查询当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询订单详情
        List<Long> ids = new ArrayList<>();
        ids.add(id);

        List<OrderDetail> orderDetailsList = orderDetailMapper.getByOrderId(ids);

        //将订单详情转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailsList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            BeanUtils.copyProperties(orderDetail, shoppingCart);

            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult orderSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVoList = getOrderVoList(page);

        return new PageResult(page.getTotal(), orderVoList);
    }

    /**
     * 将Orders对象转换为OrderVO
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVoList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        for (Orders orders : ordersList) {
            // 将共同字段复制到OrderVO
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            String orderDishesStr = getOrderDishesStr(orders);

            // 将订单菜品信息封装到orderVO中，并添加到orderVOList
            orderVO.setOrderDishes(orderDishesStr);
            orderVOList.add(orderVO);
        }
        return orderVOList;
    }

    /**
     * 将菜品信息转换为字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        //查询订单菜品详情信息（订单中的菜品和数量）
        List<Long> orderId = new ArrayList<>();
        orderId.add(orders.getId());
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDish = orderDetails.stream().map(x -> x.getName() + "*" + x.getNumber() + ";").collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDish);
    }

    /**
     * 统计各个状态的订单数量
     * @return
     */
    public OrderStatisticsVO statistics() {
        //统计各个状态的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //将统计的数量封装进OrderStatisticsVO
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //确认订单状态是待接单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.REFUND)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //判断订单是否已退款 - 是则抛出异常
        if (Objects.equals(ordersDB.getPayStatus(), Orders.REFUND)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //判断订单是否已付款 - 是则进行退款操作
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if (Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            //进行退款操作
            /*weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/

            //设置订单支付状态为已退款
            log.info("申请退款");
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //查询订单数据
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //校验订单是否已经支付
        if (Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            //已经支付,执行退款操作
            /*weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01)
            );*/

            log.info("执行退款操作");
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        //校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //校验订单是否已经支付 - 不是已支付状态则抛异常
        if (!Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //校验订单状态是否是已接单 - 不是则抛异常
        if (!Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        //查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        //校验订单是否为空
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //校验订单是否为派送中
        if (!Objects.equals(ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为派送中
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 依据百度API查询是否超出配送范围
     * @param address
     */
    private void checkOut(String address) {
        Map param = new LinkedHashMap<String, String>();
        param.put("address", shopAddress);
        param.put("output", "json");
        param.put("ak", ak);

        //获取商家经纬度
        String shopLngLat = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", param);

        //判断商家地址是否转换成功
        JSONObject location = JSON.parseObject(shopLngLat);
        if (!location.getString("status").equals("0")) {
            throw new OrderBusinessException("商家地址解析失败");
        }

        //对数据解析
        location = location.getJSONObject("result").getJSONObject("location");
        String lng = location.getString("lng");
        String lat = location.getString("lat");
        //店铺经纬度坐标
        String shopLatLng = lat + "," + lng;

        //获取用户经纬度
        param.put("address", address);
        String userLngLat = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", param);

        //判断用户地址是否转换成功
        location = JSON.parseObject(userLngLat);
        if (!location.getString("status").equals("0")) {
            throw new OrderBusinessException("用户地址解析失败");
        }

        //对数据解析
        location = location.getJSONObject("result").getJSONObject("location");
        lng = location.getString("lng");
        lat = location.getString("lat");
        //用户经纬度坐标
        String userLatLng = lat + "," + lng;

        param.put("origin", shopLatLng);
        param.put("destination", userLatLng);
        param.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/riding", param);
        
        //判断路线规划是否成功
        location = JSON.parseObject(json);
        if (!location.getString("status").equals("0")) {
            throw new OrderBusinessException("路线规划失败");
        }

        //解析数据
        location = location.getJSONObject("result");
        JSONArray jsonArray = location.getJSONArray("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        //判断是否超出配送距离
        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 用户催单
     * @param id
     */
    public void reminder(Long id) {
        //查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        //校验订单是否为空
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //通过WebSocket给客户端发送来单提醒
        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", ordersDB.getId());
        map.put("content", "催单提醒" + ordersDB.getNumber());

        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }
}
