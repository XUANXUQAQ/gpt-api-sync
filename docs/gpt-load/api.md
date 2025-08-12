# GPT-Load API 文档

本文档详细介绍了 GPT-Load 项目提供的所有 API 接口，包括管理端 API 和代理端 API。

## 1. 管理端 API

管理端 API 用于配置和监控 GPT-Load 服务，包括分组、密钥、日志和系统设置的管理。

- **基础路径**: `/api`
- **认证方式**: 所有请求都需要在 `Authorization` 头中提供 Bearer Token，其值为在环境变量中配置的 `AUTH_KEY`。
  ```
  Authorization: Bearer <your_auth_key>
  ```

---

### 1.1. 认证 (Auth)

#### **POST** `/api/auth/login`

用户登录认证，获取访问权限。

**请求体 (Request Body):**
```json
{
  "auth_key": "your_secret_auth_key"
}
```

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "message": "Authentication successful"
}
```

**失败响应 (Failure Response):**
- **状态码**: `401 Unauthorized`
```json
{
  "success": false,
  "message": "Invalid authentication key"
}
```

---

### 1.2. 分组管理 (Groups)

#### **GET** `/api/groups`

获取所有分组的列表信息。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "openai-group",
      "display_name": "OpenAI 通用分组",
      "description": "用于处理所有 OpenAI 模型的请求",
      "channel_type": "openai",
      "endpoint": "http://localhost:3001/proxy/openai-group",
      "test_model": "gpt-4o-mini",
      "upstreams": [
        {
          "url": "https://api.openai.com",
          "weight": 1
        }
      ],
      "api_keys": [
        {
          "id": 101,
          "key_value": "sk-...",
          "status": "active",
          "request_count": 120,
          "failure_count": 2,
          "created_at": "2023-10-01T10:00:00Z"
        }
      ],
      "proxy_keys": [
        "proxy-key-abc",
        "proxy-key-def"
      ]
    }
  ]
}
```

#### **POST** `/api/groups`

创建一个新的分组。

**请求体 (Request Body):**
```json
{
  "name": "anthropic-claude",
  "display_name": "Anthropic Claude 分组",
  "description": "处理 Claude Sonnet 和 Opus 模型",
  "channel_type": "anthropic",
  "test_model": "claude-3-sonnet-20240229",
  "upstreams": [
    {
      "url": "https://api.anthropic.com",
      "weight": 1
    }
  ],
  "proxy_keys": "proxy-claude-1"
}
```

**成功响应 (Success Response):**
- **状态码**: `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 2,
    "name": "anthropic-claude",
    "display_name": "Anthropic Claude 分组",
    "channel_type": "anthropic",
    "endpoint": "http://localhost:3001/proxy/anthropic-claude",
    "upstreams": [{"url": "https://api.anthropic.com", "weight": 1}],
    "proxy_keys": ["proxy-claude-1"]
  }
}
```

#### **PUT** `/api/groups/:id`

更新指定 ID 的分组信息。

**URL 参数:**
- `id` (integer, required): 分组的唯一ID。

**请求体 (Request Body):**
```json
{
  "display_name": "更新后的 OpenAI 分组",
  "description": "这是一个更新后的描述",
  "config": {
    "request_timeout": 300,
    "max_retries": 5,
    "blacklist_threshold": 10
  }
}
```

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "display_name": "更新后的 OpenAI 分组",
    "description": "这是一个更新后的描述",
    "config": {
      "request_timeout": 300,
      "max_retries": 5,
      "blacklist_threshold": 10
    }
  }
}
```

#### **DELETE** `/api/groups/:id`

删除指定 ID 的分组。

**URL 参数:**
- `id` (integer, required): 分组的唯一ID。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "message": "Group deleted successfully"
}
```

---

### 1.3. 密钥管理 (Keys)

#### **GET** `/api/keys`

根据条件查询密钥列表，支持分页。

**查询参数 (Query Parameters):**
- `group_id` (integer, required): 分组ID。
- `status` (string, optional): 密钥状态 (`active`, `invalid`)。
- `page` (integer, optional, default: 1): 页码。
- `limit` (integer, optional, default: 20): 每页数量。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 101,
        "key_value": "sk-abc...xyz",
        "status": "active",
        "request_count": 120,
        "failure_count": 2,
        "last_used_at": "2023-10-10T10:00:00Z",
        "created_at": "2023-10-01T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 55,
      "pages": 3
    }
  }
}
```

#### **POST** `/api/keys/add-multiple`

向指定分组批量添加密钥。

**请求体 (Request Body):**
```json
{
  "group_id": 1,
  "keys_text": "sk-key-one\nsk-key-two\nsk-key-three,sk-key-four"
}
```

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "added_count": 4,
    "ignored_count": 0,
    "total_in_group": 59
  }
}
```

#### **POST** `/api/keys/add-async`

异步向指定分组批量添加密钥，返回一个任务ID用于跟踪进度。

