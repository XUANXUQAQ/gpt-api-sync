# GPT-Load 项目架构设计文档

## 项目概述

GPT-Load 是一个高性能、企业级的 AI 接口透明代理服务，专门为需要集成多种 AI 服务的企业和开发者设计。项目采用现代化的微服务架构，支持多种 AI 服务格式（OpenAI、Google Gemini、Anthropic Claude），具备智能密钥管理、负载均衡和完善的监控功能。

### 技术栈

**后端技术栈：**
- **主框架**: Go 1.23+
- **Web框架**: Gin (HTTP路由和中间件)
- **依赖注入**: Uber Dig (容器化依赖管理)
- **数据库**: 支持 MySQL、PostgreSQL、SQLite
- **ORM**: GORM v2 (数据库操作)
- **缓存/消息**: Redis (可选，用于分布式缓存和消息)
- **日志**: Logrus (结构化日志)
- **配置**: godotenv (环境变量管理)

**前端技术栈：**
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI库**: Naive UI
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **工具库**: @vueuse/core

## 整体架构设计

### 1. 分层架构

```
┌─────────────────────────────────────────────┐
│                Web UI (Vue3)                │
│              管理界面前端                    │
└─────────────────┬───────────────────────────┘
                  │ HTTP API
┌─────────────────▼───────────────────────────┐
│              API Gateway                    │
│           路由、中间件、认证                │
├─────────────────────────────────────────────┤
│              Handler Layer                  │
│         业务逻辑处理和请求响应              │
├─────────────────────────────────────────────┤
│             Service Layer                   │
│     核心业务服务 (Group/Key/Log管理)        │
├─────────────────────────────────────────────┤
│            Repository Layer                 │
│         数据访问层 (Database/Store)         │
├─────────────────────────────────────────────┤
│           Infrastructure Layer              │
│    基础设施 (Config/Logger/HttpClient)      │
└─────────────────────────────────────────────┘
```

### 2. 模块架构

```
gpt-load/
├── main.go                    # 应用程序入口
├── internal/                  # 内部代码模块
│   ├── app/                   # 应用程序核心
│   ├── container/             # 依赖注入容器
│   ├── types/                 # 类型定义和接口
│   ├── config/                # 配置管理
│   ├── db/                    # 数据库连接和迁移
│   ├── models/                # 数据模型
│   ├── handler/               # HTTP 处理器
│   ├── router/                # 路由定义
│   ├── middleware/            # 中间件
│   ├── services/              # 业务服务层
│   ├── proxy/                 # 代理服务核心
│   ├── channel/               # AI服务通道适配器
│   ├── keypool/               # 密钥池管理
│   ├── store/                 # 存储抽象层
│   ├── syncer/                # 缓存同步器
│   ├── httpclient/            # HTTP客户端管理
│   ├── errors/                # 错误处理
│   ├── response/              # 响应封装
│   └── utils/                 # 工具函数
└── web/                       # 前端代码
    ├── src/
    │   ├── api/               # API调用层
    │   ├── components/        # Vue组件
    │   ├── router/            # 前端路由
    │   ├── services/          # 前端服务
    │   ├── types/             # 类型定义
    │   ├── utils/             # 工具函数
    │   └── views/             # 页面视图
    └── dist/                  # 构建输出
```

## 核心模块详解

### 1. 应用启动流程 (`main.go`, `internal/app`)

**启动序列：**
1. **容器构建**: 使用 `container.BuildContainer()` 创建依赖注入容器
2. **UI资源注入**: 将嵌入的前端资源注入容器
3. **日志初始化**: 通过 `utils.SetupLogger()` 配置全局日志
4. **应用启动**: 调用 `app.Start()` 启动服务

**主从节点架构：**
- **Master节点**: 执行数据库迁移、系统设置初始化、密钥加载、后台服务启动
- **Slave节点**: 仅启动基础服务，不执行数据库操作和后台任务

### 2. 依赖注入容器 (`internal/container`)

使用 Uber Dig 实现依赖注入，按层次注册组件：

