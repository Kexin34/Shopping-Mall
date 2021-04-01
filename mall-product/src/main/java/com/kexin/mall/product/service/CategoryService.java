package com.kexin.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kexin.common.utils.PageUtils;
import com.kexin.mall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 16:32:03
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    /**
     * 找到catelogId的完整路径：
     * [父/子/孙]
     * @param catelogId
     * @return
     */
    Long[] findCateLogPath(Long catelogId);

    void updateCascade(CategoryEntity category);
}

