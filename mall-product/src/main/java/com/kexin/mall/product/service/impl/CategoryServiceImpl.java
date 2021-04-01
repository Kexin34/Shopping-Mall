package com.kexin.mall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.Query;

import com.kexin.mall.product.dao.CategoryDao;
import com.kexin.mall.product.entity.CategoryEntity;
import com.kexin.mall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    private CategoryBrandRelationServiceImpl categoryBrandRelationService;

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

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除菜单是否被别的地方引用

        // 逻辑删除（非物理删除，只是改变数据库字段show_status）
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        paths = findParentPath(catelogId, paths);

        // 收集的时候是顺序 前端是逆序显示的 所以用集合工具类给它逆序一下
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional  //因为涉及到多次修改，因此要开启事务
    @Override
    //@CacheEvict(value = {"category"},allEntries = true)
    public void updateCascade(CategoryEntity category) {
        this.updateById(category); // 更新自己
        if (!StringUtils.isEmpty(category.getName())) { // 更新关联表里面的
            categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
        }
    }

    /**
     * 递归收集所有父节点
     */
    private List<Long> findParentPath(Long catlogId, List<Long> paths) {
        // 1、收集当前节点id
        paths.add(catlogId);
        CategoryEntity byId = this.getById(catlogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
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