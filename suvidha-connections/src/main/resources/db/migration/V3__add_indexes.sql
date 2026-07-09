CREATE INDEX idx_conn_req_citizen ON connection_request(citizen_id);
CREATE INDEX idx_conn_req_display ON connection_request(display_id);
CREATE INDEX idx_conn_timeline_conn ON connection_timeline(connection_id);
CREATE INDEX idx_conn_docs_conn ON connection_documents(connection_id);
CREATE INDEX idx_conn_history_conn ON connection_status_history(connection_id);
