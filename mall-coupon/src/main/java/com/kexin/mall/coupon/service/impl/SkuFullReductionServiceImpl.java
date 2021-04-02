package com.kexin.mall.coupon.service.impl;

import com.kexin.common.to.MemberPrice;
import com.kexin.common.to.SkuReductionTo;
import com.kexin.mall.coupon.entity.MemberPriceEntity;
import com.kexin.mall.coupon.entity.SkuLadderEntity;
import com.kexin.mall.coupon.service.MemberPriceService;
import com.kexin.mall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.Query;

import com.kexin.mall.coupon.dao.SkuFullReductionDao;
import com.kexin.mall.coupon.entity.SkuFullReductionEntity;
import com.kexin.mall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {
    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReductionTo(SkuReductionTo skuReductionTo) {
        // 6.4) sku的优惠、满减、会员价格等信息  [跨库](mall_sms->sms_sku_ladder\sms-sku_full_reduction\sms_member_price)

        // 1.sms_sku_ladder 阶梯式打折（满几件打几折）
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
//        BeanUtils.copyProperties(skuReductionTo,skuLadderEntity);
        skuLadderService.save(skuLadderEntity);

        // 2. sms-sku_full_reduction 满多少减多少
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo,reductionEntity);
        this.save(reductionEntity);

        // 3.sms_member_price 会员价格
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();

        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity priceEntity = new MemberPriceEntity();
            priceEntity.setSkuId(skuReductionTo.getSkuId());
            priceEntity.setMemberLevelId(item.getId());
            priceEntity.setMemberLevelName(item.getName());
            priceEntity.setMemberPrice(item.getPrice());
            priceEntity.setAddOther(1);
            return  priceEntity;
        }).collect(Collectors.toList());
    }

}