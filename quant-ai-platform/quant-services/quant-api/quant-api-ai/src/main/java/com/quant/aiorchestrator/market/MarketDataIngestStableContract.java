package com.quant.aiorchestrator.market;

public final class MarketDataIngestStableContract {

    public static final String LEGACY_TASK_API_BASE = "/api/tasks";

    public static final String MARKET_EVENT_TABLE = "market_event";
    public static final String MARKET_EVENT_RELATION_TABLE = "market_event_relation";
    public static final String MARKET_EVENT_ANALYSIS_TABLE = "market_event_analysis";
    public static final String DATA_INGEST_RAW_PAYLOAD_STORE = "data-ingest.raw-payload-store";
    public static final String DATA_INGEST_RUN_TABLE = "market_event_ingest_run";
    public static final String DATA_INGEST_HISTORY_STORE = "event-ingest-histories.json";

    public static final String MARKET_EVENT_STATS = "/market-event-stats";
    public static final String MARKET_EVENTS = "/market-events";
    public static final String MARKET_EVENT_DETAIL = "/market-events/{eventId}";
    public static final String MARKET_EVENT_INGEST_HISTORY = "/market-events/ingest-history";
    public static final String MARKET_EVENT_SOURCE_CONFIGS = "/market-event-source-configs";
    public static final String MARKET_EVENT_BATCH_IMPORT_PREVIEW = "/market-events/batch-import/preview";
    public static final String MARKET_EVENT_BATCH_IMPORT = "/market-events/batch-import";
    public static final String MARKET_EVENT_MOCK_INGEST = "/market-events/mock-ingest";
    public static final String MARKET_EVENT_SOURCE_SYNC = "/market-events/source-sync/{sourceCode}";
    public static final String MARKET_EVENT_SOURCE_PREVIEW = "/market-events/source-preview/{sourceCode}";
    public static final String MARKET_EVENT_SOURCE_DIAGNOSE = "/market-events/source-diagnose/{sourceCode}";
    public static final String MARKET_EVENT_CNINFO_PROXY = "/market-events/cninfo-proxy";
    public static final String MARKET_INTELLIGENCE = "/market-intelligence";
    public static final String MARKET_INTELLIGENCE_STATS = "/market-intelligence-stats";

    public static final String SOURCE_SYNC_OPERATION = "SOURCE_SYNC";
    public static final String SOURCE_PREVIEW_OPERATION = "SOURCE_PREVIEW";
    public static final String SOURCE_DIAGNOSE_OPERATION = "SOURCE_DIAGNOSE";
    public static final String MOCK_INGEST_OPERATION = "MOCK_INGEST";
    public static final String BATCH_IMPORT_OPERATION = "BATCH_IMPORT";

    private MarketDataIngestStableContract() {
    }
}
