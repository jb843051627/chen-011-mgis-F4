-- ----------------------------------------------------------------------------
-- 作业范围标签建档卡（资质监督处规矩落库）
--
-- 口径一句话：一张卡只讲一段范围。卡号由系统给号；同一单位同一段范围想再立
-- 一张，第二张顶回去；市局抄送与单位自报常常是同一件事，两条并成一条，账上
-- 不许留两笔。卡必须有出处：立卡先按资质单位编号对基础档案，只按名称匹配不
-- 作数；档案已注销的不再收新卡，老卡照旧查得到、但不能从老卡上接着添东西。
-- 专业类别按现行目录走成一棵树，卡上只许挂到末级；层级档不许人手填，由系统
-- 拿挂用类别数去撞档案上的三道顶线折算，留两位小数，类别有增减、单位改了属
-- 地，翻账时当场重新折算。
--
-- 老库注意：t_mgis_scope_card 的 2026-09-12 自动生成骨架从未启用、账上无卡，
-- 本脚本直接 DROP 重建（列语义与旧骨架不同，旧 qty 为 decimal、旧 grade_level
-- 为手填整数，均不作数）；t_mgis_unit_doc 只补三列，旧数据不动。
-- ----------------------------------------------------------------------------

-- 专业类别目录树：一级是大类，二级才是具体作业类别；卡上只许挂到末级。
-- 废止旧类别留 status=REVOKED 并指着并到哪里，挂旧类别的卡一律不给落。
CREATE TABLE IF NOT EXISTS `t_mgis_prof_category` (
  `id`           BIGINT       NOT NULL COMMENT '主键',
  `cat_code`     VARCHAR(32)  NOT NULL COMMENT '类别代号(大类G01..G10,末级G0101..)',
  `cat_name`     VARCHAR(128) NOT NULL COMMENT '类别名称',
  `parent_code`  VARCHAR(32)  NULL COMMENT '父级代号(一级大类为空;末级指大类)',
  `cat_level`    INT          NOT NULL COMMENT '层级 1大类 2具体作业类别(末级)',
  `leaf_flag`    INT          NOT NULL DEFAULT 0 COMMENT '是否末级 0否 1是(卡上只许挂末级)',
  `cat_status`   VARCHAR(16)  NOT NULL DEFAULT 'CURRENT' COMMENT '情形 CURRENT现行 REVOKED已废止(早已并进别处)',
  `merged_into`  VARCHAR(32)  NULL COMMENT '废止后并入的现行末级代号(挂旧类别不给落,指它去哪)',
  `del_flag`     INT          NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  `create_by`    VARCHAR(64)  NULL,
  `create_time`  DATETIME     NULL,
  `update_by`    VARCHAR(64)  NULL,
  `update_time`  DATETIME     NULL,
  `remark`       VARCHAR(500) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prof_category_code` (`cat_code`),
  KEY `idx_prof_category_parent` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测绘专业类别目录树(现行目录,废止类别留并入去向)';

-- 范围卡：一张卡只讲一段范围；同一单位同一签名(同一段范围)只许一张
DROP TABLE IF EXISTS `t_mgis_scope_card`;
CREATE TABLE `t_mgis_scope_card` (
  `id`                  BIGINT       NOT NULL COMMENT '主键',
  `card_no`             VARCHAR(48)  NOT NULL COMMENT '标签卡号(系统给号,FW+雪花;不劳人手)',
  `unit_id`             BIGINT       NOT NULL COMMENT '资质单位基础档案主键(备查;对档案只认编号)',
  `unit_no`             VARCHAR(64)  NOT NULL COMMENT '资质单位编号(立卡按此对档案,同名不作数)',
  `scope_sign`          VARCHAR(3000) NOT NULL COMMENT '范围签名:本卡末级类别代号升序逗号拼接(同单位同签名第二张顶回)',
  `scope_hash`          CHAR(64)      NOT NULL COMMENT '范围签名SHA-256(定长;唯一约束押它,免得长签名越过索引长度)',
  `scope_count`         INT          NOT NULL COMMENT '挂用类别数(项,正整数;即末级类别条数;负数/零/超三档顶线当场拦)',
  `new_category_count`  INT          NOT NULL DEFAULT 0 COMMENT '本年度新增挂用类别数(立卡时与该单位老卡比对,系统算出,不许手填)',
  `registered_year`     INT          NOT NULL COMMENT '立卡年度(新增类别按本年度口径比对)',
  `grade_level`         INT          NOT NULL COMMENT '层级档 1/2/3(系统撞档案三道顶线折算;等号按高一档;翻账按最新档案重算)',
  `grade_score`         DECIMAL(6,2) NOT NULL COMMENT '层级档系数=挂用类别数÷所在档顶线,两位小数(系统折算,不许人手填)',
  `status`              INT          NOT NULL DEFAULT 0 COMMENT '手续进展 0待核对 1已核对 2已定档(只由经手手续推进)',
  `create_by`           VARCHAR(64)  NULL COMMENT '创建人:按下立卡那一下是谁就是谁,整张卡重录也不许挪',
  `create_time`         DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scope_card_no` (`card_no`),
  UNIQUE KEY `uk_scope_card_scope` (`unit_no`, `scope_hash`),
  KEY `idx_scope_card_unit` (`unit_no`),
  KEY `idx_scope_card_year_new` (`registered_year`, `new_category_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业范围标签建档卡(只立不改;手续进展由服务写,无update_by/del_flag)';

-- 卡上挂的末级类别：只许挂末级，一条一项；类别有增减翻账即凭此表现
CREATE TABLE IF NOT EXISTS `t_mgis_scope_card_category` (
  `id`           BIGINT      NOT NULL COMMENT '主键',
  `card_id`      BIGINT      NOT NULL COMMENT '范围卡主键',
  `unit_no`      VARCHAR(64) NOT NULL COMMENT '资质单位编号(冗余,按单位比对新旧类别用)',
  `cat_code`     VARCHAR(32) NOT NULL COMMENT '末级专业类别代号(现行目录;废止/大类不给落)',
  `create_by`    VARCHAR(64)  NULL,
  `create_time`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scope_card_cat` (`card_id`, `cat_code`),
  KEY `idx_scope_cat_unit` (`unit_no`, `cat_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范围卡挂用类别明细(只增不改)';

-- 出处：市局抄送与单位自报常是同一件事，并在同一张卡上，账上不许留两笔
CREATE TABLE IF NOT EXISTS `t_mgis_scope_source` (
  `id`             BIGINT      NOT NULL COMMENT '主键',
  `card_id`        BIGINT      NOT NULL COMMENT '范围卡主键',
  `source_type`    VARCHAR(16) NOT NULL COMMENT '出处 CITY_COPY市局抄送 SELF_REPORT单位自报',
  `source_doc_no`  VARCHAR(64) NOT NULL COMMENT '来文/自报单号(一径由人工填写,系统不代拟)',
  `create_by`      VARCHAR(64)  NULL,
  `create_time`    DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scope_source_type` (`card_id`, `source_type`),
  KEY `idx_scope_source_card` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范围卡出处(抄送/自报并一卡;单号人工填写)';

-- 经手人：每过一道手续记一条，只增不改；与卡上创建人是两个位置，不许互相顶
CREATE TABLE IF NOT EXISTS `t_mgis_scope_handler` (
  `id`           BIGINT       NOT NULL COMMENT '主键',
  `card_id`      BIGINT       NOT NULL COMMENT '范围卡主键',
  `seq`          INT          NOT NULL COMMENT '卡内手续序号(立卡为1,逐道加一)',
  `action`       VARCHAR(16)  NOT NULL COMMENT '手续 REGISTER立卡 CHECK核对 CONFIRM定档',
  `handler`      VARCHAR(64)  NOT NULL COMMENT '经手人(落笔那一下自登录态取,请求代报不算)',
  `handled_at`   DATETIME     NOT NULL COMMENT '落笔时辰(服务层写死,精确到秒)',
  `note`         VARCHAR(255) NULL COMMENT '附言(可空)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scope_handler_seq` (`card_id`, `seq`),
  UNIQUE KEY `uk_scope_handler_point` (`card_id`, `action`),
  KEY `idx_scope_handler_card` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范围卡经手手续链(每道手续一条,只增不改)';

-- ----------------------------------------------------------------------------
-- 基础档案补属地三列：单位改了属地，层级档翻账时按新档案重新折算。
-- 老库执行下面三句（新库本脚本已建）；旧 road_name 全串保留不动。
-- ALTER TABLE `t_mgis_unit_doc` ADD COLUMN `region_province` VARCHAR(64) NULL COMMENT '注册地省' AFTER `road_name`;
-- ALTER TABLE `t_mgis_unit_doc` ADD COLUMN `region_city`     VARCHAR(64) NULL COMMENT '注册地市(属地列表按此归堆)' AFTER `region_province`;
-- ALTER TABLE `t_mgis_unit_doc` ADD COLUMN `region_county`   VARCHAR(64) NULL COMMENT '注册地区县(最末一级归堆)' AFTER `region_city`;
-- 旧档案回填（按 road_name 全串拆填）：
-- UPDATE `t_mgis_unit_doc` SET region_province='江南省', region_city='临川市', region_county='城东区' WHERE id=0;
-- UPDATE `t_mgis_unit_doc` SET region_province='江南省', region_city='临川市', region_county='城西区' WHERE id=1;
-- ----------------------------------------------------------------------------

-- 种子档案（与 doc/schema/mgis.sql 两条种子对齐：id=0 在册 / id=1 已注销）。
-- ON DUPLICATE 保证：基线已插过这两条的新库，跑本脚本也把属地三列补齐。
INSERT INTO `t_mgis_unit_doc` (`id`, `site_no`, `site_name`, `site_type`, `road_name`,
                               `region_province`, `region_city`, `region_county`,
                               `th1_max`, `th2_max`, `th3_max`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES (0, 'ZZ00', '江南省测绘院第一地理信息工程院基础档案', '事业单位', '江南省—临川市—城东区',
        '江南省', '临川市', '城东区', 30.00, 120.00, 360.00, 0, 0, 'seed', NOW()),
       (1, 'ZZ01', '旧临川市城乡测绘服务中心基础档案（已注销）', '企业单位', '江南省—临川市—城西区',
        '江南省', '临川市', '城西区', 30.00, 120.00, 360.00, 1, 0, 'seed', NOW())
ON DUPLICATE KEY UPDATE
  `region_province` = VALUES(`region_province`),
  `region_city`     = VALUES(`region_city`),
  `region_county`   = VALUES(`region_county`);

-- ----------------------------------------------------------------------------
-- 现行测绘资质专业类别目录（十类大类；二级为具体作业类别，即末级）。
-- 种子两条同名不同编号的风险由 unit_no 兜，与类别无关；废止旧类别见末尾。
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `t_mgis_prof_category` (`id`, `cat_code`, `cat_name`, `parent_code`, `cat_level`, `leaf_flag`, `cat_status`, `create_by`, `create_time`) VALUES
-- 一、大地测量
(1010, 'G01', '大地测量', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1011, 'G0101', '卫星定位测量', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1012, 'G0102', '卫星导航定位基准站网建设、运维', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1013, 'G0103', '水准测量', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1014, 'G0104', '三角测量', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1015, 'G0105', '天文测量', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1016, 'G0106', '重力测量', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
(1017, 'G0107', '大地测量数据处理', 'G01', 2, 1, 'CURRENT', 'seed', NOW()),
-- 二、测绘航空摄影
(1020, 'G02', '测绘航空摄影', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1021, 'G0201', '有人驾驶航空器航空摄影', 'G02', 2, 1, 'CURRENT', 'seed', NOW()),
(1022, 'G0202', '无人驾驶航空器航空摄影', 'G02', 2, 1, 'CURRENT', 'seed', NOW()),
(1023, 'G0203', '机载激光雷达', 'G02', 2, 1, 'CURRENT', 'seed', NOW()),
(1024, 'G0204', '机载合成孔径雷达（SAR）', 'G02', 2, 1, 'CURRENT', 'seed', NOW()),
-- 三、摄影测量与遥感
(1030, 'G03', '摄影测量与遥感', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1031, 'G0301', '卫星遥感影像处理', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1032, 'G0302', '航空摄影测量', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1033, 'G0303', '激光扫描', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1034, 'G0304', '多光谱遥感', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1035, 'G0305', '地表模型生产', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1036, 'G0306', '摄影测量与遥感外业', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
(1037, 'G0307', '摄影测量与遥感内业', 'G03', 2, 1, 'CURRENT', 'seed', NOW()),
-- 四、工程测量（去年被抽中那家合同写界线与不动产，作业范围却只找得到这一类）
(1040, 'G04', '工程测量', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1041, 'G0401', '控制测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1042, 'G0402', '地形测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1043, 'G0403', '规划测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1044, 'G0404', '建筑工程测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1045, 'G0405', '变形形变与精密测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1046, 'G0406', '市政工程测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1047, 'G0407', '水利工程测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1048, 'G0408', '线路与桥隧测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(1049, 'G0409', '地下管线测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(10410, 'G0410', '矿山测量', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
(10411, 'G0411', '工程测量监理', 'G04', 2, 1, 'CURRENT', 'seed', NOW()),
-- 五、海洋测绘
(1050, 'G05', '海洋测绘', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1051, 'G0501', '海域权属测绘', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1052, 'G0502', '海岸地形测量', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1053, 'G0503', '水深测量', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1054, 'G0504', '水文观测', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1055, 'G0505', '海洋工程测量', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1056, 'G0506', '扫海测量', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1057, 'G0507', '深度基准测量', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1058, 'G0508', '海图编制', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
(1059, 'G0509', '海洋测绘监理', 'G05', 2, 1, 'CURRENT', 'seed', NOW()),
-- 六、界线与不动产测绘
(1060, 'G06', '界线与不动产测绘', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1061, 'G0601', '地籍测绘', 'G06', 2, 1, 'CURRENT', 'seed', NOW()),
(1062, 'G0602', '房产测绘', 'G06', 2, 1, 'CURRENT', 'seed', NOW()),
(1063, 'G0603', '界线测绘', 'G06', 2, 1, 'CURRENT', 'seed', NOW()),
(1064, 'G0604', '不动产测绘监理', 'G06', 2, 1, 'CURRENT', 'seed', NOW()),
-- 七、地理信息系统工程
(1070, 'G07', '地理信息系统工程', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1071, 'G0701', '地理信息数据采集', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
(1072, 'G0702', '地理信息数据处理', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
(1073, 'G0703', '地理信息数据及数据库服务', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
(1074, 'G0704', '地面移动测量', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
(1075, 'G0705', '地理信息软件开发', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
(1076, 'G0706', '地理信息系统工程监理', 'G07', 2, 1, 'CURRENT', 'seed', NOW()),
-- 八、地图编制
(1080, 'G08', '地图编制', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1081, 'G0801', '地形图编制', 'G08', 2, 1, 'CURRENT', 'seed', NOW()),
(1082, 'G0802', '教学地图编制', 'G08', 2, 1, 'CURRENT', 'seed', NOW()),
(1083, 'G0803', '电子地图编制', 'G08', 2, 1, 'CURRENT', 'seed', NOW()),
(1084, 'G0804', '真三维地图编制', 'G08', 2, 1, 'CURRENT', 'seed', NOW()),
(1085, 'G0805', '其他专用地图编制', 'G08', 2, 1, 'CURRENT', 'seed', NOW()),
-- 九、导航电子地图制作
(1090, 'G09', '导航电子地图制作', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1091, 'G0901', '导航电子地图制作', 'G09', 2, 1, 'CURRENT', 'seed', NOW()),
-- 十、互联网地图服务
(1100, 'G10', '互联网地图服务', NULL, 1, 0, 'CURRENT', 'seed', NOW()),
(1101, 'G1001', '地理位置定位', 'G10', 2, 1, 'CURRENT', 'seed', NOW()),
(1102, 'G1002', '地理信息上传标注', 'G10', 2, 1, 'CURRENT', 'seed', NOW()),
(1103, 'G1003', '地图数据库开发', 'G10', 2, 1, 'CURRENT', 'seed', NOW()),
-- 旧目录里早已并进别处的类别：挂这些不给落，只指它去哪
(9001, 'OLD_BOUNDARY', '行政区域界线测绘（旧目录）', NULL, 2, 1, 'REVOKED', 'seed', NOW()),
(9002, 'OLD_DIGITAL', '数字化地形测量（旧目录）', NULL, 2, 1, 'REVOKED', 'seed', NOW());

UPDATE `t_mgis_prof_category` SET `merged_into` = 'G0603' WHERE `cat_code` = 'OLD_BOUNDARY';
UPDATE `t_mgis_prof_category` SET `merged_into` = 'G0402' WHERE `cat_code` = 'OLD_DIGITAL';
