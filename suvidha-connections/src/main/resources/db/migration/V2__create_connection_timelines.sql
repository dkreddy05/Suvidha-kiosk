CREATE TABLE connection_timeline (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE connection_documents (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    base64 TEXT NOT NULL
);

CREATE TABLE connection_status_history (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    comment VARCHAR(512),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
