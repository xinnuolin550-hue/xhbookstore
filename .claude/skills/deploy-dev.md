---
name: deploy-dev
description: Build and deploy the entire project locally for development. Rebuilds API (port 8091), Admin (port 8090), and UI (port 80), then starts all services. Dev profile connects to shared DB/Redis at 152.136.127.168.
---

# deploy-dev — 本地开发部署

快速构建并启动所有服务，用于本地开发调试。

## 部署流程

### 1. 构建后端

```bash
cd D:/Code/study/xhbookstorev2
mvn clean package -DskipTests -pl xhbookstore-admin,xhbookstore-api -am
```

### 2. 构建前端

```bash
cd D:/Code/study/xhbookstorev2/xhbookstore-ui
npm run build:stage
```

### 3. 停止已有服务

停止占用 8090 和 8091 端口的 Java 进程：

```powershell
Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Get-NetTCPConnection -LocalPort 8091 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
```

### 4. 启动后端服务

以 dev profile 启动 Admin 模块（端口 8090）和 API 模块（端口 8091）：

```bash
cd D:/Code/study/xhbookstorev2
nohup java -jar xhbookstore-admin/target/xhbookstore-admin.jar --spring.profiles.active=dev > logs/admin.log 2>&1 &
nohup java -jar xhbookstore-api/target/xhbookstore-api.jar --spring.profiles.active=dev > logs/api.log 2>&1 &
```

### 5. 启动前端

```bash
cd D:/Code/study/xhbookstorev2/xhbookstore-ui
npm run dev
```

### 6. 验证

```bash
echo "Admin:  $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8090/)"
echo "API:    $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8091/)"
echo "UI:     $(curl -s -o /dev/null -w '%{http_code}' http://localhost:80/)"
```

## 端口说明

| 服务 | 端口 | 配置 | 数据库 |
|------|------|------|--------|
| Admin 后台 | 8090 | application-dev.yml | 152.136.127.168 |
| API 接口 | 8091 | application-dev.yml | 152.136.127.168 |
| UI 前端 | 80 | .env.development | - |
