package com.learn.coemall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.learn.coemall.ware.vo.LockStockResult;
import com.learn.coemall.ware.vo.WareSkuLockVo;
import com.learn.common.exception.BizCodeEnum;
import com.learn.common.exception.NoStockException;
import com.learn.common.to.SkuHasStockTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.learn.coemall.ware.entity.WareSkuEntity;
import com.learn.coemall.ware.service.WareSkuService;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.R;


/**
 * 商品库存
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:37:26
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){
        try {
            Boolean stock = wareSkuService.orderLockStock(vo);
            return R.ok().put("data",stock);
        }catch (NoStockException e){
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION.getCode(), BizCodeEnum.NO_STOCK_EXCEPTION.getMsg());
        }



    }

    //查询sku是否有库存
    @PostMapping("/hasstock")
    public List<SkuHasStockTo> getSkuHasStock(@RequestBody List<Long> skuIds){
        return wareSkuService.getSkuHasStock(skuIds);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
