USE quant_ai;

ALTER TABLE task_message_log
    ADD UNIQUE KEY uk_message_consumer (topic_name, message_id, consumer_service);
