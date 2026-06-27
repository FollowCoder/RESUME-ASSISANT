-- ============================================================
-- Resume Assistant - Database Schema V1
-- PostgreSQL 初始化脚本
-- ============================================================

-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    remaining_credits INTEGER DEFAULT 10,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 用户画像表（TEXT[] 统一改为 JSONB 便于 Hibernate 映射）
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    work_years VARCHAR(20),
    tech_direction JSONB,
    target_position VARCHAR(100),
    target_industry JSONB,
    salary_range VARCHAR(50),
    education JSONB,
    core_skills JSONB,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 简历表
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(200),
    content JSONB,
    language VARCHAR(10) DEFAULT 'zh',
    template_id VARCHAR(50),
    file_path VARCHAR(500),
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- JD 匹配记录表
CREATE TABLE jd_matches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    resume_id BIGINT REFERENCES resumes(id),
    jd_content TEXT,
    match_score INTEGER,
    analysis JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 面试记录表
CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    resume_id BIGINT REFERENCES resumes(id),
    jd_content TEXT,
    mode VARCHAR(10),
    status VARCHAR(20),
    conversation JSONB,
    report JSONB,
    total_score INTEGER,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- 使用次数记录表
CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    module VARCHAR(30),
    credits_used INTEGER DEFAULT 1,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 索引
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_created_at ON resumes(created_at);
CREATE INDEX idx_jd_matches_user_id ON jd_matches(user_id);
CREATE INDEX idx_jd_matches_resume_id ON jd_matches(resume_id);
CREATE INDEX idx_interviews_user_id ON interviews(user_id);
CREATE INDEX idx_interviews_status ON interviews(status);
CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_credit_transactions_created_at ON credit_transactions(created_at);
