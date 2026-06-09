package com.quant.aiorchestrator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMessageLogMapper extends BaseMapper<TaskMessageLogDO> {

    @Select("""
            select *
            from task_message_log
            where topic_name = #{topicName}
              and message_id = #{messageId}
              and consumer_service = #{consumerService}
              and deleted = 0
            order by case consume_status
                       when 'SUCCESS' then 1
                       when 'CONSUMED' then 1
                       when 'SKIPPED' then 1
                       when 'PROCESSING' then 2
                       when 'FAILED' then 3
                       else 4
                     end,
                     updated_at desc,
                     id desc
            limit 1
            """)
    TaskMessageLogDO selectConsumerLog(@Param("topicName") String topicName,
                                       @Param("messageId") String messageId,
                                       @Param("consumerService") String consumerService);

    @Update("""
            update task_message_log
            set consume_status = 'PROCESSING',
                retry_count = #{retryCount},
                error_message = null,
                message_timestamp = #{messageTimestamp},
                trace_id = #{traceId},
                tenant_id = #{tenantId},
                updated_at = current_timestamp
            where id = #{id}
              and deleted = 0
              and consume_status = 'FAILED'
            """)
    int resetFailedToProcessing(@Param("id") Long id,
                                @Param("retryCount") Integer retryCount,
                                @Param("messageTimestamp") Long messageTimestamp,
                                @Param("traceId") String traceId,
                                @Param("tenantId") String tenantId);

    @Update("""
            update task_message_log
            set consume_status = #{consumeStatus},
                error_message = #{errorMessage},
                updated_at = current_timestamp
            where topic_name = #{topicName}
              and message_id = #{messageId}
              and consumer_service = #{consumerService}
              and deleted = 0
            """)
    int completeConsumerLog(@Param("topicName") String topicName,
                            @Param("messageId") String messageId,
                            @Param("consumerService") String consumerService,
                            @Param("consumeStatus") String consumeStatus,
                            @Param("errorMessage") String errorMessage);
}
