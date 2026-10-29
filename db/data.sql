-- ============================================================
-- 校园二手交易平台 示例数据
-- 依赖 schema.sql 先执行
-- ============================================================

USE campus_trade;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE t_operation_log;
TRUNCATE TABLE t_message;
TRUNCATE TABLE t_report;
TRUNCATE TABLE t_evaluation;
TRUNCATE TABLE t_payment;
TRUNCATE TABLE t_order;
TRUNCATE TABLE t_goods_image;
TRUNCATE TABLE t_goods;
TRUNCATE TABLE t_category;
TRUNCATE TABLE t_role_permission;
TRUNCATE TABLE t_permission;
TRUNCATE TABLE t_role;
TRUNCATE TABLE t_user;
SET FOREIGN_KEY_CHECKS = 1;

-- 用户：1 管理员 + 2 卖家 + 2 买家
INSERT INTO t_user (user_id, student_no, real_name, phone, password_hash, credit_score, role_code, status, certified, campus) VALUES
(1, '20240001', '董文杰', '13800000001', 'demo-hash-1', 88.00, 'ADMIN',  'ACTIVE', 1, '主校区'),
(2, '20240002', '李晓明', '13800000002', 'demo-hash-2', 92.00, 'SELLER', 'ACTIVE', 1, '主校区'),
(3, '20240003', '王雪',   '13800000003', 'demo-hash-3', 85.00, 'SELLER', 'ACTIVE', 1, '东校区'),
(4, '20240004', '陈浩',   '13800000004', 'demo-hash-4', 80.00, 'BUYER',  'ACTIVE', 1, '主校区'),
(5, '20240005', '刘佳',   '13800000005', 'demo-hash-5', 78.00, 'BUYER',  'PENDING', 0, '东校区');

-- 角色与权限
INSERT INTO t_role (role_id, role_code, role_name, description) VALUES
(1, 'ADMIN',  '平台管理员', '拥有全部权限'),
(2, 'AUDITOR','内容审核员', '审核商品与举报'),
(3, 'SELLER', '卖家',       '发布与管理商品'),
(4, 'BUYER',  '买家',       '浏览、下单与评价');

INSERT INTO t_permission (permission_id, perm_code, perm_name, perm_type) VALUES
(1, 'goods:audit',    '审核商品',   'BUTTON'),
(2, 'report:handle',  '处理举报',   'BUTTON'),
(3, 'category:manage','分类维护',   'BUTTON'),
(4, 'user:manage',    '用户管理',   'BUTTON'),
(5, 'stat:view',      '查看统计',   'MENU'),
(6, 'order:create',   '提交订单',   'API'),
(7, 'goods:publish',  '发布商品',   'API');

INSERT INTO t_role_permission (role_id, permission_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
(2,1),(2,2),(2,5),
(3,7),(3,6),(3,5),
(4,6),(4,5);

-- 商品分类
INSERT INTO t_category (category_id, parent_id, category_name, sort_no, status) VALUES
(1, NULL, '教材书籍', 1, 'ENABLED'),
(2, NULL, '数码产品', 2, 'ENABLED'),
(3, NULL, '生活用品', 3, 'ENABLED'),
(4, NULL, '运动户外', 4, 'ENABLED'),
(5, 2,    '手机平板', 1, 'ENABLED');

-- 商品
INSERT INTO t_goods (goods_id, title, description, price, condition_level, category_id, seller_id, campus, status, view_count) VALUES
(101, '高等数学第七版 上册', '无笔记，九成新，考研可用', 25.00, '九成新', 1, 2, '主校区', 'ON_SALE', 36),
(102, '罗技蓝牙键盘 K380',   '使用半年，功能正常，含包装', 120.00, '八成新', 2, 2, '主校区', 'ON_SALE', 58),
(103, '宿舍折叠桌',           '桌面无划痕，可自提',         45.00, '九成新', 3, 3, '东校区', 'ON_SALE', 12),
(104, 'iPad 第九代 64G',      '送保护壳，电池健康 92%',     1500.00,'九五新', 5, 3, '东校区', 'ON_SALE', 210),
(105, '羽毛球拍一对',         '闲置，适合入门',             60.00, '八成新', 4, 2, '主校区', 'OFF_SHELF', 8),
(106, '大学英语四级真题',     '只写过两套',                 10.00, '九成新', 1, 3, '东校区', 'ON_SALE', 21);

INSERT INTO t_goods_image (goods_id, image_url, sort_no) VALUES
(101, '/images/goods/101-1.jpg', 1),
(101, '/images/goods/101-2.jpg', 2),
(102, '/images/goods/102-1.jpg', 1),
(104, '/images/goods/104-1.jpg', 1);

-- 订单：一笔已完成、一笔待付款
INSERT INTO t_order (order_id, order_no, goods_id, buyer_id, seller_id, amount, status, trade_campus, paid_at, finished_at) VALUES
(1001, 'CT202610150001', 101, 4, 2, 25.00,  'FINISHED', '主校区', '2026-10-15 10:05:00', '2026-10-15 18:00:00'),
(1002, 'CT202610160002', 102, 5, 2, 120.00, 'UNPAID',   '主校区', NULL, NULL);

INSERT INTO t_payment (trade_no, order_id, amount, pay_method, status, refund_amount, paid_at) VALUES
('SB202610150001', 1001, 25.00, 'SANDBOX', 'SUCCESS', 0.00, '2026-10-15 10:05:00');

INSERT INTO t_evaluation (order_id, evaluator_id, target_id, score, content, tags) VALUES
(1001, 4, 2, 5, '书很新，人也很好，约在主楼交易很顺利', '描述相符,交易爽快');

INSERT INTO t_report (goods_id, reporter_id, report_type, evidence, status, priority) VALUES
(105, 4, '违禁品', '商品描述与实物不符', 'REJECTED', 1);

INSERT INTO t_message (sender_id, receiver_id, order_id, msg_type, content, is_read) VALUES
(4, 2, 1001, 'CHAT',   '书还有吗？可以明天中午交易吗', 1),
(2, 4, 1001, 'CHAT',   '在的，明天 12 点主楼门口可以', 1),
(2, 5, 1002, 'CHAT',   '键盘还在，可以小刀', 0),
(1, 4, NULL, 'SYSTEM', '您的实名认证已通过', 1);

INSERT INTO t_operation_log (operator_id, op_type, target_type, target_id, detail, ip) VALUES
(1, 'AUDIT_REPORT', 'REPORT', 1, '举报不成立，商品保持原状态', '127.0.0.1'),
(1, 'CATEGORY_UPDATE', 'CATEGORY', 5, '新增子分类：手机平板', '127.0.0.1');
