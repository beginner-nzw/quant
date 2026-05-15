package com.quant.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.task.domain.entity.TaskMessageLogDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskMessageLogMapper extends BaseMapper<TaskMessageLogDO> {

    @Insert("""
            insert into task_message_log (
                message_log_id,
                message_id,
                task_id,
                event_id,
                topic_name,
                message_type,
                producer_service,
                consumer_service,
                consume_status,
                retry_count,
                error_message,
                raw_message_ref,
                message_timestamp,
                trace_id,
                tenant_id,
                deleted
            )
            values (
                #{entity.messageLogId},
                #{entity.messageId},
                #{entity.taskId},
                #{entity.eventId},
                #{entity.topicName},
                #{entity.messageType},
                #{entity.producerService},
                #{entity.consumerService},
                #{entity.consumeStatus},
                #{entity.retryCount},
                #{entity.errorMessage},
                #{entity.rawMessageRef},
                #{entity.messageTimestamp},
                #{entity.traceId},
                #{entity.tenantId},
                #{entity.deleted}
            )
            on duplicate key update
                consume_status = values(consume_status),
                retry_count = values(retry_count),
                error_message = values(error_message),
                raw_message_ref = values(raw_message_ref),
                message_timestamp = values(message_timestamp),
                trace_id = values(trace_id),
                tenant_id = values(tenant_id),
                deleted = 0,
                updated_at = now()
            """)
    int upsertOutboxLog(@Param("entity") TaskMessageLogDO entity);
}
