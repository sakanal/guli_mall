package com.sakanal.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sakanal.coupon.entity.CouponEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 *
 * @author sakanal
 * @email sakanal9527@gmail.com
 * @date 2022-12-21 13:36:06
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {

}
