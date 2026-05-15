package com.quant.common.redis;

public final class RedisKeyConstants {

    public static final String TASK_STATE = "task:state:%s";
    public static final String TASK_RESULT = "task:result:%s";
    public static final String TASK_FULL = "task:full:%s";
    public static final String TASK_CONTROL = "task:control:%s";
    public static final String TASK_META = "task:meta:%s";

    public static final String TASK_STATS_GLOBAL = "task:stats:global";
    public static final String TASK_LIST_VERSION = "task:list:version";
    public static final String TASK_LIST_CACHE_PREFIX = "task:list:";

    public static final String SIGNAL_LATEST = "signal:latest:%s";
    public static final String SIGNAL_RANKING = "signal:ranking:%s";

    private RedisKeyConstants() {
    }
}
