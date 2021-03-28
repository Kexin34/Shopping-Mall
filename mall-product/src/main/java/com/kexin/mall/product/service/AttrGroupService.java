package com.kexin.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kexin.common.utils.PageUtils;
import com.kexin.mall.product.entity.AttrGroupEntity;

import java.util.Map;

/**
 * 属性分组
 *
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 16:32:03
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

