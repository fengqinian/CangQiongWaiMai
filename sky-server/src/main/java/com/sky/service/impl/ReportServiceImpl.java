package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;

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

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询营业数据 - 近三十天的营业数据
        LocalDate now = LocalDate.now();
        LocalDate begin = now.minusDays(30);
        LocalDate end = now.minusDays(1);

        //查询总营业数据
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        //2.通过POI写入数据到模板文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //读取运营数据报表模板Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取sheet表格
            XSSFSheet sheet = excel.getSheet("sheet1");

            //填充数据 - 时间
            sheet.getRow(1).getCell(1).setCellValue("日期: " + begin + "至" + end);

            //获取第四行
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

            //获取第五行
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //查询某一天的营业数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //给明细数据填充
                sheet.getRow(7 + i).getCell(1).setCellValue(String.valueOf(begin.plusDays(i))); //填充日期
                sheet.getRow(7 + i).getCell(2).setCellValue(businessData.getTurnover()); //填充营业额
                sheet.getRow(7 + i).getCell(3).setCellValue(businessData.getValidOrderCount()); //填充有效订单
                sheet.getRow(7 + i).getCell(4).setCellValue(businessData.getOrderCompletionRate()); //填充订单完成率
                sheet.getRow(7 + i).getCell(5).setCellValue(businessData.getUnitPrice()); //填充平均客单价
                sheet.getRow(7 + i).getCell(6).setCellValue(businessData.getNewUsers()); //填充新增用户数
            }

            //3.将文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
