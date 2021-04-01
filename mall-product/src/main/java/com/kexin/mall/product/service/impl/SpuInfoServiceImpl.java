package com.kexin.mall.product.service.impl;

import com.kexin.mall.product.vo.SpuSaveVo;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.Query;

import com.kexin.mall.product.dao.SpuInfoDao;
import com.kexin.mall.product.entity.SpuInfoEntity;
import com.kexin.mall.product.service.SpuInfoService;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存所有数据 [33kb左右]
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1.保存spu基本信息 pms_sku_info
        // 插入后id自动返回注入
        this.saveBatchSpuInfo(spuInfoEntity); // this.baseMapper.insert(spuInfoEntity);
        // 此处有分布式id的问题，所以要加事务

        // 2.保存spu的表述图片  pms_spu_info_desc
        // 3.保存spu的图片集  pms_sku_images

        // 先获取所有图片
        // 保存图片的时候 并且保存这个是那个spu的图片

        // 4.保存spu的规格参数  pms_product_attr_value
        // 5.保存当前spu对应所有sku信息
        //      5.1) sku的基本信息：pms_sku_info
        //      5.2）sku的图片信息：pms_sku_images
        //      5.3) sku的销售属性: pms_sku_sale_attr_value
        //      5.4) sku的优惠、满减、会员价格等信息  [跨库]
        // 1).spu的积分信息 sms_spu_bounds

        skus.forEach(item -> {
            // 2).基本信息的保存 pms_sku_info
            // skuName 、price、skuTitle、skuSubtitle 这些属性需要手动保存

            // 设置spu的品牌id

            // 3).保存sku的图片信息  pms_sku_images
            // sku保存完毕 自增主键就出来了 收集所有图片

            // 4).sku的销售属性  pms_sku_sale_attr_value
            // 5.) sku的优惠、满减、会员价格等信息  [跨库]
        });

    }

}