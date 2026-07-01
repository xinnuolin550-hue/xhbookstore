# 工具说明

> 开发工具与环境配置

## 开发环境

| 工具 | 版本/说明 |
|------|-----------|
| JDK | 17 |
| Maven | 3.9+ |
| Spring Boot | 3.5.14 |
| MySQL | 8.0 |
| Redis | 7.x |
| Node.js | >=8.9 |
| Vue CLI | 4.4.6 |

## 快速启动

### 1. 后端（API模块）
```bash
cd D:/Code/study/xhbookstorev2
mvn clean package -DskipTests -pl xhbookstore-api -am
java -jar xhbookstore-api/target/xhbookstore-api.jar --spring.profiles.active=dev
```

### 2. 后端（Admin模块）
```bash
mvn clean package -DskipTests -pl xhbookstore-admin -am
java -jar xhbookstore-admin/target/xhbookstore-admin.jar --spring.profiles.active=dev
```

### 3. 前端
```bash
cd xhbookstore-ui
npm install
npm run dev
```

## Claude Code Skills

| Skill | 说明 |
|-------|------|
| `/deploy-dev` | 一键构建+启动本地所有服务 |
| `/deploy-staging` | 构建+部署到测试服务器 |
| `/deploy-prod` | 构建+部署到生产服务器 |

## 环境配置

| 环境 | 配置文件 | DB/Redis |
|------|----------|----------|
| dev | application-dev.yml | 152.136.127.168 |
| staging | application-staging.yml | localhost |
| prod | application-prod.yml | localhost |

## 数据库连接

```
Host: 152.136.127.168:3306
Database: xhbookstore
Charset: utf8mb4
User: xhbookstore
```

## Redis连接

```
Host: 152.136.127.168:6379
Password: xhbookstore
```
