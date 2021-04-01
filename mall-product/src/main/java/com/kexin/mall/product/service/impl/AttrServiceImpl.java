package com.kexin.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kexin.common.constant.ProductConstant;
import com.kexin.mall.product.dao.AttrAttrgroupRelationDao;
import com.kexin.mall.product.dao.AttrGroupDao;
import com.kexin.mall.product.dao.CategoryDao;
import com.kexin.mall.product.entity.AttrAttrgroupRelationEntity;
import com.kexin.mall.product.entity.AttrGroupEntity;
import com.kexin.mall.product.entity.CategoryEntity;
import com.kexin.mall.product.service.CategoryService;
import com.kexin.mall.product.vo.AttrGroupRelationVo;
import com.kexin.mall.product.vo.AttrRespVo;
import com.kexin.mall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.Query;

import com.kexin.mall.product.dao.AttrDao;
import com.kexin.mall.product.entity.AttrEntity;
import com.kexin.mall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();// 这是PO，用来对应数据库
        // 重要的工具
        BeanUtils.copyProperties(attrVo, attrEntity);// 把页面VO的值，封装到PO中
        //1、保存基本数据
        this.save(attrEntity);
        //2、保存关联关系
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attrVo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            //relationEntity.setAttrSort(0);
            relationDao.insert(relationEntity);
        }
    }

    /**
     *
     * 分页模糊查询  ，比如按分类查属性、按属性类别查属性
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
    // 传入的attrType是"base"或其他，但是数据库存的是 "0"销售 / "1"基本

        // 属性都在pms_attr表中混合着
        QueryWrapper<AttrEntity> wrapper =
                new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type)
                        ?ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                        :ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

//        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type) ? 1 : 0 );

        // 如果参数带有分类id，则按分类查询
        if (catelogId != 0L ) {
            wrapper.eq("catelog_id", catelogId);
        }
        // 支持模糊查询，用id或者name查
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        // 正式查询满足条件的属性
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );

        List<AttrEntity> records = page.getRecords();
        PageUtils pageUtils = new PageUtils(page);

        // 查到属性后还要结合分类名字、分组名字(分类->属性->分组) 封装为AttrRespVo对象
        List<AttrRespVo> attrRespVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);// 先把基本属性拷贝过来

            // 1.设置分类和分组的名字  先获取中间表对象  给attrRespVo 封装分组名字
            if("base".equalsIgnoreCase(type)){ // 如果是规格参数才查询，或者说销售属性没有属性分组，只有分类
                // 根据属性id查询关联表，得到其属性分组
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId);
                    // 设置属性分组的名字
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
            // 2.查询分类id 给attrRespVo 封装三级分类名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(attrRespVos);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
       // 先查到attr的详细信息
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);// 先把基本属性拷贝过来


        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 1、设置分组信息
            // 分组关联信息
            AttrAttrgroupRelationEntity attrgroupRelation =
                    relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if(attrgroupRelation != null){
                respVo.setAttrGroupId((attrgroupRelation.getAttrGroupId()));  // 根据分组关联信息，去得到分组ID
                //设置分组名
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
                if (attrGroupEntity != null){
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 2、设置分类信息（用分类id找出完整的分类路径）
        Long catelogId = attrEntity.getCatelogId();
        //查询并设置分类路径
        Long[] catelogPath = categoryService.findCateLogPath(catelogId);
        respVo.setCatelogPath(catelogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null){
            respVo.setCatelogName(categoryEntity.getName());
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.baseMapper.updateById(attrEntity);   // 到这里都是基本的修改

        //只有当属性分组不为空时，说明更新的是规则参数，则需要更新关联表
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 1、修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", relationEntity.getAttrId()));
            if (count > 0) {
                relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            }else{
                relationDao.insert(relationEntity);
            }
        }
    }

    /**
     * 查询分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId)
        );
        List<AttrEntity> attrEntities = relationEntities.stream().map((entity) -> {
            AttrEntity attrEntity = baseMapper.selectById(entity.getAttrId());
            return attrEntity;
        }).collect(Collectors.toList());
        return attrEntities;
    }

    // 批量删除
    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.stream(vos).map(item -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        //delete from pms_attr_attrgroup_relation where (attr_id=? and attr_group_id=?) or (attr_id=? and attr_group_id=?)
        relationDao.deleteBatchRelation(relationEntities);

    }

}