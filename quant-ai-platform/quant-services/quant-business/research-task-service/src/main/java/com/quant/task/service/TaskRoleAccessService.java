package com.quant.task.service;

public interface TaskRoleAccessService {
    String PERMISSION_TASK_CREATE = "TASK_CREATE";
    String PERMISSION_TASK_RETRY = "TASK_RETRY";
    String PERMISSION_TASK_CANCEL = "TASK_CANCEL";

    void requirePermission(String permissionKey);
}
