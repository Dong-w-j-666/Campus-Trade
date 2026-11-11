-- ============================================================
-- 校园二手交易平台 数据库建表脚本
-- 对应《实验三 软件设计实验报告》表 2-1，共 13 张表
-- 适用：MySQL 8.0 / 9.x，字符集 utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_trade
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_trade;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS t_operation_log;
DROP TABLE IF EXISTS t_message;
DROP TABLE IF EXISTS t_report;
DROP TABLE IF EXISTS t_evaluation;
DROP TABLE IF EXISTS t_payment;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_goods_image;
DROP TABLE IF EXISTS t_goods;
DROP TABLE IF EXISTS t_category;
DROP TABLE IF EXISTS t_role_permission;
DROP TABLE IF EXISTS t_permission;
DROP TABLE IF EXISTS t_role;
DROP TABLE IF EXISTS t_user;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 用户表（由 E-R 图实体“用户”导出）
CREATE TABLE t_user (
  user_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  student_no    VARCHAR(20)  NOT NULL COMMENT '学号（8 位）',
  real_name     VARCHAR(50)  NOT NULL COMMENT '姓名',
  phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
  password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
  credit_score  DECIMAL(5,2) NOT NULL DEFAULT 80.00 COMMENT '信用分 0-100',
  role_code     VARCHAR(20)  NOT NULL DEFAULT 'BUYER' COMMENT '角色编码',
  status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/FROZEN',
  certified     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否实名认证',
  campus        VARCHAR(50)           DEFAULT NULL COMMENT '默认校区',
  avatar_url    VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_student_no (student_no),
  KEY idx_user_role (role_code),
  CONSTRAINT chk_user_credit CHECK (credit_score BETWEEN 0 AND 100)
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 角色表
CREATE TABLE t_role (
  role_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code   VARCHAR(20) NOT NULL COMMENT '角色编码',
  role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(200)         DEFAULT NULL COMMENT '角色说明',
  PRIMARY KEY (role_id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

-- 3. 权限表
CREATE TABLE t_permission (
  permission_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  perm_code     VARCHAR(50) NOT NULL COMMENT '权限编码，如 goods:audit',
  perm_name     VARCHAR(50) NOT NULL COMMENT '权限名称',
  perm_type     VARCHAR(20) NOT NULL COMMENT 'MENU/BUTTON/API',
  PRIMARY KEY (permission_id),
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB COMMENT='权限表';

-- 4. 角色权限表（角色与权限的多对多联系）
CREATE TABLE t_role_permission (
  role_id       BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (role_id, permission_id),
  KEY idx_rp_perm (permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES t_role (role_id) ON DELETE CASCADE,
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES t_permission (permission_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- 5. 商品分类表
CREATE TABLE t_category (
  category_id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  parent_id     BIGINT               DEFAULT NULL COMMENT '父分类ID',
  category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
  sort_no       INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  status        VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  PRIMARY KEY (category_id),
  KEY idx_category_parent (parent_id),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES t_category (category_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='商品分类表';

-- 6. 商品表
CREATE TABLE t_goods (
  goods_id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  title           VARCHAR(60)   NOT NULL COMMENT '标题，最多 30 个汉字',
  description     TEXT                  DEFAULT NULL COMMENT '描述',
  price           DECIMAL(10,2) NOT NULL COMMENT '价格',
  condition_level VARCHAR(20)   NOT NULL DEFAULT '九成新' COMMENT '成色',
  category_id     BIGINT        NOT NULL COMMENT '分类ID',
  seller_id       BIGINT        NOT NULL COMMENT '卖家ID',
  campus          VARCHAR(50)            DEFAULT NULL COMMENT '交易校区',
  status          VARCHAR(20)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/LOCKED/SOLD/OFF_SHELF/PENDING',
  view_count      INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (goods_id),
  KEY idx_goods_status_category (status, category_id),
  KEY idx_goods_seller (seller_id),
  KEY idx_goods_price (price),
  CONSTRAINT fk_goods_category FOREIGN KEY (category_id) REFERENCES t_category (category_id),
  CONSTRAINT fk_goods_seller FOREIGN KEY (seller_id) REFERENCES t_user (user_id),
  CONSTRAINT chk_goods_price CHECK (price > 0)
) ENGINE=InnoDB COMMENT='商品表';

-- 7. 商品图片表（商品“图片”多值属性）
CREATE TABLE t_goods_image (
  image_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  goods_id  BIGINT       NOT NULL COMMENT '商品ID',
  image_url VARCHAR(255) NOT NULL COMMENT '图片地址',
  sort_no   INT          NOT NULL DEFAULT 1 COMMENT '显示顺序 1-9',
  PRIMARY KEY (image_id),
  KEY idx_image_goods (goods_id),
  CONSTRAINT fk_image_goods FOREIGN KEY (goods_id) REFERENCES t_goods (goods_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='商品图片表';

-- 8. 订单表
CREATE TABLE t_order (
  order_id     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  order_no     VARCHAR(32)   NOT NULL COMMENT '订单号',
  goods_id     BIGINT        NOT NULL COMMENT '商品ID',
  buyer_id     BIGINT        NOT NULL COMMENT '买家ID',
  seller_id    BIGINT        NOT NULL COMMENT '卖家ID',
  amount       DECIMAL(10,2) NOT NULL COMMENT '订单金额',
  status       VARCHAR(20)   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/SHIPPED/FINISHED/CANCELLED',
  trade_campus VARCHAR(50)            DEFAULT NULL COMMENT '交易校区',
  remark       VARCHAR(255)           DEFAULT NULL COMMENT '备注',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at      DATETIME               DEFAULT NULL,
  finished_at  DATETIME               DEFAULT NULL,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_buyer (buyer_id, status),
  KEY idx_order_goods (goods_id),
  CONSTRAINT fk_order_goods FOREIGN KEY (goods_id) REFERENCES t_goods (goods_id),
  CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB COMMENT='订单表';

-- 9. 支付、退款与结算流水表（与订单 1:1）
CREATE TABLE t_payment (
  payment_id    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  trade_no      VARCHAR(64)   NOT NULL COMMENT '支付流水号',
  order_id      BIGINT        NOT NULL COMMENT '订单ID',
  amount        DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_method    VARCHAR(20)   NOT NULL DEFAULT 'SANDBOX' COMMENT '支付方式',
  status        VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED/REFUNDED',
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  refund_status VARCHAR(20)   NOT NULL DEFAULT 'NONE' COMMENT 'NONE/REQUESTED/APPROVED/REFUNDED/REJECTED',
  refund_reason VARCHAR(255)           DEFAULT NULL COMMENT '退款原因',
  settlement_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '卖家应结金额',
  settlement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED',
  paid_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  refunded_at   DATETIME               DEFAULT NULL COMMENT '退款完成时间',
  settled_at    DATETIME               DEFAULT NULL COMMENT '结算完成时间',
  PRIMARY KEY (payment_id),
  UNIQUE KEY uk_payment_trade_no (trade_no),
  UNIQUE KEY uk_payment_order (order_id),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES t_order (order_id),
  CONSTRAINT chk_payment_refund CHECK (refund_amount >= 0 AND refund_amount <= amount),
  CONSTRAINT chk_payment_settlement CHECK (settlement_amount >= 0)
) ENGINE=InnoDB COMMENT='支付退款结算流水表';

-- 10. 评价表（同一订单可由买卖双方各评价一次）
CREATE TABLE t_evaluation (
  evaluation_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  order_id      BIGINT       NOT NULL COMMENT '订单ID',
  evaluator_id  BIGINT       NOT NULL COMMENT '评价人ID',
  target_id     BIGINT       NOT NULL COMMENT '被评价人ID',
  score         TINYINT      NOT NULL COMMENT '评分 1-5',
  content       VARCHAR(500)          DEFAULT NULL COMMENT '评价内容',
  tags          VARCHAR(100)          DEFAULT NULL COMMENT '评价标签',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (evaluation_id),
  UNIQUE KEY uk_eval_order_evaluator (order_id, evaluator_id),
  CONSTRAINT fk_eval_order FOREIGN KEY (order_id) REFERENCES t_order (order_id),
  CONSTRAINT chk_eval_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB COMMENT='评价表';

-- 11. 举报表
CREATE TABLE t_report (
  report_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  goods_id    BIGINT       NOT NULL COMMENT '被举报商品ID',
  reporter_id BIGINT       NOT NULL COMMENT '举报人ID',
  report_type VARCHAR(30)  NOT NULL COMMENT '违禁品/虚假描述/恶意骚扰等',
  evidence    VARCHAR(500)          DEFAULT NULL COMMENT '证据说明',
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASSED/REJECTED',
  priority    TINYINT      NOT NULL DEFAULT 1 COMMENT '1 普通，2 高',
  handled_by  BIGINT                DEFAULT NULL COMMENT '处理人ID',
  handled_at  DATETIME              DEFAULT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (report_id),
  KEY idx_report_goods (goods_id),
  KEY idx_report_status (status, priority),
  CONSTRAINT fk_report_goods FOREIGN KEY (goods_id) REFERENCES t_goods (goods_id),
  CONSTRAINT fk_report_user FOREIGN KEY (reporter_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB COMMENT='举报表';

-- 12. 消息表
CREATE TABLE t_message (
  message_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  sender_id   BIGINT       NOT NULL COMMENT '发送者ID',
  receiver_id BIGINT       NOT NULL COMMENT '接收者ID',
  order_id    BIGINT                DEFAULT NULL COMMENT '关联订单ID',
  msg_type    VARCHAR(20)  NOT NULL DEFAULT 'CHAT' COMMENT 'CHAT/SYSTEM',
  content     VARCHAR(500) NOT NULL COMMENT '消息内容',
  is_read     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0 未读，1 已读',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id),
  KEY idx_msg_receiver (receiver_id, is_read),
  CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB COMMENT='消息表';

-- 13. 操作日志表
CREATE TABLE t_operation_log (
  log_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  operator_id BIGINT                DEFAULT NULL COMMENT '操作人ID',
  op_type     VARCHAR(50)  NOT NULL COMMENT '操作类型',
  target_type VARCHAR(30)           DEFAULT NULL COMMENT '操作对象类型',
  target_id   BIGINT                DEFAULT NULL COMMENT '操作对象ID',
  detail      VARCHAR(500)          DEFAULT NULL COMMENT '操作详情',
  ip          VARCHAR(45)           DEFAULT NULL COMMENT '操作 IP',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_log_operator (operator_id),
  KEY idx_log_time (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';
