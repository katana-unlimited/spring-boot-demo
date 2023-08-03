INSERT INTO sample_schema.roles(id, name, created_by, updated_by) VALUES(1, 'ROLE_GENERAL', 'SYSTEM', 'SYSTEM');
INSERT INTO sample_schema.roles(id, name, created_by, updated_by) VALUES(2, 'ROLE_ADMIN', 'SYSTEM', 'SYSTEM');

-- password = "general"
INSERT INTO sample_schema.login_user(id, name, email, password, gender, genre, created_by, updated_by) VALUES(1, '一般太郎', 'general@example.com', '$2a$10$6fPXYK.C9rCWUBifuqBIB.GRNU.nQtBpdzkkKis8ETaKVKxNo/ltO', 'FEMALE', null, 'SYSTEM', 'SYSTEM');
-- password = "admin"
INSERT INTO sample_schema.login_user(id, name, email, password, gender, genre, created_by, updated_by) VALUES(2, '管理太郎', 'admin@example.com', '$2a$10$SJTWvNl16fCU7DaXtWC0DeN/A8IOakpCkWWNZ/FKRV2CHvWElQwMS', 'MALE', null, 'SYSTEM', 'SYSTEM');
INSERT INTO sample_schema.login_user(id, name, email, password, gender, genre, created_by, updated_by) VALUES(3,'山田花子','hanako@gmail.com','$2a$10$D8G11kTDDpmsYwgW7sXBhut6vwIL.zdiQf4UQwYymh8w54Ix/wsXu','FEMALE','LIFE','SYSTEM','SYSTEM');
INSERT INTO sample_schema.login_user(id, name, email, password, gender, genre, created_by, updated_by) VALUES(4,'一般太郎2','taro@yahoo.co.jp','$2a$10$jWeBYHQ/K9YYdRcb9xNNmeNxuBb3c2BGytdrIPu8vdCNmHl8GJoBm','MALE','SPORTS|MUSIC','SYSTEM','SYSTEM');
INSERT INTO sample_schema.login_user(id, name, email, password, gender, genre, created_by, updated_by) VALUES(5,'メッシ','messi@maiami.com','$2a$10$nVXQEuj6arBbkhhi5NcpJei9Xk4EBUirlmXA4LUkI4njhUKceU0tq','MALE','FINANCE|SPORTS','SYSTEM','SYSTEM');

INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(1, 1);
INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(2, 1);
INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(2, 2);
INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(3, 1);
INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(4, 1);
INSERT INTO sample_schema.user_role(user_id, role_id) VALUES(5, 1);
