package com.quant.task.service;

import com.quant.task.domain.entity.TaskOutboxMessageDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskOutboxPublisherService {
        public void publishPending();

        public int publishPendingOnce(int limit);
}
