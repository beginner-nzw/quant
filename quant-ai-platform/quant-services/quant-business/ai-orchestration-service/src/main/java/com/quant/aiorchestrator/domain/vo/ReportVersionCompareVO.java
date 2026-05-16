package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReportVersionCompareVO {
    private String taskId;
    private String reportId;
    private Integer fromVersionNo;
    private Integer toVersionNo;
    private Boolean sameVersion;
    private Boolean changed;
    private VersionSummary fromVersion;
    private VersionSummary toVersion;
    private List<FieldChange> reportFieldsChanged;
    private List<ItemChange> sectionsAdded;
    private List<ItemChange> sectionsRemoved;
    private List<FieldChange> sectionsChanged;
    private List<ItemChange> evidenceRefsAdded;
    private List<ItemChange> evidenceRefsRemoved;
    private List<FieldChange> evidenceRefsChanged;
    private List<FieldChange> reviewFieldsChanged;

    @Data
    public static class VersionSummary {
        private String versionId;
        private Integer versionNo;
        private String snapshotSource;
        private String createdAt;
    }

    @Data
    public static class FieldChange {
        private String path;
        private String field;
        private Object fromValue;
        private Object toValue;
    }

    @Data
    public static class ItemChange {
        private String key;
        private Object value;
    }
}
