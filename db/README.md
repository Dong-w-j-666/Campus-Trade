# Campus-Trade MySQL 实验说明

实验三的数据库部分使用 MySQL 8.0/9.x。这里的 SQL 是可在本机执行的实验实现，`schema.sql` 与设计报告中的 13 张表对应；`DatabaseSmokeTest.java` 用于检查连接、表结构、示例数据、联查和事务回滚。

## 文件

| 文件 | 说明 |
|---|---|
| `db/schema.sql` | 创建 `campus_trade`、13 张表、主外键、索引和 CHECK 约束 |
| `db/data.sql` | 导入 5 个用户、6 件商品、2 笔订单及支付、评价、举报和消息 |
| `db/db.properties.example` | JDBC 地址、用户名和密码模板 |
| `src/DatabaseSmokeTest.java` | JDBC 结构和事务校验 |
| `lib/mysql-connector-j-8.0.31.jar` | MySQL JDBC 驱动 |

## 1. 启动 MySQL

确认 MySQL 8.0 或 9.x 已安装并运行。不同系统的启动方式可能不同，可以用以下命令确认服务可连接：

```bash
mysql -u root -p -e "SELECT VERSION();"
```

IDEA 用户也可以新建 MySQL 数据源：主机 `localhost`、端口 `3306`、用户名 `root`，连接成功后再执行 SQL。

## 2. 配置连接

把模板复制为本地配置：

```bash
cp db/db.properties.example db/db.properties
```

编辑 `db/db.properties`，填写本机用户名和密码。该文件已被 `.gitignore` 排除，不会提交到仓库。

## 3. 导入数据库

在仓库根目录执行：

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/data.sql
```

`schema.sql` 会删除并重建 `campus_trade` 中的同名表。不要对包含正式数据的数据库执行该脚本。

导入后应看到 13 张表，包括 `t_user`、`t_role`、`t_permission`、`t_role_permission`、`t_category`、`t_goods`、`t_goods_image`、`t_order`、`t_payment`、`t_evaluation`、`t_report`、`t_message` 和 `t_operation_log`。

## 4. 运行数据库校验

在仓库根目录编译：

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  -cp lib/mysql-connector-j-8.0.31.jar \
  src/DatabaseSmokeTest.java
```

运行：

```bash
java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest
```

Windows 命令提示符使用分号分隔类路径：

```bat
java -cp "out/production/Campus-Trade;lib/mysql-connector-j-8.0.31.jar" DatabaseSmokeTest
```

也可以直接通过参数提供连接信息：

```bash
java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest \
  "jdbc:mysql://127.0.0.1:3306/campus_trade?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  root "你的密码"
```

预期结果：

```text
[通过] 共 13 张数据表
[通过] 外键约束已建立（≥10 个）
[通过] 支付退款结算字段完整
[通过] 双方评价联合唯一键
[通过] 示例数据已导入
[通过] 联查返回 5 条在售商品
[通过] 写入与回滚正常（事务可用）
校验结果：通过 7 项，失败 0 项，数据库结构正常
```

## 表与设计报告的对应

| 设计对象 | 实际数据表 |
|---|---|
| 用户 | `t_user` |
| 角色、权限及关联 | `t_role`、`t_permission`、`t_role_permission` |
| 商品分类 | `t_category` |
| 商品与商品图片 | `t_goods`、`t_goods_image` |
| 订单 | `t_order` |
| 支付、退款与结算流水 | `t_payment` |
| 评价 | `t_evaluation` |
| 举报 | `t_report` |
| 消息 | `t_message` |
| 操作日志 | `t_operation_log` |

## 已知限制

`t_evaluation` 已使用 `(order_id, evaluator_id)` 唯一键，示例数据也包含买卖双方各一条评价；仍需用真实接口验证重复评价和并发提交。`t_payment` 已保留退款金额、退款状态、卖家应结金额和结算状态，但当前只是沙箱和数据库设计，不接入真实资金通道。

`DatabaseSmokeTest` 只验证基础结构、示例联查和事务回滚；真实并发锁、支付回调、退款和跨表业务流程仍需等 Spring Boot 服务实现后进行集成测试。
