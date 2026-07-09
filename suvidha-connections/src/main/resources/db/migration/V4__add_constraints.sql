ALTER TABLE connection_timeline
    ADD CONSTRAINT fk_timeline_request FOREIGN KEY (connection_id) REFERENCES connection_request(id) ON DELETE CASCADE;

ALTER TABLE connection_documents
    ADD CONSTRAINT fk_documents_request FOREIGN KEY (connection_id) REFERENCES connection_request(id) ON DELETE CASCADE;

ALTER TABLE connection_status_history
    ADD CONSTRAINT fk_history_request FOREIGN KEY (connection_id) REFERENCES connection_request(id) ON DELETE CASCADE;
