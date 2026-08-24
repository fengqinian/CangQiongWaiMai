package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 统计指定时间内的营业额
     * @return
     */
    TurnoverReportVO getByTurnover(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间内的用户数量
     * @return
     */
    UserReportVO getByUser(LocalDate begin, LocalDate end);
}
