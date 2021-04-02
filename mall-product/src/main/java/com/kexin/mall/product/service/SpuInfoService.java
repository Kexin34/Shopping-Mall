package com.kexin.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kexin.common.utils.PageUtils;
import com.kexin.mall.product.entity.SpuInfoDescEntity;
import com.kexin.mall.product.entity.SpuInfoEntity;
import com.kexin.mall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 16:32:02
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);


}