```go
// 基础设施服务
container.Provide(config.NewManager)
container.Provide(db.NewDB)
container.Provide(store.NewStore)

// 业务服务
container.Provide(services.NewGroupManager)
container.Provide(services.NewKeyService)
container.Provide(keypool.NewProvider)

// 处理器和路由
container.Provide(handler.NewServer)
container.Provide(proxy.NewProxyServer)
container.Provide(router.NewRouter)
```

### 3. 配置管理系统 (`internal/config`)

**双层配置架构：**

**静态配置** (环境变量，启动时加载):
- 服务器配置 (端口、主机、超时)
- 认证配置 (管理密钥)
- 数据库连接配置
- CORS 和性能配置

**动态配置** (热重载，存储在数据库):
- 系统设置 (请求超时、连接池等)
- 分组配置 (可覆盖系统设置)
- 代理密钥配置

**配置优先级**: 分组配置 > 系统设置 > 默认值

### 4. 数据库层 (`internal/db`, `internal/models`)

**支持的数据库：**
- SQLite (单机部署)
- MySQL (生产环境推荐)
- PostgreSQL (企业级部署)

**核心数据模型：**

```go
// 系统设置
type SystemSetting struct {
    SettingKey   string
    SettingValue string
    Description  string
}

// 分组管理
type Group struct {
    Name               string
    Upstreams          datatypes.JSON
    ChannelType        string
    TestModel          string
    Config             datatypes.JSONMap
    APIKeys            []APIKey
    EffectiveConfig    types.SystemSettings
}

// API密钥
type APIKey struct {
    KeyValue     string
    GroupID      uint
    Status       string  // active/invalid
    RequestCount int64
    FailureCount int64
}

// 请求日志
type RequestLog struct {
    GroupName    string
    KeyValue     string
    Model        string
    IsSuccess    bool
    StatusCode   int
    Duration     int64
    ErrorMessage string
}
```

### 5. 路由和中间件系统 (`internal/router`, `internal/middleware`)

**路由架构：**
```
/health                     # 健康检查
/api/auth/login            # 认证登录
/api/groups/*              # 分组管理 (需认证)
/api/keys/*                # 密钥管理 (需认证)
/api/dashboard/*           # 仪表板数据 (需认证)
/api/logs/*                # 日志查询 (需认证)
/api/settings/*            # 系统设置 (需认证)
/proxy/:group_name/*       # 代理转发 (代理认证)
/*                         # 前端静态资源
```

**中间件栈：**
1. **Recovery**: 恢复 panic 异常
2. **ErrorHandler**: 统一错误处理
3. **Logger**: 请求日志记录
4. **CORS**: 跨域资源共享
5. **RateLimiter**: 请求速率限制
6. **Auth**: 管理端认证
7. **ProxyAuth**: 代理端认证

### 6. 代理服务核心 (`internal/proxy`)

**代理流程：**

1. **请求解析**: 从URL路径提取分组名称
2. **分组查找**: 通过 `GroupManager` 获取分组配置
3. **通道创建**: 根据 `ChannelType` 创建对应的AI服务适配器
4. **参数覆盖**: 应用分组级别的参数覆盖
5. **密钥选择**: 从密钥池中轮换选择可用密钥
6. **请求转发**: 构建上游请求并转发
7. **重试机制**: 失败时自动重试（可配置）
8. **响应处理**: 流式或普通响应处理
9. **日志记录**: 记录请求统计信息

**重试和故障处理：**
- 支持配置重试次数和黑名单阈值
- 密钥失败达到阈值自动禁用
- 智能密钥恢复机制
- 详细的错误解析和日志记录

### 7. 通道适配器系统 (`internal/channel`)

**支持的AI服务类型：**

**OpenAI Channel:**
- 认证方式: `Authorization: Bearer <key>`
- 流式检测: 检查 `stream` 参数和 `Accept` 头
- 验证端点: `/v1/chat/completions`

**Gemini Channel:**
- 认证方式: URL参数 `?key=<key>`
- 特殊处理: Google API格式适配

**Anthropic Channel:**
- 认证方式: `x-api-key: <key>`
- 版本头: `anthropic-version`

