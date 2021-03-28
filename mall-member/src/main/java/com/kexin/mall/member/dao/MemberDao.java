package com.kexin.mall.member.dao;

import com.kexin.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 19:01:58
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
