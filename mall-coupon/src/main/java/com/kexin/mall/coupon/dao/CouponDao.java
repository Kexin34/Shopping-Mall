package com.kexin.mall.coupon.dao;

import com.kexin.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author kexinwen
 * @email kexinwen.ca@gmail.com
 * @date 2021-03-28 18:44:16
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
