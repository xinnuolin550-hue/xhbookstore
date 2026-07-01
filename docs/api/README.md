# 新华书店小程序 API 模块 — 项目文档

> 基础路径: `http://localhost:8091/api/mp/v1` | 更新时间: 2026-06-30

## 目录结构

```
docs/api/
├── README.md                    # 本文件 — 项目总览
├── 工具说明/                    # 开发工具、环境配置说明
├── 接口文档/                    # API 接口技术文档（按模块分组）
├── 开发计划/                    # 迭代计划、版本规划
├── 设计规范/                    # 接口设计规范、命名约定
└── 需求文档/                    # ★ 核心目录 — 业务需求文档
    └── 页面级需求/              # 按页面维度组织的需求文档
        ├── 版本记录文件/        # 版本变更记录
        ├── C01~C11/            # 通用基础组件 / 弹窗页面
        ├── E01~E11/            # 员工端页面
        └── U01~U05/            # 用户端页面
```

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| xhbookstore-api | 8091 | 小程序 API（JWT 认证 + AOP 日志 + 统一错误码） |
| xhbookstore-admin | 8090 | 管理后台 |
| xhbookstore-ui | 80 | 管理后台前端（Vue2 + ElementUI） |

## API 基础信息

- **Content-Type**: `application/json;charset=UTF-8`
- **认证方式**: `Authorization: Bearer <accessToken>`
- **统一响应格式**: `{ "code": 0, "message": "操作成功", "data": {}, "requestId": null }`

## 开发环境

| 项目 | 值 |
|------|-----|
| Java | 17 |
| Spring Boot | 3.5.14 |
| MySQL | 8.0 @ 152.136.127.168:3306 |
| Redis | 152.136.127.168:6379 |
