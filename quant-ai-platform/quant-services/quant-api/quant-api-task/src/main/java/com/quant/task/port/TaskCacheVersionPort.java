package com.quant.task.port;

public interface TaskCacheVersionPort {

    String currentVersion();

    void bumpVersion();
}
