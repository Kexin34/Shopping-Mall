package com.kexin.mall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo{
    private String catelogName;  // 所属分类的名字 "手机/数码/手机"
    private String groupName;   // 所属分组名字  “主体”

    private Long[] catelogPath;

}
