package com.learn.coemall.ware.service.impl;

import com.learn.coemall.ware.entity.PurchaseDetailEntity;
import com.learn.coemall.ware.service.PurchaseDetailService;
import com.learn.coemall.ware.vo.MergeVo;
import com.learn.common.constant.WareConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.ware.dao.PurchaseDao;
import com.learn.coemall.ware.entity.PurchaseEntity;
import com.learn.coemall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    /**
     * 合并采购单
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null){
            //新建
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());

            baseMapper.insert(purchaseEntity);

            purchaseId = purchaseEntity.getId();
        }else {
            //确认采购单状态是0，1才可以合并
            PurchaseEntity purchaseEntity = getById(purchaseId);
            if (purchaseEntity.getStatus() != WareConstant.PurchaseStatusEnum.CREATED.getCode()
                || purchaseEntity.getStatus() != WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()){
                log.error("采购单状态是0，1才可以合并");
                return;
            }
        }

        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = mergeVo.getItems().stream().map(i -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());

            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(collect);

        //更新 更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        updateById(purchaseEntity);
    }

    /**
     * 领取采购单
     */
    @Override
    public void received(List<Long> ids) {
        List<PurchaseEntity> collect = ids.stream().map(this::getById)
                .filter(item -> {
                    //确认当前采购单是新建或已分配状态
                    return item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
                }).map(item -> {
                    item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                    item.setUpdateTime(new Date());
                    return item;
                }).collect(Collectors.toList());

        //改变采购单的状态
        updateBatchById(collect);

        //改变采购项的状态
        collect.forEach(item -> {
            List<PurchaseDetailEntity> entities = purchaseDetailService.list(new QueryWrapper<PurchaseDetailEntity>()
                    .eq("purchase_id", item.getId()));
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(entity.getId());
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());

                return purchaseDetailEntity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }

}