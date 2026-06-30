---
name: deploy-staging
description: Build the entire project and deploy to the staging/test server (152.136.127.168). Rebuilds all modules with staging profile, uploads via SCP, and restarts remote services.
---

# deploy-staging — 测试环境部署

构建并部署到测试服务器 `152.136.127.168`。

## 部署流程

### 1. 构建后端（staging 配置）

在本地使用 Maven 构建所有模块：

```bash
cd D:/Code/study/xhbookstorev2
mvn clean package -DskipTests -pl xhbookstore-admin,xhbookstore-api -am
```

### 2. 构建前端（staging 配置）

构建前端测试包（使用 .env.staging，API 前缀为 /stage-api）：

```bash
cd D:/Code/study/xhbookstorev2/xhbookstore-ui
npm run build:stage
```

### 3. 上传到测试服务器

上传 jar 包和前端静态文件（需要提前配置 SSH 免密登录）：

```bash
# 上传后端 jar
scp xhbookstore-admin/target/xhbookstore-admin.jar root@152.136.127.168:/www/xhbookstore/admin/
scp xhbookstore-api/target/xhbookstore-api.jar root@152.136.127.168:/www/xhbookstore/api/

# 上传前端 dist
scp -r xhbookstore-ui/dist/* root@152.136.127.168:/www/xhbookstore/ui/
```

### 4. 远程重启服务

SSH 登录测试服务器并重启：

```bash
ssh root@152.136.127.168 << 'EOF'
# 停止旧服务
pkill -f xhbookstore-admin.jar || true
pkill -f xhbookstore-api.jar || true
sleep 2

# 启动 Admin（端口 8090）
cd /www/xhbookstore/admin
nohup java -jar xhbookstore-admin.jar --spring.profiles.active=staging > logs/admin.log 2>&1 &

# 启动 API（端口 8091）
cd /www/xhbookstore/api
nohup java -jar xhbookstore-api.jar --spring.profiles.active=staging > logs/api.log 2>&1 &

echo "Services restarted"
EOF
```

### 5. 验证

```bash
echo "Admin:  $(curl -s -o /dev/null -w '%{http_code}' http://152.136.127.168:8090/)"
echo "API:    $(curl -s -o /dev/null -w '%{http_code}' http://152.136.127.168:8091/)"
echo "Swagger: $(curl -s -o /dev/null -w '%{http_code}' http://152.136.127.168:8090/swagger-ui.html)"
```

## 服务器信息

| 项目 | 值 |
|------|-----|
| 服务器 IP | 152.136.127.168 |
| Admin 端口 | 8090 |
| API 端口 | 8091 |
| 部署路径 | /www/xhbookstore/ |