**通道工厂模式：**
```go
type Factory struct {
    channelCache map[uint]ChannelProxy
}

func (f *Factory) GetChannel(group *models.Group) (ChannelProxy, error) {
    // 缓存检查和配置更新检测
    // 通道构造和配置应用
}
```

### 8. 密钥池管理 (`internal/keypool`)

**密钥池架构：**
- **选择策略**: 轮换算法 (`Rotate`)
- **状态管理**: 成功/失败计数和自动禁用
- **批量操作**: 支持批量添加、删除、恢复密钥
- **缓存同步**: 数据库和缓存双写一致性

**关键特性：**
- 原子性密钥选择
- 异步状态更新
- 数据库事务重试机制
- 智能故障恢复

### 9. 服务层 (`internal/services`)

**核心业务服务：**

**GroupManager:**
- 分组的热重载缓存管理
- 有效配置计算 (系统设置 + 分组覆盖)
- 代理密钥映射构建

**KeyService:**
- 密钥的CRUD操作
- 批量导入和验证
- 密钥格式验证和解析

**RequestLogService:**
- 异步日志写入 (可配置同步/异步模式)
- 批量数据库写入优化
- 统计数据更新

### 10. 存储抽象层 (`internal/store`)

**存储接口设计：**
```go
type Store interface {
    // 基础KV操作
    Set(key string, value []byte, ttl time.Duration) error
    Get(key string) ([]byte, error)

    // Hash操作 (密钥详情存储)
    HSet(key string, values map[string]any) error
    HGetAll(key string) (map[string]string, error)

    // List操作 (密钥轮换)
    LPush(key string, values ...any) error
    Rotate(key string) (string, error)

    // Set操作 (日志键管理)
    SAdd(key string, members ...any) error
    SPopN(key string, count int64) ([]string, error)

    // 发布订阅 (缓存同步)
    Publish(channel string, message []byte) error
    Subscribe(channel string) (Subscription, error)
}
```

**实现：**
- **Memory Store**: 单机内存存储
- **Redis Store**: 分布式Redis存储，支持发布订阅和管道操作

## API文档

### 管理端API

**基础路径**: `/api`
**认证方式**: Bearer Token (管理密钥)

#### 1. 认证接口

**登录**
```http
POST /api/auth/login
Content-Type: application/json

{
  "auth_key": "sk-123456"
}

Response:
{
  "success": true,
  "message": "Authentication successful"
}
```

#### 2. 分组管理

**创建分组**
```http
POST /api/groups
Authorization: Bearer <auth_key>

{
  "name": "openai",
  "display_name": "OpenAI Group",
  "description": "OpenAI服务分组",
  "channel_type": "openai",
  "test_model": "gpt-4o-mini",
  "upstreams": [
    {"url": "https://api.openai.com", "weight": 1}
  ],
  "proxy_keys": "proxy-key-1,proxy-key-2"
}
```

**获取分组列表**
```http
GET /api/groups
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "openai",
      "display_name": "OpenAI Group",
      "channel_type": "openai",
      "endpoint": "http://localhost:3001/proxy/openai",
      "upstreams": [...],
      "api_keys": [...]
    }
  ]
}
```

**更新分组**
```http
PUT /api/groups/:id
Authorization: Bearer <auth_key>

{
  "display_name": "Updated OpenAI Group",
  "config": {
    "request_timeout": 300,
    "max_retries": 5
  }
}
```

**删除分组**
```http
DELETE /api/groups/:id
Authorization: Bearer <auth_key>
```

#### 3. 密钥管理

**批量添加密钥**
```http
POST /api/keys/add-multiple
Authorization: Bearer <auth_key>

{
  "group_id": 1,
  "keys_text": "sk-key1\nsk-key2\nsk-key3"
}

Response:
{
  "success": true,
  "data": {
    "added_count": 2,
    "ignored_count": 1,
    "total_in_group": 10
  }
}
```

**异步批量添加密钥**
```http
POST /api/keys/add-async
Authorization: Bearer <auth_key>

{
  "group_id": 1,
  "keys_text": "sk-key1,sk-key2,sk-key3"
}

Response:
{
  "success": true,
  "data": {
    "task_id": "uuid",
    "status": "processing",
    "progress": 0
  }
}
```

