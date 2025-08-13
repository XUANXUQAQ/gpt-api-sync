# GPT-API Sync Service

## 简介

本项目是一个用于同步两个不同GPT API管理平台渠道的服务。它能够从 `GPT-Load` 读取渠道配置，并将其同步到`New-API`。

该项目提供了一个简单的Web界面来触发同步、查看状态和管理配置。

## 主要功能

- **渠道同步**: 一键将源平台的渠道信息同步到目标平台。
- **服务状态监控**: 提供API端点以检查本服务及所依赖服务的连接状态。
- **Web UI**: 内置一个简单的前端页面，方便用户操作。
- **模型重定向**: 支持模型重定向功能。
- **配置管理**: 支持通过API和UI动态更新和重载配置。

## 快速开始

最简单的启动方式是使用 Docker Compose。

1.  **克隆项目**
    ```bash
    git clone https://github.com/XUANXUQAQ/gpt-api-sync
    cd gpt-api-sync
    ```

2.  **启动服务**
    在项目根目录下，运行以下命令：
    ```bash
    vim docker-compose.yml
    docker-compose up -d
    ```

3.  **访问服务**
    服务启动后，您可以通过浏览器访问 `http://localhost:7000` 来打开Web管理界面。

## Docker Compose 配置

以下是 `docker-compose.yml` 文件的内容：

```yaml
version: '3.8'
services:
  gpt-api-sync:
    container_name: gpt-api-sync
    restart: always
    image: ghcr.io/xuanxuqaq/gpt-api-sync:latest
    ports:
      - "7000:7000"
    volumes:
      - ./data:/app/data
```
