CREATE TABLE IF NOT EXISTS WORKER_LOCK (
    id varchar(255),
    lock_client varchar(255),
    PRIMARY KEY (id)
)#