# 刷新 Token

> **接口路径**: `POST /api/mp/v1/auth/refresh-token`
> **认证要求**: 否（使用 refreshToken 换新，无需 Bearer 认证）
> **Content-Type**: application/json;charset=UTF-8
> **对应代码**: `AuthController.java`

## 接口说明

使用长期有效的 `refreshToken`（有效期 30 天）换取新的短期 `accessToken`（有效期 2 小时）。用于 `accessToken` 过期后无感续期，避免用户频繁重新登录。

---

## 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| refreshToken | String | 是 | 无 | 登录时下发的 refreshToken（JWT），有效期 2592000 秒 |

## 请求报文

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJ0eXBlIjoicmVmcmVzaCIsImp0aSI6IjU1NjZlMDQwLWFmM2ItNDY4OC1hYjRlLWI3Y2QxOTk5YTAxMCIsImlhdCI6MTc1MTU1MjAwMCwiZXhwIjoxNzU0MTQ0MDAwfQ.def456signature"
}
```

## curl 测试

```bash
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJ0eXBlIjoicmVmcmVzaCIsImp0aSI6IjU1NjZlMDQwLWFmM2ItNDY4OC1hYjRlLWI3Y2QxOTk5YTAxMCIsImlhdCI6MTc1MTU1MjAwMCwiZXhwIjoxNzU0MTQ0MDAwfQ.def456signature"

curl -X POST http://localhost:8091/api/mp/v1/auth/refresh-token \
  -H "Content-Type: application/json;charset=UTF-8" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

## 响应参数

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 0=成功 |
| message | String | 提示信息 |
| data.accessToken | String | 新的访问令牌（JWT），有效期 7200 秒 |
| data.expiresIn | Long | accessToken 有效期（秒），固定 7200 |
| requestId | String | 本次请求唯一标识 |

## 成功响应

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJpc1N0YWZmIjp0cnVlLCJwaG9uZSI6IjEzODAwMDAwMDEiLCJtZW1iZXJJZCI6MTIzLCJ0eXBlIjoiYWNjZXNzIiwiaWF0IjoxNzUxNTU5MjAwLCJleHAiOjE3NTE1NjY0MDB9.newAccessTokenSignature",
    "expiresIn": 7200
  },
  "requestId": "req_20260630120101"
}
```

## 错误响应

### refreshToken 已过期

```json
{
  "code": 10003,
  "message": "refreshToken已过期，请重新登录",
  "data": null,
  "requestId": "req_20260630120102"
}
```

### refreshToken 无效（格式错误或类型不匹配）

```json
{
  "code": 10004,
  "message": "无效的refreshToken",
  "data": null,
  "requestId": "req_20260630120103"
}
```

### 缺少 refreshToken

```json
{
  "code": 400,
  "message": "缺少refreshToken",
  "data": null,
  "requestId": "req_20260630120104"
}
```

## 业务逻辑

1. 接收 `refreshToken`，解析 JWT 并校验签名
2. 验证 `type` 字段是否为 `"refresh"`（防止用 accessToken 冒充）
3. 校验通过后，继承原 token 中的 `userId`、`isStaff`、`phone`、`memberId` 生成新的 `accessToken`
4. 新 `accessToken` 的 `iat`（签发时间）更新为当前时间，`exp` 顺延 7200 秒
5. `refreshToken` 本身不做轮替（同一个 refreshToken 可反复使用直到过期）
6. JWT 解析失败（签名错误、篡改等）统一返回 `AUTH_TOKEN_INVALID`

## 代码关联

| 文件 | 说明 |
|------|------|
| `AuthController.java` | 控制器，校验 refreshToken 并签发新 accessToken |
| `SecurityProperties.java` | JWT 密钥及有效期配置 |
