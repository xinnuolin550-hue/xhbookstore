# 代码 ↔ 文档 对照索引

> 标注每个 Java 源文件对应的需求文档，方便对照查阅。

## 目录树总览

```
docs/api/
├── README.md                           # 项目总览
├── CODE_DOC_MAPPING.md                 # ★ 本文件 — 代码文档对照索引
├── 工具说明/
│   └── README.md                       # 开发工具与环境配置说明
├── 接口文档/
│   └── API接口清单.md                   # 全部 26 个 API 端点清单
├── 开发计划/
│   └── README.md                       # 版本迭代计划
├── 设计规范/
│   └── README.md                       # API 设计规范、错误码、命名约定
└── 需求文档/
    └── 页面级需求/
        ├── 版本记录文件/
        │   ├── TODO.md                 # 待开发事项
        │   ├── v20260624.md            # v20260624 版本记录
        │   └── v20260625.md            # v20260625 版本记录
        ├── C01 - 登录弹层.md            # 微信登录/刷新Token/校验/退出
        ├── C02 - 设置页.md              # 用户设置（部分 TODO）
        ├── C03 - 注销须知页.md          # 账号注销资格检查+注销
        ├── C04 - 图片预览.md            # 图书图片上传（腾讯云COS）
        ├── C05 - 通用确认弹窗.md         # 通用二次确认弹窗 UI 组件
        ├── C06 - 通用结果反馈.md         # 通用操作结果反馈 UI 组件
        ├── C07 - 空状态及异常状态.md      # 空状态/网络错误/服务端错误/鉴权错误
        ├── C08 - 借阅详情页.md           # 借阅详情（用户端+员工端共用）
        ├── C09 - 积分详情页.md           # 积分记录详情（用户端+员工端共用）
        ├── C10 - 扫码登录确认页.md        # 员工扫码确认登录
        ├── C11 - 组件预览页.md            # 开发者组件预览页
        ├── E01 - 员工首页.md             # 员工端首页数据
        ├── E02 - 借阅列表页.md           # 全市借阅列表+还书操作
        ├── E04 - 积分列表页.md           # 全市积分记录列表
        ├── E06 - 微信扫码能力.md          # 扫码解析会员码
        ├── E07 - 扫码会员首页.md          # 扫码后会员概要页
        ├── E08 - 办理借阅页.md           # 为会员办理借书
        ├── E09 - 会员借阅记录页.md        # 查看会员全部借阅记录
        ├── E10 - 积分操作页.md           # 增加/消耗积分操作
        ├── E11 - 借阅卡开卡续费页.md      # 开卡续费（TODO规划）
        ├── U01 - 用户首页.md             # 用户端首页
        ├── U02 - 会员码弹层.md           # 生成动态会员二维码
        ├── U03 - 借阅记录页.md           # 用户本人借阅记录
        └── U05 - 积分记录页.md           # 用户本人积分记录
```

## Java 源码 → 文档对照

| Java 文件 | 对应文档 | 说明 |
|-----------|----------|------|
| **Controller 层** |||
| `AuthController.java` | C01 | 微信登录/刷新Token/校验/退出 |
| `UserController.java` | U01, U02, U03, U05, C08, C09 | 用户端全部接口 |
| `StaffController.java` | E01, E02, E04, E06, E07, E08, E09, E10, C08, C09, C10 | 员工端全部接口 |
| `AccountController.java` | C03 | 注销资格查询+注销执行 |
| `FileController.java` | C04 | 图书图片上传 |
| **Config 层** |||
| `ApiSecurityConfig.java` | C01 | 白名单路径配置 |
| `SecurityProperties.java` | C01 | JWT 密钥/过期时间配置 |
| `CorsConfig.java` | — | 跨域配置 |
| `CosConfig.java` | C04 | 腾讯云COS配置 |
| `WechatConfig.java` | C01 | 微信小程序配置 |
| `WebConfig.java` | — | Web MVC 配置 |
| **Aspect 层** |||
| `ApiLogAspect.java` | — | API 请求日志 AOP |
| `RateLimitAspect.java` | C01 | 登录接口限流 |
| **Filter 层** |||
| `JwtAuthenticationFilter.java` | C01 | JWT Token 解析与校验 |
| **Interceptor 层** |||
| `IdempotencyInterceptor.java` | — | 幂等性拦截器 |
| **Exception 层** |||
| `ApiException.java` | C06, C07 | 业务异常 |
| `ApiGlobalExceptionHandler.java` | C06, C07 | 全局异常处理→统一响应 |
| **Model 层** |||
| `ApiResponse.java` | 设计规范 | 统一响应模型 |
| `PageResult.java` | 设计规范 | 分页响应模型 |
| **Constant 层** |||
| `ApiErrorCode.java` | 设计规范 | 错误码枚举 |
| **Service 层** |||
| `IWechatService.java` / `WechatServiceImpl.java` | C01 | 微信手机号获取（Mock） |
| `ICosService.java` / `CosServiceImpl.java` | C04 | COS文件上传 |
| `IApiLogService.java` / `ApiLogServiceImpl.java` | — | API日志记录 |

## 文档 → API 端点 对照

| 文档 | 端点 (数量) | 具体端点 |
|------|-------------|----------|
| C01 - 登录弹层 | 4 | POST auth/wechat-phone-login, POST auth/refresh-token, GET auth/session, POST auth/logout |
| C02 - 设置页 | 2 (planned) | — |
| C03 - 注销须知页 | 2 | GET account/cancel-eligibility, POST account/cancel |
| C04 - 图片预览 | 1 | POST files/book-attachment-images |
| C05 - 通用确认弹窗 | 0 | (UI 组件，无独立 API) |
| C06 - 通用结果反馈 | 0 | (UI 组件，无独立 API) |
| C07 - 空状态及异常状态 | 0 | (UI 组件，无独立 API) |
| C08 - 借阅详情页 | 2 | GET user/borrows/{id}, GET staff/borrows/{id} |
| C09 - 积分详情页 | 2 | GET user/points-records/{id}, GET staff/points-records/{id} |
| C10 - 扫码登录确认页 | 2 | POST staff/member-code/scan, GET staff/members/{id}/overview |
| C11 - 组件预览页 | 0 | (开发者页面，无独立 API) |
| E01 - 员工首页 | 1 | GET staff/home |
| E02 - 借阅列表页 | 3 | GET staff/borrows, GET staff/borrows/{id}, POST staff/borrow-returns |
| E04 - 积分列表页 | 2 | GET staff/points-records, GET staff/points-records/{id} |
| E06 - 微信扫码能力 | 1 | POST staff/member-code/scan |
| E07 - 扫码会员首页 | 1 | GET staff/members/{id}/overview |
| E08 - 办理借阅页 | 1 | POST staff/members/{id}/borrows |
| E09 - 会员借阅记录页 | 1 | GET staff/members/{id}/borrows |
| E10 - 积分操作页 | 3 | GET staff/points-reasons, POST staff/members/{id}/points/add, POST staff/members/{id}/points/deduct |
| E11 - 借阅卡开卡续费页 | 0 | (TODO 规划中) |
| U01 - 用户首页 | 1 | GET user/home |
| U02 - 会员码弹层 | 1 | POST user/member-code |
| U03 - 借阅记录页 | 1 | GET user/borrows |
| U05 - 积分记录页 | 1 | GET user/points-records |
