-- V10: 客户/供应商示例数据
INSERT IGNORE INTO cust_customer (customer_code, customer_name, customer_name_en, customer_type, country, contact_person, contact_email, credit_limit, payment_terms, tax_id, swift_code) VALUES
('CUS001', 'ABC贸易公司', 'ABC Trading Inc.', 'buyer', '美国', 'John Smith', 'john@abctrading.com', 200000.00, 'T/T 30/70', 'US-12345678', 'BOFAUS3N'),
('CUS002', '环球进口有限公司', 'Global Imports LLC', 'buyer', '美国', 'Sarah Johnson', 'sarah@globalimports.com', 150000.00, 'L/C at sight', 'US-87654321', 'CITIUS33'),
('CUS003', '太平洋商品公司', 'Pacific Goods Co.', 'distributor', '日本', 'Tanaka Hiroshi', 'tanaka@pacificgoods.jp', 300000.00, 'T/T 100%', 'JP-98765432', 'SMBCJPJT'),
('CUS004', '欧洲贸易有限公司', 'Euro Trade GmbH', 'agent', '德国', 'Hans Mueller', 'hans@eurotrade.de', 180000.00, 'D/P at sight', 'DE-123456789', 'DEUTDEFF'),
('CUS005', '亚洲合作伙伴', 'Asia Partners Ltd.', 'distributor', '新加坡', 'Lee Wei Ming', 'weiming@asiapartners.sg', 250000.00, 'T/T 30/70', 'SG-1234567A', 'DBSGSG22');

INSERT IGNORE INTO cust_supplier (supplier_code, supplier_name, province, city, contact_person, contact_phone, rating, payment_terms, main_products) VALUES
('SUP001', '深圳华强电子有限公司', '广东', '深圳', '王经理', '13800138001', 5, '月结30天', 'LED灯具、电源适配器'),
('SUP002', '浙江正泰电器股份有限公司', '浙江', '温州', '陈总监', '13900139002', 5, 'T/T 30%预付', '断路器、接触器、配电箱'),
('SUP003', '广东力特电机制造有限公司', '广东', '佛山', '李工', '13700137003', 4, '月结60天', '电机、减速机'),
('SUP004', '江苏阳光纺织集团', '江苏', '无锡', '赵总', '13600136004', 4, 'T/T 50%预付', '纺织品、面料'),
('SUP005', '山东鲁阳机械有限公司', '山东', '济南', '刘经理', '13500135005', 3, '货到付款', '机械设备、零部件'),
('SUP006', '福建安溪茶业进出口公司', '福建', '泉州', '黄女士', '13300133006', 4, 'T/T 100%预付', '茶叶、茶具');
