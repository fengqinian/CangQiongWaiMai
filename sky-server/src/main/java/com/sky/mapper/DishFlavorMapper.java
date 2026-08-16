package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 插入菜品口味数据
     * @param flavors
     */
    void insert(List<DishFlavor> flavors);

    /**
     * 批量删除菜品口味数据
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据id删除菜品口味数据
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteById(Long dishId);

    /**
     * 根据id查询菜品口味数据
     * @param dishId
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getById(Long dishId);
}
