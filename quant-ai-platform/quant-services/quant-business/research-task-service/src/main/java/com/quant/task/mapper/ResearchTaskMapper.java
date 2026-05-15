package com.quant.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.task.domain.entity.ResearchTaskDO;
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
        """)
    int updateTaskStage(@Param("taskId") String taskId,
                        @Param("status") String status,
                        @Param("currentStage") String currentStage);

    @Update("""
        update research_task
        set status = #{status},
            result_ref = #{resultRef},
            finish_time = now(),
            updated_at = now()
        where task_id = #{taskId}
        """)
    int updateTaskResult(@Param("taskId") String taskId,
                         @Param("status") String status,
                         @Param("resultRef") String resultRef);
}