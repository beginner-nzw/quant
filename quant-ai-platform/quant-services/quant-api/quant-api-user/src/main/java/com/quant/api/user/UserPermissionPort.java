package com.quant.api.user;

import com.quant.api.user.dto.UserCreateDTO;
import com.quant.api.user.dto.UserRoleGrantDTO;
import com.quant.api.user.vo.UserProfileVO;

public interface UserPermissionPort {

    UserProfileVO createUser(UserCreateDTO dto);

    UserProfileVO grantRoles(String userId, UserRoleGrantDTO dto);

    UserProfileVO getUserProfile(String userId);
}
