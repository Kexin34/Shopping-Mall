package com.kexin.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kexin.common.utils.PageUtils;
import com.kexin.mall.product.entity.SkuInfoEntity;

import java.util.Map;

/**
 * sku信息
 *
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 16:32:02
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuInfo(SkuInfoEntity skuInfoEntity);
}

