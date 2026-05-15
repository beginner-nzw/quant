package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface RoleAccessConfigService {
        String PERMISSION_TASK_VIEW = "TASK_VIEW";
        String PERMISSION_TASK_CREATE = "TASK_CREATE";
        String PERMISSION_TASK_RETRY = "TASK_RETRY";
        String PERMISSION_TASK_CANCEL = "TASK_CANCEL";
        String PERMISSION_AUDIT_COMPLIANCE_VIEW = "AUDIT_COMPLIANCE_VIEW";
        String PERMISSION_REPORT_REVIEW = "REPORT_REVIEW";
        String PERMISSION_MODEL_AGENT_CONFIG_VIEW = "MODEL_AGENT_CONFIG_VIEW";
        String PERMISSION_MODEL_AGENT_CONFIG_EDIT = "MODEL_AGENT_CONFIG_EDIT";
        public List<RoleAccessConfigItemVO> loadRoles();

        public boolean hasPermissionForCurrentRole(String permissionKey);

        public boolean hasPermission(String currentRole, String permissionKey);

        public void requirePermission(String permissionKey);

        public void saveRole(String roleCode, RoleAccessConfigUpdateDTO dto);

        public String resolveConfigPathForDisplay();
}
