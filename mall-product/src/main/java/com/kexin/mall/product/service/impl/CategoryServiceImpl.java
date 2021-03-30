package com.kexin.mall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.Query;

import com.kexin.mall.product.dao.CategoryDao;
import com.kexin.mall.product.entity.CategoryEntity;
import com.kexin.mall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1、查出所有分类

        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2、组装成父子的树形结构
        //      2.1）找到所有一级分类
        //      2.2）将当前菜单的子分类写进去（用一个递归方法找到所有子菜单）
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getCatLevel() == 1)
                .map(menu -> {
                    // 递归找到当前遍历菜单的所有子菜单
                    menu.setChildren(getChildens(menu, entities));
                    return menu;
                })
                // 找到子菜单后，排序子菜单。这里的空判断是为了防止了空指针
                .sorted(Comparator.comparingInt(item -> (item.getSort() == null ? 0 : item.getSort())))
                .collect(Collectors.toList());
        return level1Menus;
    }

    /**
     * 递归查找当前菜单的子菜单
     * @param root  当前菜单
     * @param all   从哪里获得菜单（所有菜单）
     * @return
     */
    private List<CategoryEntity> getChildens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> childList = all.stream()
                // 规范tips：包装类型间的相等判断应该用equals，而不是用==
                // 1、找到子菜单
                .filter(item -> item.getParentCid().equals(root.getCatId()))
                .map(menu -> {
                    menu.setChildren(getChildens(menu, all));
                    return menu;
                })
                // 2、菜单的排序
                .sorted(Comparator.comparingInt(item -> (item.getSort() == null ? 0 : item.getSort())))
                .collect(Collectors.toList());
        return childList;
    }

}