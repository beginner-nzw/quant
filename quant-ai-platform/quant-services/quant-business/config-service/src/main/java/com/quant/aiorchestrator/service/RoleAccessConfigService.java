package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;

import java.util.List;

public interface RoleAccessConfigService extends RoleAccessPermissionPort {
    String PERMISSION_TASK_VIEW = RoleAccessPermissions.TASK_VIEW;
    String PERMISSION_TASK_CREATE = RoleAccessPermissions.TASK_CREATE;
    String PERMISSION_TASK_RETRY = RoleAccessPermissions.TASK_RETRY;
    String PERMISSION_TASK_CANCEL = RoleAccessPermissions.TASK_CANCEL;
    String PERMISSION_AUDIT_COMPLIANCE_VIEW = RoleAccessPermissions.AUDIT_COMPLIANCE_VIEW;
    String PERMISSION_REPORT_REVIEW = RoleAccessPermissions.REPORT_REVIEW;
    String PERMISSION_MODEL_AGENT_CONFIG_VIEW = RoleAccessPermissions.MODEL_AGENT_CONFIG_VIEW;
    String PERMISSION_MODEL_AGENT_CONFIG_EDIT = RoleAccessPermissions.MODEL_AGENT_CONFIG_EDIT;

    List<RoleAccessConfigItemVO> loadRoles();

    @Override
    boolean hasPermissionForCurrentRole(String permissionKey);

    @Override
    boolean hasPermission(String currentRole, String permissionKey);

    @Override
    void requirePermission(String permissionKey);

    void saveRole(String roleCode, RoleAccessConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
