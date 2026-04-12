CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NOT NULL,
    avatar_url  TEXT,
    created_at  BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE user_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(256) NOT NULL,
    expired_at  BIGINT       NOT NULL,
    PRIMARY KEY (id)
);