**查询密钥列表**
```http
GET /api/keys?group_id=1&status=active&page=1&limit=20
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 100,
      "pages": 5
    }
  }
}
```

**批量删除密钥**
```http
POST /api/keys/delete-multiple
Authorization: Bearer <auth_key>

{
  "group_id": 1,
  "keys_text": "sk-key1\nsk-key2"
}
```

**验证分组密钥**
```http
POST /api/keys/validate-group
Authorization: Bearer <auth_key>

{
  "group_id": 1,
  "status": "invalid"  // 可选，验证特定状态的密钥
}
```

#### 4. 仪表板数据

**获取统计数据**
```http
GET /api/dashboard/stats
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": {
    "key_count": {
      "value": 150,
      "sub_value": 5,
      "sub_value_tip": "无效密钥数量"
    },
    "rpm": {
      "value": 25.5,
      "trend": 12.5,
      "trend_is_growth": true
    },
    "request_count": {
      "value": 1200,
      "trend": -5.2,
      "trend_is_growth": false
    },
    "error_rate": {
      "value": 2.1,
      "trend": -0.5,
      "trend_is_growth": true
    }
  }
}
```

**获取图表数据**
```http
GET /api/dashboard/chart?groupId=1
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": {
    "labels": ["2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ...],
    "datasets": [
      {
        "label": "成功请求",
        "data": [120, 135, 98, ...],
        "color": "rgba(10, 200, 110, 1)"
      },
      {
        "label": "失败请求",
        "data": [5, 8, 3, ...],
        "color": "rgba(255, 70, 70, 1)"
      }
    ]
  }
}
```

#### 5. 请求日志

**查询日志**
```http
GET /api/logs?group_id=1&status=error&page=1&limit=20
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "timestamp": "2024-01-01T00:00:00Z",
        "group_name": "openai",
        "model": "gpt-4o-mini",
        "is_success": false,
        "status_code": 429,
        "duration_ms": 1500,
        "error_message": "Rate limit exceeded",
        "retries": 2
      }
    ],
    "pagination": {...}
  }
}
```

#### 6. 系统设置

**获取设置**
```http
GET /api/settings
Authorization: Bearer <auth_key>

Response:
{
  "success": true,
  "data": {
    "app_url": "http://localhost:3001",
    "request_timeout": 600,
    "max_retries": 3,
    "blacklist_threshold": 3,
    ...
  }
}
```

**更新设置**
```http
PUT /api/settings
Authorization: Bearer <auth_key>

{
  "request_timeout": 300,
  "max_retries": 5,
  "blacklist_threshold": 5
}
```

### 代理端API

**基础路径**: `/proxy/:group_name`
**认证方式**: 与原始AI服务一致，但使用配置的代理密钥

#### 代理调用示例

**OpenAI格式代理**
```http
POST /proxy/openai/v1/chat/completions
Authorization: Bearer <proxy_key>
Content-Type: application/json

{
  "model": "gpt-4o-mini",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "stream": false
}
```

**Gemini格式代理**
```http
POST /proxy/gemini/v1beta/models/gemini-2.0-flash-exp:generateContent?key=<proxy_key>
Content-Type: application/json

{
  "contents": [
    {"parts": [{"text": "Hello"}]}
  ]
}
```

**Claude格式代理**
```http
POST /proxy/anthropic/v1/messages
x-api-key: <proxy_key>
anthropic-version: 2023-06-01
Content-Type: application/json

{
  "model": "claude-sonnet-4-20250514",
  "messages": [
    {"role": "user", "content": "Hello"}
  ]
}
```

## 部署架构

### 1. 单机部署

```
┌─────────────────────────┐
│     GPT-Load Server     │
│   ┌─────────────────┐   │
│   │   Web UI        │   │
│   ├─────────────────┤   │
│   │   API Gateway   │   │
│   ├─────────────────┤   │
│   │   Proxy Server  │   │
│   ├─────────────────┤   │
│   │   SQLite DB     │   │
│   └─────────────────┘   │
└─────────────────────────┘
```

### 2. 集群部署

