# Campus-Trade 校园二手交易平台课程实验

本仓库是“2024 级网络工程第 2 组”软件工程实验的阶段成果，包含实验一至实验五的五份报告、十模块核心业务测试程序、MySQL 建表与示例数据，以及数据库结构和事务校验程序。

规划中的完整平台采用 Vue 3、Spring Boot 和 MySQL。当前仓库还没有实现 Vue 前端、Spring Boot 服务或完整的 Web 交易流程；现有 Java 程序是独立运行的实验验证代码。

## 已完成内容

| 实验成果 | 主要内容 |
|---|---|
| 实验一 项目选题与任务分解 | 项目范围、8 人分工、10 个模块、29 项任务与进度计划 |
| 实验二 需求分析 | REQ-01 至 REQ-16、用例模型、Chen E-R 图、分层数据流图和逐条需求落点核对 |
| 实验三 软件设计 | 分层架构、模块接口、订单状态机、14 张表及设计追踪 |
| 实验三 软件测试 | 10 个模块 × 5 个用例，首轮 40/50、回归 50/50 |
| MySQL 数据库实验 | 建表、示例数据、支付退款结算字段、7 项结构、联查和事务校验 |
| 实验五 数据库结构设计 | E-R 转关系模式、14 张表、1NF/2NF/3NF 检查、21 个外键和 MySQL 实测 |

## 技术环境

| 实验部分 | 运行依赖 |
|---|---|
| 核心业务逻辑测试 | JDK 21 或更高版本，仅使用 Java 标准库 |
| MySQL 数据库实验 | MySQL 8.0/9.x、MySQL Connector/J 8.0.31 |
| 实验文档 | Word 或兼容 DOCX 的软件 |
| 后续完整平台 | Vue 3、Spring Boot、MySQL（尚未实现） |

核心测试程序使用内存夹具验证业务规则，不连接数据库；MySQL 实验则必须启动数据库。两种测试不能混为同一种端到端验证。

## 仓库结构

```text
Campus-Trade/
├── src/
│   ├── CampusMarketTest.java     # 10 模块 × 5 用例的核心业务测试
│   └── DatabaseSmokeTest.java    # MySQL 结构、联查和事务校验
├── db/
│   ├── schema.sql                # 14 张业务表及约束
│   ├── data.sql                  # 用户、商品、订单等示例数据
│   ├── db.properties.example     # 数据库连接配置模板
│   └── README.md                 # MySQL 实验操作说明
├── docs/                         # 实验一至实验三五份 DOCX 报告
├── lib/                          # MySQL JDBC 驱动
└── .idea/                        # IntelliJ IDEA 工程配置
```

`out/`、`db/db.properties` 和运行生成的 `测试结果_*.csv` 不进入版本库。

## 核心业务逻辑测试

在仓库根目录执行：

```bash
# 编译
javac -encoding UTF-8 -d out/production/Campus-Trade \
  src/CampusMarketTest.java

# 修复后回归：预期 50/50 通过
java -cp out/production/Campus-Trade CampusMarketTest

# 首轮缺陷复现：预期 40/50 通过
java -cp out/production/Campus-Trade CampusMarketTest --legacy
```

两条命令分别生成 `测试结果_回归.csv` 和 `测试结果_首轮.csv`。10 个失败用例对应 BUG-01 至 BUG-10。

## MySQL 数据库实验

1. 启动 MySQL 8.0/9.x。
2. 复制配置模板并填写本机密码：

```bash
cp db/db.properties.example db/db.properties
```

3. 导入结构和示例数据：

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/data.sql
```

`schema.sql` 会重建 `campus_trade` 中的 14 张表，只应在实验数据库执行。`t_payment` 同时保留支付、退款和卖家结算字段；`t_evaluation` 使用 `(order_id, evaluator_id)` 唯一键，支持买卖双方各评价一次；`t_evaluation_tag` 把评价标签拆成原子记录。订单表不重复保存卖家，查询时通过 `t_order.goods_id → t_goods.seller_id` 关联。

4. 编译并运行数据库校验：

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  -cp lib/mysql-connector-j-8.0.31.jar \
  src/DatabaseSmokeTest.java

java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest
```

预期通过表数量、外键、资金字段、双方评价唯一键、示例数据、商品联查和事务回滚共 7 项检查。详细步骤见 [数据库说明](db/README.md)。

## 实验报告

- [实验一 项目选题与任务分解](<docs/实验一 项目选题与任务分解.docx>)
- [实验二 需求分析](<docs/实验二 需求分析实验报告.docx>)
- [实验三 软件设计](<docs/实验三 软件设计实验报告.docx>)
- [实验三 软件测试](<docs/实验三 软件测试实验报告.docx>)
- [实验五 数据库结构设计](<docs/实验五 数据库结构设计实验报告.docx>)

## 当前限制

- `CampusMarketTest` 验证核心规则、状态和缺陷回归，不等于真实 HTTP 接口、浏览器页面或数据库事务测试。
- MySQL 脚本和 `DatabaseSmokeTest` 已覆盖数据库结构、联查及基础事务，支付与结算目前是沙箱和数据库设计，不接入真实资金。
- 并发下单、重复支付、权限缓存和 200 并发性能目标仍需在完整平台中继续验证。

## 提交与推送

```bash
git add -A
git commit -m "说明本次修改内容"
git push
```

也可以使用仓库中的脚本；提交消息可选，未填写时会自动附加当前时间，避免连续提交显示成相同标题：

```bash
./git-push.sh
./git-push.sh https://github.com/Dong-w-j-666/Campus-Trade.git "补充项目说明"
```
