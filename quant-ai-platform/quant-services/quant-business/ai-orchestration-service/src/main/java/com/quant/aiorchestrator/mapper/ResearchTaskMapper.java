package com.quant.aiorchestrator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ResearchTaskMapper extends BaseMapper<ResearchTaskDO> {

    @Update("""
        update research_task
        set status = #{status},
            current_stage = #{currentStage},
            updated_at = now()
        where task_id = #{taskId}
          and status in ('DISPATCHED', 'RUNNING')
        """)
    int updateTaskStage(@Param("taskId") String taskId,
                        @Param("status") String status,
                        @Param("currentStage") String currentStage);

    @Update("""
        update research_task
        set status = #{status},
            current_stage = #{currentStage},
            result_ref = #{resultRef},
            error_message = null,
            finish_time = now(),
            updated_at = now()
        where task_id = #{taskId}
        """)
    int updateTaskResult(@Param("taskId") String taskId,
                         @Param("status") String status,
                         @Param("currentStage") String currentStage,
                         @Param("resultRef") String resultRef);

    @Update("""
        update research_task
        set retry_count = #{retryCount},
            status = #{status},
            current_stage = #{currentStage},
            result_ref = null,
            error_message = null,
            finish_time = null,
            updated_at = now()
        where task_id = #{taskId}
        """)
    int updateTaskRetryDispatched(@Param("taskId") String taskId,
                                  @Param("retryCount") Integer retryCount,
                                  @Param("status") String status,
                                  @Param("currentStage") String currentStage);


    @Update("""
    update research_task
    set status = 'FAILED',
        current_stage = #{currentStage},
        error_message = #{errorMessage},
        finish_time = now(),
        updated_at = now()
    where task_id = #{taskId}
    """)
    int updateTaskFailed(@Param("taskId") String taskId,
                         @Param("currentStage") String currentStage,
                         @Param("errorMessage") String errorMessage);

    @Update("""
    update research_task
    set status = 'CANCELLED',
        current_stage = 'CANCELLED',
        error_message = #{cancelReason},
        finish_time = now(),
        updated_at = now()
    where task_id = #{taskId}
    """)
    int updateTaskCancelled(@Param("taskId") String taskId,
                            @Param("cancelReason") String cancelReason);
}
