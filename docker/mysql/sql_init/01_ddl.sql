CREATE DATABASE IF NOT EXISTS sample_schema CHARACTER SET
utf8 COLLATE utf8_general_ci;

USE sample_schema;

DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS login_user;
DROP TABLE IF EXISTS roles;

-- ロール
CREATE TABLE roles(
    id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ロールのID',
    name VARCHAR(32) NOT NULL COMMENT 'ロールの名前',
    created_by    character varying(255) NOT NULL COMMENT '作成者',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    updated_by    character varying(255) NOT NULL COMMENT '更新者',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時'
);

-- ユーザー
CREATE TABLE login_user(
    id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ユーザーのID',
    name VARCHAR(128) NOT NULL COMMENT 'ユーザーの表示名',
    email VARCHAR(256) NOT NULL COMMENT 'メールアドレス（ログイン時に利用）',
    password VARCHAR(128) NOT NULL COMMENT 'ハッシュ化済みのパスワード',
    gender   VARCHAR(32) NOT NULL COMMENT '性別',
    genre    VARCHAR(256) COMMENT '興味のあるジャンル',
    created_by    character varying(255) NOT NULL COMMENT '作成者',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    updated_by    character varying(255) NOT NULL COMMENT '更新者',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
	UNIQUE KEY `idx_email` (`email`)
);

-- ユーザーとロールの対応付け
CREATE TABLE user_role(
    user_id INTEGER COMMENT 'ユーザーのID',
    role_id INTEGER COMMENT 'ロールのID',
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES login_user(id),
    CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES roles(id)
);
