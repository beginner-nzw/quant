package com.quant.config.port;

import com.quant.config.api.AgentConfigItem;

import java.util.List;

public interface AgentConfigQueryPort {

    List<? extends AgentConfigItem> loadAgents();
}
