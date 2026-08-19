package com.sky.service;

import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 分页查询套餐
     * @param dto
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dto);

    /**
     * 根据id查询套餐数据
     * @param id
     * @return
     */
    SetmealVO getById(Long id);

    /**
     * 修改套餐和套餐菜品数据
     * @param setmealDTO
     */
    void updateSetmealWithSetmealDishes(SetmealDTO setmealDTO);

    /**
     * 批量删除套餐数据和关联菜品数据
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 启售和停售套餐
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

}
