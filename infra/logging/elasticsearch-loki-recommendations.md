# Elasticsearch / Loki — Centralized Log Aggregation

## Decision Matrix

| Criterion | Elasticsearch + Kibana | Loki + Grafana |
|---|---|---|
| Full-text search | Excellent (inverted index) | Limited (label + regex filter) |
| Storage cost | High (indexes every field) | Low (compresses raw logs, indexes labels only) |
| Query language | Lucene / KQL | LogQL |
| Cardinality tolerance | High | Low (high-cardinality labels = OOM) |
| Operational complexity | High (JVM, shards, replicas) | Low (Go binary, simple) |
| Best for | Debugging, forensic analysis, business analytics | Operational monitoring, cost-sensitive, high-volume |

**Recommendation for Suvidha:** Start with **Loki** for operational logs (lower cost, simpler ops). Add **Elasticsearch** later only if you need full-text search over business data.

---

## Option A: Loki Stack (Recommended)

### Architecture

```
suvidha-* services
  └─ stdout JSON logs
       └─ Promtail / Fluent Bit (sidecar or DaemonSet)
            └─ Loki (distributed or single-binary)
                 └─ Grafana (query + dashboards)
                      └─ Alertmanager (alerting)
```

### Promtail Config (`promtail-config.yml`)

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: suvidha-services
    static_configs:
      - targets:
          - localhost
        labels:
          job: suvidha
          __path__: /var/log/suvidha/*.log

    pipeline_stages:
      - json:
          expressions:
            timestamp: timestamp
            level: level
            service: service
            trace_id: trace_id
            correlation_id: correlation_id
            session_id: session_id
            user_id: user_id
            message: message
            http_method: http.method
            http_status: http.status_code
            http_duration_ms: http.duration_ms
            env: env

      - labels:
          level:
          service:
          env:
          http_status:
          http_method:

      - timestamp:
          source: timestamp
          format: RFC3339Nano

      - output:
          source: message
```

### Loki Config (`loki-config.yml`)

```yaml
auth_enabled: false

server:
  http_listen_port: 3100

common:
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2026-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

limits_config:
  max_query_series: 5000
  max_query_lookback: 720h
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  max_label_value_length: 2048
  max_label_names_per_series: 30
  ingestion_rate_mb: 16
  ingestion_burst_size_mb: 32

compactor:
  working_directory: /loki/compactor
  retention_enabled: true
  retention_delete_delay: 2h
  retention_delete_worker_count: 150

storage_config:
  tsdb_shipper:
    active_index_directory: /loki/tsdb-index
    cache_location: /loki/tsdb-cache
  boltdb_shipper:
    active_index_directory: /loki/boltdb-shipper-active
    cache_location: /loki/boltdb-shipper-cache
    shared_store: filesystem

table_manager:
  retention_deletes_enabled: true
  retention_period: 720h
```

### Key LogQL Queries

```logql
# All errors for a specific service
{service="suvidha-billing", level="ERROR"}

# Trace a single request across all services
{job="suvidha"} |~ "trace_id=\"4bf92f50b7d5c1a2\""

# Slow requests (>1s) across all services
{job="suvidha"} | json | http_duration_ms > 1000

# Error rate by service (last 5 min)
sum(rate({job="suvidha", level="ERROR"}[5m])) by (service)

# Requests per user session
{job="suvidha"} | json | session_id != "" | count by (session_id)

# Payment failures with correlation ID
{service="suvidha-billing", level="ERROR"}
  |~ "payment|transaction"
  | json
  | line_format "{{.correlation_id}} {{.message}}"

# 95th percentile latency by service
quantile_over_time(0.95,
  {job="suvidha"} | json | unwrap http_duration_ms [5m]
) by (service)
```

### Grafana Dashboard Panels

| Panel | Query Type | Purpose |
|---|---|---|
| Error rate over time | Time series | Detect spikes |
| Latency heatmap | Heatmap | Spot tail latency |
| Log volume by service | Bar gauge | Identify noisy services |
| Top error messages | Table + Logs | Quick triage |
| Trace view | Logs (filtered by trace_id) | Distributed trace |
| Active sessions | Stat | System usage |

---

## Option B: Elasticsearch + Kibana

### Filebeat Config (`filebeat.yml`)

```yaml
filebeat.inputs:
  - type: filestream
    id: suvidha-logs
    enabled: true
    paths:
      - /var/log/suvidha/*.log
    json.keys_under_root: true
    json.add_error_key: true
    json.message_key: message
    json.overwrite_keys: true

    processors:
      - add_host_metadata: ~
      - add_cloud_metadata: ~
      - decode_json_fields:
          fields: ["message"]
          target: ""
          overwrite_keys: true
      - drop_fields:
          fields: ["ecs", "agent", "host.os", "host.architecture"]
          ignore_missing: true

output.elasticsearch:
  hosts: ["http://elasticsearch:9200"]
  username: "elastic"
  password: "${ES_PASSWORD}"
  index: "suvidha-logs-%{+yyyy.MM.dd}"

setup.ilm.enabled: true
setup.ilm.policy_name: "suvidha-logs"
setup.ilm.policy_file: /usr/share/filebeat/ilm-policy.json
setup.template.name: "suvidha-logs"
setup.template.pattern: "suvidha-logs-*"
```

### ILM Policy (`ilm-policy.json`)

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_primary_shard_size": "50gb",
            "max_age": "1d"
          },
          "set_priority": { "priority": 100 }
        }
      },
      "warm": {
        "min_age": "2d",
        "actions": {
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 },
          "set_priority": { "priority": 50 }
        }
      },
      "cold": {
        "min_age": "7d",
        "actions": {
          "set_priority": { "priority": 0 },
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### Index Template Mapping

```json
{
  "index_patterns": ["suvidha-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 2,
      "number_of_replicas": 1,
      "index.lifecycle.name": "suvidha-logs",
      "index.lifecycle.rollover_alias": "suvidha-logs"
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "service": { "type": "keyword" },
        "message": { "type": "text", "analyzer": "standard" },
        "trace_id": { "type": "keyword" },
        "span_id": { "type": "keyword" },
        "correlation_id": { "type": "keyword" },
        "session_id": { "type": "keyword" },
        "user_id": { "type": "keyword" },
        "env": { "type": "keyword" },
        "instance_id": { "type": "keyword" },
        "http.method": { "type": "keyword" },
        "http.status_code": { "type": "integer" },
        "http.duration_ms": { "type": "integer" },
        "error.code": { "type": "keyword" },
        "business.entity_type": { "type": "keyword" },
        "business.entity_id": { "type": "keyword" },
        "business.action": { "type": "keyword" },
        "business.result": { "type": "keyword" }
      }
    }
  }
}
```

### Elasticsearch Sizing Guidelines

| Metric | Small (< 5 services) | Medium (5-15 services) | Large (15+ services) |
|---|---|---|---|
| Data nodes | 2 | 3 | 5+ |
| RAM per node | 16 GB | 32 GB | 64 GB |
| Heap per node | 8 GB (max 31 GB) | 16 GB | 30 GB |
| Shards per index | 1 | 2 | 3-5 |
| Daily log volume | ~5 GB | ~20 GB | ~100 GB+ |
| Retention | 7 days | 14 days | 30 days |

---

## Docker Compose (Loki Stack Quick Start)

```yaml
services:
  loki:
    image: grafana/loki:3.0.0
    ports:
      - "3100:3100"
    volumes:
      - ./infra/logging/loki-config.yml:/etc/loki/config.yml
      - loki-data:/loki
    command: -config.file=/etc/loki/config.yml

  promtail:
    image: grafana/promtail:3.0.0
    volumes:
      - ./infra/logging/promtail-config.yml:/etc/promtail/config.yml
      - /var/log/suvidha:/var/log/suvidha:ro
    command: -config.file=/etc/promtail/config.yml

  grafana:
    image: grafana/grafana:11.0.0
    ports:
      - "3000:3000"
    environment:
      GF_AUTH_ANONYMOUS_ENABLED: "true"
      GF_AUTH_ANONYMOUS_ORG_ROLE: "Admin"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./infra/logging/grafana-datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
      - ./infra/logging/grafana-dashboards.yml:/etc/grafana/provisioning/dashboards/dashboards.yml
      - ./infra/logging/dashboards:/var/lib/grafana/dashboards

volumes:
  loki-data:
  grafana-data:
```

---

## Best Practices for Aggregation

1. **Never index high-cardinality fields as labels in Loki** — `user_id`, `correlation_id`, `trace_id` should be extracted via `| json` at query time, not as static labels.

2. **Use ILM in Elasticsearch** — rollover daily, shrink in warm phase, delete after 30 days.

3. **Set `discardingThreshold: 0` in Logback async appenders** — dropping WARN/ERROR logs during queue overflow hides production issues.

4. **Compress archived logs** — `.gz` in file rolling policy reduces storage 80-90%.

5. **Separate audit logs** — send to a different index/stream with longer retention (90+ days).

6. **Add `env` label to every log** — enables filtering dev/staging noise from production queries.

7. **Use `instance_id` (pod name) as a label** — enables correlating logs with specific replicas during rolling deployments.
