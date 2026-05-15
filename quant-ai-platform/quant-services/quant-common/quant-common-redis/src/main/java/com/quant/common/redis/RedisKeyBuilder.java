package com.quant.common.redis;

public final class RedisKeyBuilder {

    public static String taskState(String taskId) {
        return String.format(RedisKeyConstants.TASK_STATE, taskId);
    }

    public static String taskResult(String taskId) {
        return String.format(RedisKeyConstants.TASK_RESULT, taskId);
    }

    public static String taskFull(String taskId) {
        return String.format(RedisKeyConstants.TASK_FULL, taskId);
    }

    public static String taskControl(String taskId) {
        return String.format(RedisKeyConstants.TASK_CONTROL, taskId);
    }

    public static String taskMeta(String taskId) {
        return String.format(RedisKeyConstants.TASK_META, taskId);
    }

    public static String signalLatest(String entityCode) {
        return String.format(RedisKeyConstants.SIGNAL_LATEST, entityCode);
    }

    public static String signalRanking(String signalDate) {
        return String.format(RedisKeyConstants.SIGNAL_RANKING, signalDate);
    }

    private RedisKeyBuilder() {
    }
}
