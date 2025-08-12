# GPT-API-Sync 服务 API 文档

本文档详细描述了 GPT-API-Sync 服务的可用 API 端点。

## 根端点

### `GET /`

提供服务的基本信息和所有可用端点的列表。

-   **方法**: `GET`
-   **路径**: `/`
-   **描述**: 获取服务的状态、版本和可用端点列表。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    {
      "service": "GPT-API同步服务",
      "version": "1.0.0",
      "status": "running",
      "endpoints": {
        "sync": "/sync - 触发同步操作",
        "status": "/status - 查看服务状态",
        "health": "/health - 健康检查",
        "getGptLoadInfo": "/api/gpt-load - 获取gpt-load信息",
        "getNewApiInfo": "/api/new-api - 获取new-api信息",
        "getConfig": "/config - 获取当前配置（屏蔽敏感信息）",
        "reloadConfig": "POST /config/reload - 从文件重新加载配置",
        "updateConfig": "PUT /config - 更新配置文件并重新加载"
      }
    }
    ```

---

## 同步端点

### `POST /sync`

触发 gpt-load 和 new-api 之间的数据同步。

-   **方法**: `POST`
-   **路径**: `/sync`
-   **描述**: 从 gpt-load 获取分组，并将其智能同步（创建或更新）为 new-api 中的渠道。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    {
      "success": true,
      "message": "同步成功完成",
      "groups_fetched": 5,
      "channels_created": 2,
      "channels_updated": 3,
      "channels_failed": 0,
      "duration_ms": 1520
    }
    ```
-   **失败响应 (500 Internal Server Error)**:
    ```json
    {
      "success": false,
      "error": "同步失败: 从 gpt-load 获取的分组列表为空或获取失败",
      "duration_ms": 120
    }
    ```

---

## 状态与健康检查

### `GET /status`

获取服务的当前状态和依赖连接状态。

-   **方法**: `GET`
-   **路径**: `/status`
-   **描述**: 提供服务版本、依赖服务（gpt-load, new-api）的连接状态和基本配置信息。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    {
      "service": "GPT-API同步服务",
      "version": "1.0.0",
      "connections": {
        "gptLoad": true,
        "newApi": true
      },
      "config": {
        "gptLoadUrl": "http://localhost:8001",
        "newApiUrl": "http://localhost:3000"
      }
    }
    ```

### `GET /health`

执行健康检查。

-   **方法**: `GET`
-   **路径**: `/health`
-   **描述**: 检查核心服务（如 gpt-load）的连通性，并返回服务的整体健康状况。
-   **请求**: 无
-   **健康响应 (200 OK)**:
    ```json
    {
      "gptLoad": "ok",
      "status": "healthy",
      "timestamp": 1678886400000
    }
    ```
-   **不健康响应 (503 Service Unavailable)**:
    ```json
    {
      "gptLoad": "error",
      "status": "unhealthy",
      "timestamp": 1678886400000
    }
    ```

---

## API 信息获取

### `GET /api/gpt-load`

获取 gpt-load 中的所有分组信息。

-   **方法**: `GET`
-   **路径**: `/api/gpt-load`
-   **描述**: 从 gpt-load 服务获取所有已配置的分组列表。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    [
      {
        "name": "group-1",
        "keys": ["key1", "key2"],
        "models": ["gpt-4", "gpt-3.5-turbo"],
        "channel_type": "openai"
      }
    ]
    ```
-   **失败响应 (500 Internal Server Error)**:
    ```json
    {
      "error": "获取 gpt-load 信息失败: [错误详情]"
    }
    ```

### `GET /api/new-api`

获取 new-api 中的所有渠道信息。

-   **方法**: `GET`
-   **路径**: `/api/new-api`
-   **描述**: 从 new-api 服务获取所有已配置的渠道列表。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    [
      {
        "id": 1,
        "name": "channel-1",
        "type": 1,
        "base_url": "http://localhost:8001/group/group-1",
        "models": "gpt-4,gpt-3.5-turbo",
        "group": "default",
        "status": 1
      }
    ]
    ```
-   **失败响应 (500 Internal Server Error)**:
    ```json
    {
      "error": "获取 new-api 信息失败: [错误详情]"
    }
    ```

---

## 配置管理

### `GET /config`

获取当前服务的配置信息。

-   **方法**: `GET`
-   **路径**: `/config`
-   **描述**: 返回当前加载的配置，敏感字段（如密钥）会被屏蔽。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    {
      "server": {
        "port": 8080
      },
      "gptLoad": {
        "baseUrl": "http://localhost:8001",
        "authKey": "******"
      },
      "newApi": {
        "baseUrl": "http://localhost:3000",
        "accessToken": "******"
      },
      "channelDefaults": {
        "group": "default",
        "priority": 0
      },
      "modelRedirect": {
          "standardModels": ["gpt-4", "gpt-3.5-turbo"]
      }
    }
    ```

### `POST /config/reload`

从文件重新加载配置。

-   **方法**: `POST`
-   **路径**: `/config/reload`
-   **描述**: 触发服务器从 `config.json` 文件重新加载配置。
-   **请求**: 无
-   **成功响应 (200 OK)**:
    ```json
    {
      "success": true,
      "message": "配置已成功从 config.json 重新加载。"
    }
    ```
-   **失败响应 (500 Internal Server Error)**:
    ```json
    {
      "success": false,
      "error": "重新加载配置时发生错误: [错误详情]"
    }
    ```

### `PUT /config`

更新配置文件并重新加载。

-   **方法**: `PUT`
-   **路径**: `/config`
-   **描述**: 接收一个新的配置对象，将其写入 `config.json` 文件，然后重新加载。如果请求中的敏感字段（`authKey`, `accessToken`）值为 `******`，则会保留原有的值。
-   **请求体**: `application/json`
    ```json
    {
      "server": {
        "port": 8081
      },
      "gptLoad": {
        "baseUrl": "http://localhost:8002",
        "authKey": "******"
      },
      "newApi": {
        "baseUrl": "http://localhost:3001",
        "accessToken": "new-secret-token"
      },
      "channelDefaults": {
        "group": "new-group",
        "priority": 10
      },
      "modelRedirect": {
          "standardModels": ["gpt-4o", "gpt-4-turbo"]
      }
    }
    ```
-   **成功响应 (200 OK)**:
    ```json
    {
      "success": true,
      "message": "配置已成功更新并重新加载。"
    }
    ```
-   **失败响应 (500 Internal Server Error)**:
    ```json
    {
      "success": false,
      "error": "更新配置时发生未知错误: [错误详情]"
    }