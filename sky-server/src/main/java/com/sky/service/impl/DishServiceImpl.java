package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 新增菜品和菜品口味
     * @param dishDTO
     */
    @Transactional
    public void saveDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);

        //获取菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });

            dishFlavorMapper.insert(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult queryPage(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除菜品
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids) {
        //判断菜品是否能被删除 - 是否是停售状态
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //判断菜品是否能被删除 - 是否被套餐关联了
        List<Long> count = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (count != null && !count.isEmpty()) {
            //当前菜品不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品数据
        dishMapper.deleteByIds(ids);

        //删除菜品口味数据
        dishFlavorMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询菜品数据
     * @param id
     * @return
     */
    public DishVO getDishWithDishFlavorById(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        //根据id查询菜品口味数据
        List<DishFlavor> dishFlavor = dishFlavorMapper.getById(id);

        //将查询到的结果封装
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    public void updateByIdWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品数据
        dishMapper.update(dish);

        //删除菜品口味数据
        dishFlavorMapper.deleteById(dishDTO.getId());

        //新增菜品口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });

            dishFlavorMapper.insert(flavors);
        }
    }

    /**
     * 启售停售菜品
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();

        dishMapper.update(dish);

        //如果是停售操作,则菜品关联的套餐也要停售
        if (status == StatusConstant.DISABLE) {
            ArrayList<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIdsByDishIds != null && !setmealIdsByDishIds.isEmpty()) {
                for (Long setmealId : setmealIdsByDishIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .status(status)
                            .id(setmealId)
                            .build();

                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> dishes = dishMapper.getByCategoryId(categoryId);
        return dishes;
    }
}
