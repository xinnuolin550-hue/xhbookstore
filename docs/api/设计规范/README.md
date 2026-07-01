# 设计规范

> API 接口设计规范与命名约定

## 响应格式

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "requestId": "req_20260630_143000_001"
}
```

## 错误码规范

| 错误码 | 说明 |
|--------|------|
| 0 | 操作成功 |
| 400 | 参数无效 |
| 401 | 未登录或登录已过期 |
| 500 | 服务器内部错误 |
| 10004 | 无效的访问令牌 |
| 10005 | Token已过期 |
| 20001 | 会员不存在 |
| 30001 | 积分不足 |
| 30003 | 无效的积分事项 |
| 40001 | 不允许办理借阅 |
| 40002 | 至少需要一本图书 |
| 40004 | 不允许办理还书 |
| 50001 | 文件上传失败 |
| 50002 | 文件大小超出限制 |
| 50003 | 不支持的文件类型 |

## URL 命名规范

- 基础路径: `/api/mp/v1`
- 资源用复数名词: `/borrows`, `/points-records`
- 子资源: `/members/{memberId}/borrows`
- 动作用动词: `/scan`, `/add`, `/deduct`, `/cancel`

## 认证规范

- 白名单接口: 登录、刷新Token（无需Token）
- 需认证接口: 所有业务接口（Header: `Authorization: Bearer <accessToken>`）
- accessToken: 2小时有效（dev），1小时（prod）
- refreshToken: 30天有效（dev），7天（prod）

## 分页规范

```json
{
  "list": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 100,
  "hasMore": true
}
```

- pageNo 从 1 开始
- pageSize 默认 20，最大 100
- hasMore = pageNo * pageSize < total

## 订单号规范

| 前缀 | 说明 | 示例 |
|------|------|------|
| DY | 借书单 | DY20260630143000123456 |
| HS | 还书明细 | HS20260630143000654321 |
| JSDD | 购书订单 | JSDD20260630143000789012 |
| IN | 积分入账 | IN20260628120000123456 |
| OT | 积分出账 | OT20260628120000123456 |

## 文件组织规范

Controller 层只做参数校验和路由，业务逻辑在 Service 层。

```
xhbookstore-api/
├── config/         # 配置类
├── constant/       # 常量/错误码
├── controller/     # 控制器（路由+参数校验）
├── domain/         # 实体类
├── exception/      # 异常处理
├── filter/         # JWT过滤器
├── interceptor/    # 拦截器
├── mapper/         # MyBatis Mapper
├── model/          # 响应模型（ApiResponse, PageResult）
└── service/        # 业务逻辑层
```
