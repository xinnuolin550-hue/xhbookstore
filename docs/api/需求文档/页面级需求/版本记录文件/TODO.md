# TODO — 待开发事项

> 更新日期: 2026-06-30

## P0 — 紧急

| 模块 | 内容 | 涉及文件 |
|------|------|----------|
| 借书业务 | 建11张表 + Service层 + Controller真实逻辑 | book_* / dd_* |
| 还书业务 | 还书Service + 更新借书明细 + 库存恢复 | book_return_detail |
| 借阅列表 | 关联查询 book_borrow_order + detail + return_detail | StaffController, UserController |

## P1 — 重要

| 模块 | 内容 | 涉及文件 |
|------|------|----------|
| 借转购 | 购书Service + 生成JSDD订单 + 更新借书明细 | dd_book_purchase_order |
| 图书管理 | 图书CRUD + 上架下架 + 库存管理 | book_info, book_image |
| 消耗积分 | OUT类型订单 + 出账单 + 核销关联 | xhbs_points_* |
| 微信对接 | 微信getPhoneNumber + member表关联 | AuthController, WechatServiceImpl |

## P2 — 一般

| 模块 | 内容 | 涉及文件 |
|------|------|----------|
| 员工首页 | 真实统计（今日借阅量/还书量） | StaffController.home() |
| 用户首页 | 真实会员数据关联 | UserController.home() |
| 数量闭环自检 | 每日定时校验SQL | book_borrow_detail |
| 会员码 | 动态二维码生成（时效30秒） | UserController.memberCode() |
