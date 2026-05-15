package com.quant.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskOutboxMessageMapper extends BaseMapper<TaskOutboxMessageDO> {

    @Select("""
            select *
            from task_outbox_message
            where deleted = 0
              and (
                    status = 'PENDING'
                    or (
                        status = 'FAILED'
                        and retry_count < max_retry_count
                        and (next_retry_at is null or next_retry_at <= now())
                    )
                    or (
                        status = 'SENDING'
                        and updated_at <= #{staleBefore}
                    )
              )
            order by created_at asc
            limit #{limit}
            """)
    List<TaskOutboxMessageDO> selectReadyToPublish(@Param("limit") int limit,
                                                   @Param("staleBefore") LocalDateTime staleBefore);

    @Update("""
            update task_outbox_message
            set status = 'SENDING',
                updated_at = now()
            where outbox_id = #{outboxId}
              and deleted = 0
              and status in ('PENDING', 'FAILED', 'SENDING')
            """)
    int markSending(@Param("outboxId") String outboxId);

    @Update("""
            update task_outbox_message
            set status = 'SENT',
                sent_at = now(),
                last_error = null,
                updated_at = now()
            where outbox_id = #{outboxId}
              and deleted = 0
              and status = 'SENDING'
            """)
    int markSent(@Param("outboxId") String outboxId);

    @Update("""
            update task_outbox_message
            set status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_at = #{nextRetryAt},
                last_error = #{lastError},
                updated_at = now()
            where outbox_id = #{outboxId}
              and deleted = 0
              and status = 'SENDING'
            """)
    int markFailed(@Param("outboxId") String outboxId,
                   @Param("nextRetryAt") LocalDateTime nextRetryAt,
                   @Param("lastError") String lastError);
}
