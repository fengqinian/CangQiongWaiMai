package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间内的营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getByTurnover(LocalDate begin, LocalDate end) {
        //该集合存储开始日期到结束日期的每一天
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //将日期加一天然后添加进集合
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //统计每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间内的用户数量
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getByUser(LocalDate begin, LocalDate end) {
        //该集合存储开始日期到结束日期的每一天
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //将日期加一天然后添加进集合
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存储每一天的新用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存储每一天的总用户数量
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end", endTime);

            Integer totalUser = userMapper.countByUser(map);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByUser(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间内的用户数量
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getByOrder(LocalDate begin, LocalDate end) {
        //该集合存储开始日期到结束日期的每一天
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //将日期加一天然后添加进集合
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //遍历dateList集合,查询指定时间内的订单数和有效订单数
        List<Integer> totalOrderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            //查询指定时间的订单数
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Integer totalOrder = getOrderCount(beginTime, endTime, null);

            //查询指定时间的有效订单数
            Integer validOrder = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            totalOrderCountList.add(totalOrder);
            validOrderCountList.add(validOrder);
        }

        //订单总数
        Integer totalOrderCount = totalOrderCountList.stream().reduce(Integer::sum).get();

        //有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(totalOrderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 获取订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    /**
     * 统计指定时间内的销量top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getByTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.getByTop10(beginTime, endTime);

        List<String> nameList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String name = StringUtils.join(nameList, ",");

        List<Integer> numberList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String number = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO.builder()
                .nameList(name)
                .numberList(number)
                .build();
    }
}
