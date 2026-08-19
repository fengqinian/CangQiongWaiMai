package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    public SetmealMapper setmealMapper;
    @Autowired
    public SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealDTO, setmeal);

        //新增套餐
        setmealMapper.insert(setmeal);

        //获取插入返回的id
        Long id = setmeal.getId();

        //新增套餐菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);
        });

        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 分页查询套餐
     * @param dto
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(dto);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id查询套餐数据
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        //查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);

        //查询套餐关联菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getById(id);

        //组装数据
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐和套餐菜品数据
     * @param setmealDTO
     */
    @Transactional
    public void updateSetmealWithSetmealDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();

        //修改套餐数据
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //删除菜品数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        //新增菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });

        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 批量删除套餐数据和关联菜品数据
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids) {
        //判断套餐是否能被删除 - 是否在停售状态
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //删除套餐数据
        setmealMapper.deleteByIds(ids);

        //删除菜品数据
        setmealDishMapper.deleteByIds(ids);
    }

    /**
     * 启售和停售套餐
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //判断套餐中是否有菜品没有启售 - 有菜品没有启售则套餐不能启售
        if (status == StatusConstant.ENABLE) {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            dishList.forEach(dish -> {
                if (dish.getStatus() == StatusConstant.DISABLE) {
                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            });
        }

        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();

        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
