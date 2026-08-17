package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 查询是否有菜品关联套餐
     * @param ids
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    /**
     * 插入菜品和套餐关联数据
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据id查询套餐关联菜品数据
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getById(Long setmealId);

    /**
     * 根据套餐id删除菜品数据
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐id批量删除菜品数据
     * @param ids
     */
    void deleteByIds(List<Long> ids);
}
