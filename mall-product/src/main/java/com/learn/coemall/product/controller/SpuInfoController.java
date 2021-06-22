package com.learn.coemall.product.controller;

import java.util.Arrays;
import java.util.Map;

import com.learn.coemall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.learn.coemall.product.entity.SpuInfoEntity;
import com.learn.coemall.product.service.SpuInfoService;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.R;


/**
 * spu信息
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 12:58:33
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    @GetMapping("/skuId/{id}")
    public R getSpuInfoBySkuId(@PathVariable("id") Long id){
        SpuInfoEntity entity = spuInfoService.getSpuInfoBySkuId(id);

        return R.ok().put("data",entity);
    }

    // /product/spuinfo/{spuId}/up
    @PostMapping("/{spuId}/up")
    public R spuUp(@PathVariable("spuId") Long spuId){
        spuInfoService.up(spuId);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SpuSaveVo vo) {
//        spuInfoService.save(spuInfo);
        spuInfoService.saveSpuInfo(vo);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo) {
        spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
