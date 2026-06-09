package com.quant.aiorchestrator.dataingest;

public enum SourceFetchStatus {
    FETCHED,
    FETCH_FAILED,
    STANDARDIZED,
    STANDARDIZE_FAILED,
    DEADLETTERED
}
