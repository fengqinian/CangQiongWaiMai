package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 插入订单明细数据
     * @param orderDetailList
     */
    void insert(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询订单详细信息
     * @param orderIds
     * @return
     */
    List<OrderDetail> getByOrderId(List<Long> orderIds);
}
