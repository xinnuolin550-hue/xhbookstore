# 接口文档

> API 技术文档索引 | 基础路径: `/api/mp/v1`

## 接口分组

### 认证模块 (AuthController)
| 方法 | 路径 | 说明 | 详细文档 |
|------|------|------|----------|
| POST | /auth/wechat-phone-login | 微信手机号登录 | C01 |
| POST | /auth/refresh-token | 刷新Token | C01 |
| GET | /auth/session | 校验登录态 | C01 |
| POST | /auth/logout | 退出登录 | C01 |

### 用户端 (UserController)
| 方法 | 路径 | 说明 | 详细文档 |
|------|------|------|----------|
| GET | /user/home | 用户首页 | U01 |
| POST | /user/member-code | 生成动态会员码 | U02 |
| GET | /user/borrows | 本人借阅记录 | U03 |
| GET | /user/borrows/{id} | 借阅详情 | C08 |
| GET | /user/points-records | 本人积分记录 | U05 |
| GET | /user/points-records/{id} | 积分详情 | C09 |

### 员工端 (StaffController)
| 方法 | 路径 | 说明 | 详细文档 |
|------|------|------|----------|
| GET | /staff/home | 员工首页 | E01 |
| POST | /staff/member-code/scan | 解析会员码 | E06 |
| GET | /staff/members/{id}/overview | 会员概要 | E07 |
| GET | /staff/borrows | 全市借阅列表 | E02 |
| GET | /staff/borrows/{id} | 借阅详情 | E02 |
| POST | /staff/borrow-returns | 办理还书 | E02 |
| GET | /staff/members/{id}/borrows | 会员借阅记录 | E09 |
| POST | /staff/members/{id}/borrows | 办理借阅 | E08 |
| GET | /staff/points-reasons | 积分事项 | E10 |
| POST | /staff/members/{id}/points/add | 增加积分 | E10 |
| POST | /staff/members/{id}/points/deduct | 消耗积分 | E10 |
| GET | /staff/points-records | 全市积分列表 | E04 |
| GET | /staff/points-records/{id} | 积分详情 | E04 |

### 账号管理 (AccountController)
| 方法 | 路径 | 说明 | 详细文档 |
|------|------|------|----------|
| GET | /account/cancel-eligibility | 注销资格查询 | C03 |
| POST | /account/cancel | 注销账号 | C03 |

### 文件上传 (FileController)
| 方法 | 路径 | 说明 | 详细文档 |
|------|------|------|----------|
| POST | /files/book-attachment-images | 上传图书图片 | C04 |

## 通用规范

- **Content-Type**: `application/json;charset=UTF-8`（上传除外）
- **认证方式**: `Authorization: Bearer <accessToken>`
- **响应格式**: `{"code":0,"message":"操作成功","data":{},"requestId":"..."}`
- **分页格式**: `{"list":[],"pageNo":1,"pageSize":20,"total":100,"hasMore":true}`
