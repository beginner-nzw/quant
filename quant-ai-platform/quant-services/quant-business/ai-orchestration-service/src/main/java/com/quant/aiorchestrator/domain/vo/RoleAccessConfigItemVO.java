package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoleAccessConfigItemVO {

    private String roleCode;
    private String roleName;
    private String roleDescription;
    private List<String> menuKeys;
    private List<String> permissionKeys;
    private String remark;
}