```
       ┌─────────────────┐
       │   Load Balancer │
       └────────┬────────┘
                │
    ┌───────────┼───────────┐
    │           │           │
┌───▼───┐   ┌───▼───┐   ┌───▼───┐
│Master │   │Slave 1│   │Slave N│
│ Node  │   │ Node  │   │ Node  │
└───┬───┘   └───┬───┘   └───┬───┘
    │           │           │
    └───────────┼───────────┘
                │
    ┌───────────▼───────────┐
    │                       │
┌───▼───┐               ┌───▼───┐
│ MySQL │               │ Redis │
│ DB    │               │ Cache │
└───────┘               └───────┘
```

### 3. Docker部署

**单机Docker运行：**
```bash
docker run -d --name gpt-load \
  -p 3001:3001 \
  -e AUTH_KEY=sk-123456 \
  -e DATABASE_DSN=./data/gpt-load.db \
  -v $(pwd)/data:/app/data \
  ghcr.io/tbphp/gpt-load:latest
```

**Docker Compose部署：**
```yaml
version: '3.8'
services:
  gpt-load:
    image: ghcr.io/tbphp/gpt-load:latest
    ports:
      - "3001:3001"
    environment:
      - AUTH_KEY=sk-123456
      - DATABASE_DSN=mysql://user:pass@mysql:3306/gptload
      - REDIS_DSN=redis://redis:6379
    depends_on:
      - mysql
      - redis

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_DATABASE=gptload
      - MYSQL_USER=gptload
      - MYSQL_PASSWORD=password
      - MYSQL_ROOT_PASSWORD=rootpassword

  redis:
    image: redis:7-alpine
```

## 性能优化设计

### 1. 密钥池优化
- **O(1)密钥选择**: 使用Redis List的RPOPLPUSH实现轮换
- **异步状态更新**: 避免阻塞请求处理
- **批量数据库操作**: 减少数据库连接开销

### 2. HTTP客户端优化
- **连接池复用**: 独立的流式和非流式客户端池
- **HTTP/2支持**: 启用HTTP/2以提高并发性能
- **自定义缓冲区**: 优化流式传输的缓冲区大小

### 3. 缓存策略
- **分层缓存**: 内存 + Redis 双层缓存
- **热重载**: 配置变更实时同步到所有节点
- **缓存预热**: 启动时预加载关键数据

### 4. 日志优化
- **异步写入**: 批量写入数据库，减少I/O开销
- **日志聚合**: 按小时聚合统计数据
- **延迟删除**: 定期清理过期日志

## 监控和运维

### 1. 健康检查
```http
GET /health

Response:
{
  "status": "healthy",
  "timestamp": "2024-01-01T00:00:00Z",
  "uptime": "2h30m45s"
}
```

### 2. 关键指标
- **QPS**: 每秒请求数
- **延迟**: P50/P95/P99响应时间
- **错误率**: 按状态码分类的错误率
- **密钥状态**: 有效/无效密钥数量
- **缓存命中率**: Redis缓存效率

### 3. 日志级别
- **DEBUG**: 详细的调试信息
- **INFO**: 关键操作日志
- **WARN**: 警告信息
- **ERROR**: 错误和异常

### 4. 优雅关闭
- **信号处理**: 捕获SIGINT/SIGTERM信号
- **连接排空**: 等待现有请求完成
- **服务停止**: 按依赖顺序停止各项服务
- **资源释放**: 清理数据库连接和缓存

## 安全设计

### 1. 认证授权
- **双重认证**: 管理端和代理端分离认证
- **密钥轮换**: 支持代理密钥的热更新
- **权限隔离**: 分组级别的访问控制

### 2. 数据安全
- **密钥脱敏**: 日志中自动脱敏API密钥
- **传输加密**: 支持HTTPS/TLS加密
- **数据库加密**: 支持数据库连接加密

### 3. 防护机制
- **速率限制**: 可配置的并发请求限制
- **输入验证**: 严格的参数验证和清理
- **错误处理**: 统一的错误响应，避免信息泄露

这个架构设计确保了GPT-Load在高并发、高可用、易扩展的同时，提供了完整的管理功能和监控能力，非常适合企业级AI服务代理需求。
