package com.quant.api.user;

import com.quant.api.user.dto.UserCreateDTO;
import com.quant.api.user.dto.UserRoleGrantDTO;
import com.quant.api.user.vo.UserProfileVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserApiBoundaryTests {

    @Test
    void userPermissionContractsBelongToApiModule() {
        assertEquals("com.quant.api.user", UserPermissionPort.class.getPackageName());
        assertEquals("com.quant.api.user.dto", UserCreateDTO.class.getPackageName());
        assertEquals(UserCreateDTO.class.getPackageName(), UserRoleGrantDTO.class.getPackageName());
        assertEquals("com.quant.api.user.vo", UserProfileVO.class.getPackageName());
    }
}
