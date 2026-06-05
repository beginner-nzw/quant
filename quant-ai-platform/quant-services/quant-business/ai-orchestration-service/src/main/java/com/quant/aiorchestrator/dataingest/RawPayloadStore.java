package com.quant.aiorchestrator.dataingest;

public interface RawPayloadStore {
    String save(String sourceCode, String stage, Object payload);
}
