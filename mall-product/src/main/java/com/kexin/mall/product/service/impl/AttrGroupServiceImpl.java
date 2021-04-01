package com.kexin.mall.product.service.impl;

import com.kexin.mall.product.entity.AttrEntity;
import com.kexin.mall.product.service.AttrService;
import com.kexin.mall.product.vo.AttrGroupWithAttrsVo;
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

import com.kexin.mall.product.dao.AttrGroupDao;
import com.kexin.mall.product.entity.AttrGroupEntity;
import com.kexin.mall.product.service.AttrGroupService;

import org.springframework.util.StringUtils;

@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override   // 按关键字或者按id查
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        // 检索条件（常用与模糊查询关键字）
        String key = (String) params.get("key");

        // 构造检索条件
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();

        // select * from AttrGroup where attr_group_id=key or attr_group_name=key
        // key不为空
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> obj
                    .eq("attr_group_id", key)
                    .or()
                    .like("attr_group_name", key)
            );
        }

        // 0表示查询所有
        if (catelogId == 0) {
            // Query可以把map封装为IPage // this.page(IPage,QueryWrapper)
            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        } else {
            // 非0，要按照三级分类查询
            // 增加id信息
            wrapper.eq("catelog_id", catelogId);

            IPage<AttrGroupEntity> page =
                    this.page(new Query<AttrGroupEntity>().getPage(params),
                            wrapper);
            return new PageUtils(page);
        }
    }

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性（用作规格参数）
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrgroupWithAttrsByCatelogId(Long catelogId) {

        // 1. 查询分组信息（要用关联关系来查询）
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        // 2、查询所有属性（基于分组）
        List<AttrGroupWithAttrsVo> collect = groupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrsVO = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, attrsVO);

            // 查询这个分组下的所有属性(按照分组的id）
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVO.getAttrGroupId());
            attrsVO.setAttrs(attrs);

            return attrsVO;
        }).collect(Collectors.toList());
        return collect;
    }

}