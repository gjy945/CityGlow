-- CityGlow 数据库 schema
-- 设计文档第 4.2 节
-- 生产环境用 MySQL 8;本文件作为参考 SQL/手动初始化用
-- 注意:运行时 hibernate ddl-auto=update 会自动建表,本文件不会被自动执行

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 观星日志表
CREATE TABLE IF NOT EXISTS observation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    location_name VARCHAR(100) COMMENT '地点名称',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    longitude DECIMAL(10, 7) COMMENT '经度',
    image_url VARCHAR(255) COMMENT '星空照片路径',
    bortle_level INT COMMENT '当时观测到的暗夜等级',
    description VARCHAR(500) COMMENT '观测描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_observation_logs_location (latitude, longitude),
    INDEX idx_observation_logs_created_at (created_at)
);

-- 天文事件表
CREATE TABLE IF NOT EXISTS astro_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '事件标题',
    event_time TIMESTAMP NOT NULL COMMENT '发生时间',
    description TEXT COMMENT '事件描述',
    event_type VARCHAR(20) COMMENT '类型: METEOR, ECLIPSE, PLANET',
    INDEX idx_astro_events_event_time (event_time)
);