**请求体 (Request Body):**
```json
{
  "group_id": 1,
  "keys_text": "sk-key-one\nsk-key-two\n..."
}
```

**成功响应 (Success Response):**
- **状态码**: `202 Accepted`
```json
{
  "success": true,
  "data": {
    "task_id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "status": "processing",
    "progress": 0
  }
}
```

#### **POST** `/api/keys/delete-multiple`

从指定分组批量删除密钥。

**请求体 (Request Body):**
```json
{
  "group_id": 1,
  "keys_text": "sk-key-one\nsk-key-two"
}
```

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "deleted_count": 2,
    "total_in_group": 57
  }
}
```

#### **POST** `/api/keys/validate-group`

验证指定分组下的所有密钥（或特定状态的密钥）。

**请求体 (Request Body):**
```json
{
  "group_id": 1,
  "status": "invalid"
}
```

**成功响应 (Success Response):**
- **状态码**: `202 Accepted`
```json
{
  "success": true,
  "data": {
    "task_id": "b2c3d4e5-f6a7-8901-2345-67890abcdef1",
    "status": "processing",
    "progress": 0
  }
}
```

---

### 1.4. 仪表板 (Dashboard)

#### **GET** `/api/dashboard/stats`

获取仪表板的核心统计数据。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
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
      "trend_is_growth": false
    }
  }
}
```

#### **GET** `/api/dashboard/chart`

获取用于绘制图表的时序数据。

**查询参数 (Query Parameters):**
- `groupId` (integer, optional): 按分组ID筛选。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "labels": ["2023-10-10T08:00:00Z", "2023-10-10T09:00:00Z", "2023-10-10T10:00:00Z"],
    "datasets": [
      {
        "label": "成功请求",
        "data": [120, 135, 98],
        "color": "rgba(10, 200, 110, 1)"
      },
      {
        "label": "失败请求",
        "data": [5, 8, 3],
        "color": "rgba(255, 70, 70, 1)"
      }
    ]
  }
}
```

---

### 1.5. 请求日志 (Logs)

#### **GET** `/api/logs`

查询请求日志，支持筛选和分页。

**查询参数 (Query Parameters):**
- `group_id` (integer, optional): 分组ID。
- `status` (string, optional): 请求状态 (`success`, `error`)。
- `page` (integer, optional, default: 1): 页码。
- `limit` (integer, optional, default: 20): 每页数量。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "c3d4e5f6-a7b8-9012-3456-7890abcdef12",
        "timestamp": "2023-10-10T10:01:30Z",
        "group_name": "openai-group",
        "key_value": "sk-abc...xyz",
        "model": "gpt-4o-mini",
        "is_success": false,
        "status_code": 429,
        "duration_ms": 1500,
        "error_message": "Rate limit exceeded for model `gpt-4o-mini`",
        "retries": 3
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 15,
      "pages": 1
    }
  }
}
```

---

### 1.6. 系统设置 (Settings)

#### **GET** `/api/settings`

获取所有可动态配置的系统设置。

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "data": {
    "app_url": "http://localhost:3001",
    "request_timeout": 600,
    "max_retries": 3,
    "blacklist_threshold": 3,
    "key_check_interval": 3600,
    "log_async_enabled": true,
    "log_queue_size": 1000
  }
}
```

#### **PUT** `/api/settings`

更新系统设置。

**请求体 (Request Body):**
```json
{
  "request_timeout": 300,
  "max_retries": 5,
  "blacklist_threshold": 10
}
```

**成功响应 (Success Response):**
- **状态码**: `200 OK`
```json
{
  "success": true,
  "message": "Settings updated successfully"
}
```

---

## 2. 代理端 API

代理端 API 用于将来自客户端的 AI 请求透明地转发到上游服务。

- **基础路径**: `/proxy/:group_name`
- **认证方式**: 与原始 AI 服务一致，但使用在分组中配置的**代理密钥 (Proxy Key)**。

#### **POST** `/proxy/:group_name/*`

**URL 参数:**
- `group_name` (string, required): 目标分组的名称。
- `*` (wildcard): 匹配原始 AI 服务的完整路径。

**请求示例 (以 OpenAI 为例):**

假设已创建一个名为 `openai-general` 的分组，其代理密钥为 `proxy-abc-123`。

**原始请求:**
```http
POST https://api.openai.com/v1/chat/completions
Authorization: Bearer sk-real-openai-key
Content-Type: application/json

{
  "model": "gpt-4o-mini",
  "messages": [{"role": "user", "content": "Hello!"}]
}
```

**代理请求:**
```http
POST http://localhost:3001/proxy/openai-general/v1/chat/completions
Authorization: Bearer proxy-abc-123
Content-Type: application/json

{
  "model": "gpt-4o-mini",
  "messages": [{"role": "user", "content": "Hello!"}]
}
```

**响应:**
代理服务器会将上游服务（如 OpenAI）的响应原封不动地返回给客户端，包括状态码、响应头和响应体。
