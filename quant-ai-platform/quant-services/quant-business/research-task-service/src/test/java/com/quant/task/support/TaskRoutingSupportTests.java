package com.quant.task.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskRoutingSupportTests {

    @Test
    void shouldResolveTaskTypeFromAnalysisScope() {
        assertEquals(
                TaskRoutingSupport.TASK_TYPE_RISK_REVIEW,
                TaskRoutingSupport.resolveTaskType(null, TaskRoutingSupport.ANALYSIS_SCOPE_RISK_RECHECK)
        );
        assertEquals(
                TaskRoutingSupport.TASK_TYPE_FOLLOW_UP_RESEARCH,
                TaskRoutingSupport.resolveTaskType(null, TaskRoutingSupport.ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP)
        );
    }

    @Test
    void shouldResolveAnalysisScopeFromTaskType() {
        assertEquals(
                TaskRoutingSupport.ANALYSIS_SCOPE_AUDIT_RECHECK,
                TaskRoutingSupport.resolveAnalysisScope(TaskRoutingSupport.TASK_TYPE_AUDIT_REVIEW, null)
        );
        assertEquals(
                TaskRoutingSupport.ANALYSIS_SCOPE_DEEP_RESEARCH,
                TaskRoutingSupport.resolveAnalysisScope(TaskRoutingSupport.TASK_TYPE_STOCK_RESEARCH, null)
        );
    }
}
