#!/usr/bin/env sh

create_topic() {
  kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic "$1" --partitions 3 --replication-factor 1
}

create_topic market.news.raw
create_topic market.announcement.raw
create_topic market.financial-report.raw
create_topic market.policy.raw
create_topic market.event.standardized

create_topic ai.task.dispatch
create_topic ai.task.status
create_topic ai.task.result
create_topic ai.task.audit
create_topic ai.task.deadletter

create_topic risk.warning.generated
create_topic strategy.signal.generated
create_topic report.generated
create_topic notification.dispatch

create_topic market.event.deadletter
create_topic business.event.deadletter
