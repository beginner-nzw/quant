package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleAccessConfigUpdateDTO {

    private String roleName;
    private String roleDescription;
    private List<String> menuKeys;
    private List<String> permissionKeys;
    private String remark;
}
