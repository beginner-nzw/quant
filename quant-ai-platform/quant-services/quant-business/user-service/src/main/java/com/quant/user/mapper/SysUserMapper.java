package com.quant.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.user.domain.entity.SysUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {
}
