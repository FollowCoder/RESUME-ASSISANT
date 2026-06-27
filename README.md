# 📄 Resume Assistant - AI 智能简历助手

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-blue.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-lightgrey.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**一款 AI 驱动的全栈简历助手 Web 应用，通过多智能体协同编排，提供从简历创建到面试准备的全流程求职辅助。**

[功能特性](#功能特性) • [快速开始](#快速开始) • [系统架构](#系统架构) • [API 文档](#api-接口文档) • [部署指南](#部署指南)

</div>

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [模块功能介绍](#模块功能介绍)
- [API 接口文档](#api-接口文档)
- [项目结构](#项目结构)
- [部署指南](#部署指南)
- [开发规范](#开发规范)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目简介

Resume Assistant 是一款面向求职者的 AI 驱动 Web 应用，基于 **AgentScope Java** 多智能体框架，集成通义千问大模型，为用户提供一站式求职准备服务。

### 目标用户

- **应届毕业生**：缺乏简历编写经验，需要从零开始创建专业简历
- **在职跳槽者**：需要优化现有简历、精准匹配目标岗位
- **技术岗求职者**：需要针对性的技术面试模拟训练

### 核心价值

- **一站式服务**：从简历生成到面试模拟的完整链路，数据自动流转
- **智能个性化**：基于用户画像（工作年限、技术栈、行业方向）提供精准建议
- **多智能体协同**：各模块独立 Agent 处理，通过数据流转实现协同增效

---

## 功能特性

### 🎯 四大核心模块

| 模块 | 功能描述 | 消耗次数 |
|------|----------|----------|
| **简历编写** | 对话式引导 / 表单填写，多模板选择，PDF/Word 导出 | 2 次 |
| **简历优化** | 上传/粘贴简历，多维度 AI 分析，逐条优化建议 | 1 次 |
| **JD 匹配度** | 简历与职位描述深度匹配分析，差距与改进建议 | 1 次 |
| **模拟面试** | 基于简历+JD 的技术面试模拟，支持文字/语音模式 | 3 次 |

### ✨ 特色能力

- **多智能体编排**：基于 AgentScope 的 ReAct 推理模式，每个模块拥有独立智能体
- **用户画像系统**：贯穿所有模块的个性化服务基础，支持多维度用户特征
- **实时语音面试**：集成通义语音 ASR/TTS，支持语音对话式面试
- **多种简历模板**：5 种风格模板，支持中/英双语简历生成
- **智能文件解析**：支持 PDF、Word 格式简历上传解析
- **计费系统**：新用户注册赠送 10 次免费额度，灵活的使用次数管理

---

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Vue3 + Vite 前端                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ 简历编写  │ │ 简历优化  │ │ JD匹配度 │ │ 模拟面试  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP / WebSocket
┌───────────────────────┼─────────────────────────────────┐
│              Spring Boot 4 后端                           │
│  ┌────────────────────┴────────────────────────┐        │
│  │            API Gateway / Controller          │        │
│  └────────────────────┬────────────────────────┘        │
│  ┌────────────────────┴────────────────────────┐        │
│  │           AgentScope 智能体编排层             │        │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌──────┐ │        │
│  │  │Writer  │ │Optimizer│ │Matcher │ │Inter-│ │        │
│  │  │Agent   │ │Agent   │ │Agent   │ │viewer│ │        │
│  │  └────────┘ └────────┘ └────────┘ └──────┘ │        │
│  └─────────────────────────────────────────────┘        │
│  ┌──────────────────┐  ┌───────────────────────┐        │
│  │  用户画像服务     │  │  文件解析/生成服务     │        │
│  └──────────────────┘  └───────────────────────┘        │
└───────────────────────┬─────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
   ┌────┴────┐    ┌─────┴─────┐   ┌────┴────┐
   │PostgreSQL│    │通义千问 API│   │通义语音  │
   └─────────┘    └───────────┘   └─────────┘
```

### 智能体编排设计

基于 AgentScope Java 的 HarnessAgent + 子 Agent 声明式编排模式：

```
Supervisor Agent (主调度)
├── ResumeWriterAgent     (简历编写智能体)
├── ResumeOptimizerAgent  (简历优化智能体)
├── JDMatcherAgent        (JD匹配度智能体)
└── InterviewerAgent      (模拟面试智能体)
```

### 数据流转关系

```
用户画像 ──────────────────────────────────────┐
    │                                           │
    ▼                                           ▼
简历编写 ───生成简历──→ 简历优化 ───优化后──→ JD匹配度
    │                      │                    │
    │                      │                    │
    └──────────────────────┴────────────────────┘
                           │
                           ▼
                       模拟面试
                           │
                           ▼
                       评估报告
```

---

## 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本，虚拟线程支持 |
| Spring Boot | 4.0.0 | 后端框架，单体架构 |
| AgentScope Java | 1.0.12 | 多智能体编排框架 |
| Spring AI Alibaba | 1.1.2 | 阿里云 AI 服务集成 |
| Spring Security | 6.x | 安全认证框架 |
| Spring Data JPA | 3.x | ORM 框架 |
| PostgreSQL | 16 | 关系型数据库 |
| JJWT | 0.12.6 | JWT 令牌处理 |
| Apache PDFBox | 3.0.3 | PDF 文件解析 |
| Apache POI | 5.3.0 | Word 文件解析/生成 |
| OpenPDF | 2.0.3 | PDF 文件生成 |
| Flying Saucer | 9.13.3 | HTML 转 PDF |
| Thymeleaf | 3.x | 模板引擎 |
| DashScope SDK | 2.22.4 | 通义千问/语音 API |
| Caffeine | 3.x | 本地缓存 |
| Lombok | - | 简化代码工具 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.5 | 前端框架 |
| TypeScript | 5.3 | 类型安全 |
| Vite | 6.0 | 构建工具 |
| Vue Router | 4.3 | 路由管理 |
| Pinia | 2.2 | 状态管理 |
| Element Plus | 2.8 | UI 组件库 |
| Axios | 1.7 | HTTP 客户端 |
| Marked | 13.0 | Markdown 渲染 |
| Sass | 1.77 | CSS 预处理器 |
| Vitest | 1.6 | 单元测试框架 |

---

## 快速开始

### 前置环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | Spring Boot 4 要求 |
| Node.js | 18+ | 前端构建 |
| npm | 9+ | 前端包管理 |
| PostgreSQL | 14+ | 数据库 |
| Maven | 3.9+ | 后端构建 |

### 1. 克隆项目

```bash
git clone https://github.com/your-username/resume-assistant.git
cd resume-assistant
```

### 2. 环境配置

复制环境变量模板并配置：

```bash
cp .env.tmp .env
```

编辑 `.env` 文件，配置以下必要变量：

```env
# 数据库配置
DB_USERNAME=postgres
DB_PASSWORD=your_password

# AI 服务配置（阿里云 DashScope）
DASHSCOPE_API_KEY=your_dashscope_api_key

# JWT 密钥（至少 256 位）
JWT_SECRET=your_jwt_secret_key_at_least_256_bits_long

# 应用配置
SPRING_PROFILES_ACTIVE=local
```

### 3. 初始化数据库

```sql
CREATE DATABASE resume_assistant;
```

数据库表结构将由 Flyway 自动创建：`backend/src/main/resources/db/migration/V1__init_schema.sql`

### 4. 启动后端服务

```bash
cd backend

# PowerShell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
./mvnw spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 5. 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

### 6. 访问应用

打开浏览器访问 `http://localhost:5173`，注册账号即可开始使用。

---

## 环境配置

### 后端配置

后端配置文件位于 `backend/src/main/resources/`：

- `application.yml` — 主配置文件
- `application-local.yml` — 本地开发配置
- `application-dev.yml` — 开发环境配置

核心配置项：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/resume_assistant
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

# AgentScope 配置
agentscope:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
  llm:
    provider: dashscope
    main-model: qwen-max        # 主模型（复杂推理）
    light-model: qwen-turbo     # 轻量模型（简单任务）
    multi-agent-model: qwen-plus # 多智能体模型

# JWT 配置
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24小时

# 语音服务配置
app:
  speech:
    asr-model: fun-asr-realtime
    tts-model: cosyvoice-v1
    tts-voice: longxiaochun
```

### 前端配置

前端配置文件位于 `frontend/`：

- `vite.config.ts` — Vite 构建配置
- `.env.development` — 开发环境变量

---

## 模块功能介绍

### 1. 用户画像系统

用户画像贯穿所有模块，是个性化服务的基础。

| 字段 | 类型 | 示例 |
|------|------|------|
| 工作年限 | 枚举 | 应届 / 1-3年 / 3-5年 / 5-10年 / 10年+ |
| 技术方向 | 标签组 | Java / Python / 前端 / 大数据 / AI |
| 目标岗位 | 文本 | 高级Java工程师 |
| 目标行业 | 标签组 | 互联网 / 金融 / 游戏 |
| 期望薪资 | 范围 | 20k-30k |
| 教育背景 | 结构化 | 本科/硕士 + 学校 + 专业 |
| 核心技能 | 标签组 | Spring Boot, 分布式, MySQL |

### 2. 简历编写模块

**交互模式**：

- **模式 A：对话式引导** — Agent 通过多轮对话引导用户回忆和描述经历
- **模式 B：表单填写** — 提供结构化表单，用户直接填入各模块信息

**输出能力**：

- 5 种简历模板风格选择
- PDF / Word（.docx）导出
- 中/英双语简历生成
- 生成后自动流转至"简历优化"模块

### 3. 简历优化模块

**输入方式**：

- 粘贴纯文本
- 上传文件解析（PDF / Word）
- 从"简历编写"模块直接流转

**优化维度**：

| 维度 | 说明 |
|------|------|
| 个人总结 | 精炼核心竞争力描述 |
| 工作经历 | STAR 法则优化，补充量化数据 |
| 项目经验 | 突出技术深度和业务价值 |
| 技能清单 | 技能关键词优化和分级 |
| 整体结构 | 排版逻辑和信息优先级调整 |

### 4. JD 匹配度模块

**分析输出**：

- **总体匹配分数**：0-100 分，配合等级标识（极佳/良好/一般/较差）
- **维度分析**：技能匹配、经验匹配、教育匹配、关键词覆盖
- **差距分析**：明确列出简历中缺失但 JD 要求的关键能力
- **改进建议**：针对每个差距给出具体的简历修改建议

### 5. 模拟面试模块

**面试流程**：

1. **面试准备**：Agent 分析简历 + JD，生成面试题目库
2. **开场介绍**：模拟面试官开场，邀请自我介绍
3. **技术提问**：基于简历项目经验和 JD 技术要求逐步深入
4. **追问环节**：根据回答质量动态追问
5. **反问环节**：模拟"你有什么想问的"
6. **面试结束**：生成评估报告

**评分维度**：

| 维度 | 说明 |
|------|------|
| 技术深度 | 对核心技术的理解深度 |
| 表达能力 | 回答的逻辑性和清晰度 |
| 项目理解 | 对自己项目的掌握程度 |
| 应变能力 | 面对追问和陌生问题的反应 |
| 综合评分 | 加权总分 + 面试通过概率预估 |

---

## API 接口文档

### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/login` | 用户登录 |
| `GET` | `/api/auth/me` | 获取当前用户信息 |

### 用户画像

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/profile` | 获取用户画像 |
| `PUT` | `/api/profile` | 更新用户画像 |

### 简历编写

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/resume/form` | 表单式提交生成 |
| `GET` | `/api/resume/templates` | 获取模板列表 |
| `POST` | `/api/resume/export` | 导出 PDF/Word |
| `GET` | `/api/resume/{id}` | 获取简历详情 |
| `GET` | `/api/resume/list` | 获取用户简历列表 |

### 简历优化

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/optimize/text` | 粘贴文本优化 |
| `POST` | `/api/optimize/file` | 上传文件优化 |
| `POST` | `/api/optimize/resume/{id}` | 优化已有简历 |
| `POST` | `/api/optimize/apply` | 应用优化建议 |

### JD 匹配度

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/match/analyze` | 匹配度分析 |
| `GET` | `/api/match/history` | 匹配历史记录 |
| `GET` | `/api/match/{id}` | 获取匹配详情 |

### 模拟面试

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/interview/start` | 开始面试 |
| `WS` | `/ws/interview/{id}` | 面试实时通信 |
| `POST` | `/api/interview/{id}/end` | 结束面试 |
| `GET` | `/api/interview/{id}/report` | 获取面试报告 |
| `GET` | `/api/interview/history` | 面试历史 |

### 次数管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/credits/balance` | 查询剩余次数 |
| `POST` | `/api/credits/purchase` | 购买次数 |
| `GET` | `/api/credits/transactions` | 使用记录 |

### 通用响应格式

所有 API 返回统一的 `ApiResponse` 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 项目结构

```
resume-assistant/
├── frontend/                          # Vue3 + Vite 前端
│   ├── src/
│   │   ├── views/                     # 页面组件
│   │   │   ├── ResumeWriter.vue       # 简历编写
│   │   │   ├── ResumeOptimizer.vue    # 简历优化
│   │   │   ├── JDMatcher.vue          # JD匹配度
│   │   │   ├── MockInterview.vue      # 模拟面试
│   │   │   ├── Profile.vue            # 用户画像
│   │   │   ├── Login.vue              # 登录
│   │   │   └── Register.vue           # 注册
│   │   ├── components/                # 通用组件
│   │   │   ├── ChatPanel.vue          # 聊天面板
│   │   │   ├── ResumeForm.vue         # 简历表单
│   │   │   ├── ResumePreview.vue      # 简历预览
│   │   │   ├── InterviewChat.vue      # 面试聊天
│   │   │   ├── VoiceRecorder.vue      # 语音录制
│   │   │   └── ...
│   │   ├── stores/                    # Pinia 状态管理
│   │   │   ├── auth.ts                # 认证状态
│   │   │   ├── resume.ts              # 简历状态
│   │   │   └── interview.ts           # 面试状态
│   │   ├── api/                       # API 请求层
│   │   ├── utils/                     # 工具函数
│   │   ├── layouts/                   # 布局组件
│   │   └── router/                    # 路由配置
│   ├── package.json
│   └── vite.config.ts
│
├── backend/                           # Spring Boot 4 后端
│   ├── src/main/java/com/resume/
│   │   ├── controller/                # API 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── ProfileController.java
│   │   │   ├── ResumeController.java
│   │   │   ├── OptimizeController.java
│   │   │   ├── MatchController.java
│   │   │   ├── InterviewController.java
│   │   │   └── CreditController.java
│   │   ├── service/                   # 业务服务层
│   │   ├── agent/                     # AgentScope 智能体
│   │   │   ├── writer/                # 简历编写 Agent
│   │   │   ├── optimizer/             # 简历优化 Agent
│   │   │   ├── matcher/               # JD 匹配 Agent
│   │   │   └── interviewer/           # 模拟面试 Agent
│   │   ├── model/                     # 数据模型
│   │   │   ├── dto/                   # 数据传输对象
│   │   │   └── entity/                # JPA 实体
│   │   ├── repository/                # 数据访问层
│   │   ├── config/                    # 配置类
│   │   ├── websocket/                 # WebSocket 处理
│   │   └── util/                      # 工具类
│   ├── src/main/resources/
│   │   ├── db/migration/              # Flyway 迁移脚本
│   │   ├── prompts/                   # Agent Prompt 模板
│   │   ├── templates/resume/          # 简历 HTML 模板
│   │   └── application.yml            # 应用配置
│   └── pom.xml
│
├── docker-compose.yml                 # Docker 编排配置
├── build.ps1                          # 构建脚本
├── SPEC.md                            # 产品规格说明书
└── README.md
```

---

## 部署指南

### Docker Compose 部署（推荐）

1. **配置环境变量**

```bash
cp .env.tmp .env
# 编辑 .env 文件，配置 DASHSCOPE_API_KEY、DB_PASSWORD、JWT_SECRET
```

2. **启动服务**

```bash
docker-compose up -d
```

3. **访问应用**

- 前端：http://localhost:80
- 后端 API：http://localhost:8080

### Docker Compose 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| `postgres` | 5432 | PostgreSQL 数据库 |
| `backend` | 8080 | Spring Boot 后端 |
| `frontend` | 80 | Nginx 静态文件服务 |

### 手动部署

#### 后端部署

```bash
cd backend

# 打包
./mvnw clean package -DskipTests

# 运行
java -jar target/resume-assistant-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://your-db-host:5432/resume_assistant \
  --app.jwt.secret=your-production-jwt-secret
```

#### 前端部署

```bash
cd frontend

# 构建
npm run build

# 部署 dist/ 目录到 Nginx 或其他静态文件服务器
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/frontend/dist;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 开发规范

### 后端开发规范

- **代码风格**：遵循 Google Java Style Guide
- **分层架构**：Controller → Service → Repository，禁止跨层调用
- **异常处理**：使用 `@RestControllerAdvice` 统一异常处理
- **日志规范**：使用 SLF4J + Logback，关键操作必须记录日志
- **API 设计**：RESTful 风格，统一使用 `ApiResponse` 包装响应

### 前端开发规范

- **组件命名**：PascalCase，如 `ResumeWriter.vue`
- **文件组织**：按功能模块分组，通用组件放 `components/`
- **状态管理**：使用 Pinia，按业务模块拆分 store
- **样式规范**：使用 SCSS，遵循 BEM 命名规范
- **TypeScript**：严格模式，禁止使用 `any`

### Git 提交规范

```
<type>(<scope>): <subject>

类型：
- feat: 新功能
- fix: 修复 Bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具相关

示例：
feat(resume): 添加简历 PDF 导出功能
fix(auth): 修复 JWT Token 过期未刷新问题
```

---

## 贡献指南

欢迎贡献代码、报告问题或提出改进建议！

### 如何贡献

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'feat: add some feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交 Pull Request

### 开发流程

1. 确保本地环境配置正确
2. 从 `main` 分支创建功能分支
3. 编写代码并添加测试
4. 确保所有测试通过：`mvn test`（后端）/ `npm test`（前端）
5. 提交 PR 并描述更改内容

### 问题反馈

- 使用 GitHub Issues 报告 Bug
- 提供详细的复现步骤和环境信息
- 包含错误日志和截图（如适用）

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！**

</div>
