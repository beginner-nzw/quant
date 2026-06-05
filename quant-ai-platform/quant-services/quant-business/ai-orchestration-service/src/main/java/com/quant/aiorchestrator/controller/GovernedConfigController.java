package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import com.quant.aiorchestrator.domain.dto.ConfigRollbackDTO;
import com.quant.aiorchestrator.domain.vo.ConfigRollbackResultVO;
import com.quant.aiorchestrator.domain.vo.GovernedConfigVO;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.common.core.exception.BizException;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class GovernedConfigController {

    private final GovernedConfigStore governedConfigStore;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/config-store/{storeCode}")
    public Result<GovernedConfigVO> readConfig(@PathVariable("storeCode") String storeCode) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW);
        ConfigStoreKey key = resolveKey(storeCode);
        GovernedConfigVO vo = new GovernedConfigVO();
        vo.setStoreCode(key.storeCode());
        vo.setConfigPath(governedConfigStore.displayPath(key));
        vo.setRoot(governedConfigStore.readRoot(key, emptyRootFor(key)));
        vo.setAudits(governedConfigStore.listAudit(key));
        return Result.success(vo);
    }

    @PostMapping("/config-store/{storeCode}/rollback")
    public Result<ConfigRollbackResultVO> rollback(@PathVariable("storeCode") String storeCode,
                                                   @RequestBody ConfigRollbackDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT);
        if (dto == null || dto.getVersionId() == null || dto.getVersionId().isBlank()) {
            throw new BizException("CONFIG_ROLLBACK_VERSION_EMPTY", "rollback version cannot be empty");
        }
        ConfigStoreKey key = resolveKey(storeCode);
        GovernedConfigStore.ConfigWriteResult writeResult = governedConfigStore.rollback(key, dto.getVersionId(), dto.getReason());
        ConfigRollbackResultVO vo = new ConfigRollbackResultVO();
        vo.setStoreCode(key.storeCode());
        vo.setVersionId(writeResult.versionId());
        vo.setVersion(writeResult.version());
        vo.setConfigPath(writeResult.configPath());
        return Result.success(vo);
    }

    private ConfigStoreKey resolveKey(String storeCode) {
        try {
            return ConfigStoreKey.fromStoreCode(storeCode);
        } catch (IllegalArgumentException e) {
            throw new BizException("CONFIG_STORE_UNSUPPORTED", e.getMessage());
        }
    }

    private Map<String, Object> emptyRootFor(ConfigStoreKey key) {
        Map<String, Object> root = new LinkedHashMap<>();
        switch (key) {
            case AGENT -> root.put("agents", java.util.List.of());
            case WORKFLOW -> root.put("workflows", java.util.List.of());
            case MODEL_STRATEGY -> root.put("strategies", java.util.List.of());
            case EVENT_SOURCE -> root.put("sources", java.util.List.of());
            case EVENT_AUTO_TRIGGER -> {
                root.put("enabled", false);
                root.put("rules", java.util.List.of());
            }
            case ROLE_ACCESS -> root.put("roles", java.util.List.of());
            case PROMPT_TEMPLATE -> root.put("templates", new LinkedHashMap<>());
            default -> {
            }
        }
        return root;
    }
}
