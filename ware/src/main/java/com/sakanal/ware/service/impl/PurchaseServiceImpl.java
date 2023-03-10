package com.sakanal.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakanal.common.constant.WareConstant;
import com.sakanal.common.utils.PageUtils;
import com.sakanal.common.utils.Query;
import com.sakanal.ware.dao.PurchaseDao;
import com.sakanal.ware.entity.PurchaseDetailEntity;
import com.sakanal.ware.entity.PurchaseEntity;
import com.sakanal.ware.service.PurchaseDetailService;
import com.sakanal.ware.service.PurchaseService;
import com.sakanal.ware.service.WareSkuService;
import com.sakanal.ware.vo.MergeVo;
import com.sakanal.ware.vo.PurchaseDoneVo;
import com.sakanal.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }
    @Override
    public PageUtils queryPageUnReceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);

    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId(); // ?????????id
        // ???????????????????????????id???????????????????????????
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();

            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity); // ?????????????????????
            purchaseId = purchaseEntity.getId(); // ????????????????????????id

        }

        //TODO ???????????????status???0???1???????????????

        // ???????????????????????????id
        List<Long> items = mergeVo.getItems();
        // ????????????????????????????????????????????????????????????????????????final?????????????????????????????????????????????????????????purchaseId???????????????????????????????????????????????????finalPurchaseId
        Long finalPurchaseId = purchaseId;
        // ??????id????????????????????????????????????purchase_id?????????
        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();

            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(collect);
    }

    @Override
    public void received(List<Long> ids) {
        // 1???????????????????????????????????????????????????????????????
        // 1.1?????????id?????????purchaseEntity
        List<PurchaseEntity> purchaseEntities = ids.stream().map(this::getById).filter(purchaseEntity -> {
            // 1.2????????????????????????0???1???purchaseEntity
            return purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
        }).peek(purchaseEntity -> {
            // ??????purchaseEntity????????????????????????????????????
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            purchaseEntity.setUpdateTime(new Date());
        }).collect(Collectors.toList());

        // 2???????????????????????????
        this.updateBatchById(purchaseEntities);

        // 3??????????????????????????????
        purchaseEntities.forEach(purchaseEntity -> {
            // 3.1???????????????????????????????????????
            List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.listDetailByPurChaseId(purchaseEntity.getId());
            // 3.2????????????????????????????????????
            purchaseDetailEntities.forEach(purchaseDetailEntity -> {
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
            });
            purchaseDetailService.updateBatchById(purchaseDetailEntities);
        });

    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        // 1???????????????????????????
        AtomicBoolean flag = new AtomicBoolean(true);
        List<PurchaseItemDoneVo> purchaseItemDoneVos = doneVo.getItems(); // ??????????????????????????????

        // ???purchaseItemDoneVos?????????purchaseDetailEntities
        List<PurchaseDetailEntity> purchaseDetailEntities = purchaseItemDoneVos.stream().map(item -> {
            // ??????id????????????????????????????????????
            PurchaseDetailEntity purchaseDetailEntity = purchaseDetailService.getById(item.getItemId());
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag.set(false);
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
            } else {
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                // 2?????????????????????????????????
                wareSkuService.addStock(purchaseDetailEntity.getSkuId(), purchaseDetailEntity.getWareId(), purchaseDetailEntity.getSkuNum());

            }
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        // ????????????????????????
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        // 2????????????????????????
        PurchaseEntity purchaseEntity = this.getById(doneVo.getId());
        purchaseEntity.setStatus(flag.get() ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }


}
