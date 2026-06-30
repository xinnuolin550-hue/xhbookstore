---
name: deploy-prod
description: Build and deploy the entire project to production. Rebuilds all modules with production profile, uploads to production server, and restarts services with security-hardened settings.
---

# deploy-prod — 生产环境部署

构建并部署到生产服务器。**⚠️ 操作前请确认：**
- 已通过代码审查
- 数据库备份已完成
- 在低峰时段执行

## 部署流程

### 1. 构建后端（prod 配置）

使用生产配置构建所有模块（跳过测试，生产 profile）：

```bash
cd D:/Code/study/xhbookstorev2
mvn clean package -DskipTests -pl xhbookstore-admin,xhbookstore-api -am
```

### 2. 构建前端（production 配置）

构建前端生产包（使用 .env.production，API 前缀为 /prod-api）：

```bash
cd D:/Code/study/xhbookstorev2/xhbookstore-ui
npm run build:prod
```

### 3. 上传到生产服务器

上传 jar 包和前端静态文件：

```bash
# 上传后端 jar
scp xhbookstore-admin/target/xhbookstore-admin.jar root@<prod-server>:/www/xhbookstore/admin/
scp xhbookstore-api/target/xhbookstore-api.jar root@<prod-server>:/www/xhbookstore/api/

# 上传前端 dist
scp -r xhbookstore-ui/dist/* root@<prod-server>:/www/xhbookstore/ui/
```

### 4. 远程重启服务（生产 profile）

SSH 登录生产服务器并重启：

```bash
ssh root@<prod-server> << 'EOF'
# 停止旧服务
pkill -f xhbookstore-admin.jar || true
pkill -f xhbookstore-api.jar || true
sleep 2

# 启动 Admin（生产配置：swagger 关闭，日志 WARN 级别，连接池 20-200）
cd /www/xhbookstore/admin
nohup java -jar -Xms512m -Xmx1024m xhbookstore-admin.jar --spring.profiles.active=prod > logs/admin.log 2>&1 &

# 启动 API（生产配置：限流严格，JWT 1h 过期，swagger 关闭）
cd /www/xhbookstore/api
nohup java -jar -Xms512m -Xmx1024m xhbookstore-api.jar --spring.profiles.active=prod > logs/api.log 2>&1 &

echo "Production services restarted"
EOF
```

### 5. 验证

```bash
echo "Admin:  $(curl -s -o /dev/null -w '%{http_code}' http://<prod-server>:8090/)"
echo "API:    $(curl -s -o /dev/null -w '%{http_code}' http://<prod-server>:8091/)"
echo "UI:     $(curl -s -o /dev/null -w '%{http_code}' http://<prod-server>/)"
```

### 6. 生产环境差异

| 配置项 | 生产值 |
|--------|--------|
| Swagger/Springdoc | 关闭 |
| Druid 监控页 | 关闭 |
| 日志级别 | WARN / ERROR |
| JWT accessToken | 1小时 |
| JWT refreshToken | 7天 |
| 登录限流 | 5次/分钟/IP |
| 连接池 min-max | 20-200 |
| 环境变量支持 | JWT_SECRET, WECHAT_APP_ID, WECHAT_APP_SECRET, COS_SECRET_ID, COS_SECRET_KEY |
