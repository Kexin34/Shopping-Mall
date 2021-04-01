package com.kexin.mall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.kexin.mall.product.entity.AttrEntity;
import com.kexin.mall.product.service.AttrService;
import com.kexin.mall.product.service.CategoryService;
import com.kexin.mall.product.vo.AttrGroupRelationVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.kexin.mall.product.entity.AttrGroupEntity;
import com.kexin.mall.product.service.AttrGroupService;
import com.kexin.common.utils.PageUtils;
import com.kexin.common.utils.R;



/**
 * 属性分组
 *
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 17:50:29
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    AttrService attrService;

    /**
     * 列表
     * @param  catelogId 0的话查所有
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params,@PathVariable Long catelogId){
        //        PageUtils page = attrGroupService.queryPage(params);

        PageUtils page = attrGroupService.queryPage(params,catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        // 用当前当前分类id查询完整路径并写入 attrGroup
        Long[] catelogPath = categoryService.findCateLogPath(attrGroup.getCatelogId());
        attrGroup.setCatelogPath(catelogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }



    @GetMapping ("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        // 获取到当前分组 关联的所有属性
        List<AttrEntity> attrEntities= attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", attrEntities);
    }

    ///product/attrgroup/attr/relation/delete
    @PostMapping("attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrService.deleteRelation(vos);
        return R.ok();
    }




}
