package com.sky.controller.admin;

import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理")
public class SetmealController {

    @Autowired
    public SetmealService setmealService;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐: {}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /**
     * 分页查询套餐
     * @param dto
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> pageQuery(DishPageQueryDTO dto) {
        log.info("分页查询套餐: {}", dto);
        PageResult pageResult = setmealService.pageQuery(dto);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询套餐数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐数据")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐数据: {}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐和套餐菜品数据
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐和套餐菜品数据")
    public Result updateSetmealWithSetmealDishes(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐和套餐菜品数据: {}", setmealDTO);
        setmealService.updateSetmealWithSetmealDishes(setmealDTO);
        return Result.success();
    }

    /**
     * 批量删除套餐数据和关联菜品数据
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐数据和关联菜品数据")
    public Result deleteByIds(@RequestParam List<Long> ids) {
        log.info("批量删除套餐数据和关联菜品数据: {}", ids);
        setmealService.deleteByIds(ids);
        return Result.success();
    }

    /**
     * 启售和停售套餐
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启售和停售套餐: {}, {}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
