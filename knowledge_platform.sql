/*
 Navicat Premium Dump SQL

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80408 (8.4.8)
 Source Host           : localhost:3306
 Source Schema         : knowledge_platform

 Target Server Type    : MySQL
 Target Server Version : 80408 (8.4.8)
 File Encoding         : 65001

 Date: 20/09/2026 01:38:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for exam_paper
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `duration` int NOT NULL,
  `enabled` bit(1) NOT NULL,
  `pass_score` int NOT NULL,
  `question_count` int NOT NULL,
  `question_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `random_order` bit(1) NOT NULL,
  `show_answer` bit(1) NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `total_score` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_paper
-- ----------------------------
INSERT INTO `exam_paper` VALUES (1, '销售培训', '2026-04-24 14:08:09.413784', '系统管理员', '手麻售前知识基础测试', 60, b'1', 60, 0, NULL, b'1', b'1', '手麻售前知识基础测试', 100, '2026-04-24 14:08:09.413784', '系统管理员');

-- ----------------------------
-- Table structure for exam_question
-- ----------------------------
DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `difficulty` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enabled` bit(1) NOT NULL,
  `explanation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `score` int NOT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `use_count` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_question
-- ----------------------------
INSERT INTO `exam_question` VALUES (1, 'A', '销售培训', '手麻三甲客户有哪些\n', '2026-04-24 14:07:40.733350', '系统管理员', 'easy', b'1', '', '[{\"label\":\"A\",\"content\":\"杭州市人民医院\"},{\"label\":\"B\",\"content\":\"浙江大学附属第一医院\"}]', 5, 'single', 0);

-- ----------------------------
-- Table structure for exam_record
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `correct_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `duration` int NULL DEFAULT NULL,
  `passed` bit(1) NOT NULL,
  `score` int NOT NULL,
  `start_time` datetime(6) NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `submit_time` datetime(6) NULL DEFAULT NULL,
  `total_questions` int NOT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `paper_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK3obao230jstg1j0bmcxdsr55d`(`paper_id` ASC) USING BTREE,
  CONSTRAINT `FK3obao230jstg1j0bmcxdsr55d` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_record
-- ----------------------------

-- ----------------------------
-- Table structure for kp_assessment_record
-- ----------------------------
DROP TABLE IF EXISTS `kp_assessment_record`;
CREATE TABLE `kp_assessment_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assessment_date` datetime(6) NOT NULL,
  `assessment_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `assessor_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `evaluation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `grade` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `training_record_id` bigint NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_assessment_record
-- ----------------------------
INSERT INTO `kp_assessment_record` VALUES (1, '2026-06-02 20:53:00.000000', '模拟售前', '', '2026-06-03 12:53:47.939683', '系统管理员', '手麻产品讲解', 'B', '手麻产品讲解', 1, '2026-06-03 12:53:47.939683', '系统管理员');

-- ----------------------------
-- Table structure for kp_attachment
-- ----------------------------
DROP TABLE IF EXISTS `kp_attachment`;
CREATE TABLE `kp_attachment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stored_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `uploaded_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKnhflpj5rchsql3v4blvd86spe`(`item_id` ASC) USING BTREE,
  CONSTRAINT `FKnhflpj5rchsql3v4blvd86spe` FOREIGN KEY (`item_id`) REFERENCES `kp_knowledge_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_attachment
-- ----------------------------
INSERT INTO `kp_attachment` VALUES (14, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\50a56755-31aa-4232-9e3d-690d3da6cf4f_附件1.调研文档_XX医院.xlsx', 202818, '附件1.调研文档_XX医院.xlsx', '50a56755-31aa-4232-9e3d-690d3da6cf4f_附件1.调研文档_XX医院.xlsx', '2026-05-22 14:32:39.484292', '系统管理员', 9);
INSERT INTO `kp_attachment` VALUES (15, 'video/mp4', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\ec80d54c-388d-4e71-a5fb-e210d222254e_ZING_宣传_1.mp4', 15658107, 'ZING_宣传_1.mp4', 'ec80d54c-388d-4e71-a5fb-e210d222254e_ZING_宣传_1.mp4', '2026-05-25 15:20:06.048240', '系统管理员', 10);
INSERT INTO `kp_attachment` VALUES (17, 'application/pdf', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\2df00dae-1414-4afe-9385-df3779723e32_彩页0522-2.pdf', 28222542, '彩页0522-2.pdf', '2df00dae-1414-4afe-9385-df3779723e32_彩页0522-2.pdf', '2026-05-25 15:22:57.916563', '系统管理员', 10);
INSERT INTO `kp_attachment` VALUES (18, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\17cbbbcd-97d9-4204-99ae-c6ae2935ae4f_fd8eb728a3d19e813dcd970a81bab3c.png', 102317, 'fd8eb728a3d19e813dcd970a81bab3c.png', '17cbbbcd-97d9-4204-99ae-c6ae2935ae4f_fd8eb728a3d19e813dcd970a81bab3c.png', '2026-06-03 17:38:33.038154', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (19, 'application/pdf', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\6016ed6d-b479-4bca-96c3-604f21e2dff8_泽进科技-国产化信创数据库运维边界纪律规范通知-20260605.pdf.pdf', 1330330, '泽进科技-国产化信创数据库运维边界纪律规范通知-20260605.pdf.pdf', '6016ed6d-b479-4bca-96c3-604f21e2dff8_泽进科技-国产化信创数据库运维边界纪律规范通知-20260605.pdf.pdf', '2026-06-05 15:50:46.920793', '系统管理员', 13);
INSERT INTO `kp_attachment` VALUES (21, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\c93ca79f-c087-4293-9712-b369f0772735_场地授权.png', 102317, '场地授权.png', 'c93ca79f-c087-4293-9712-b369f0772735_场地授权.png', '2026-06-12 11:16:46.328606', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (22, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\55728723-8de3-47d6-87cc-a44a7cfcb08e_机器码.png', 253676, '机器码.png', '55728723-8de3-47d6-87cc-a44a7cfcb08e_机器码.png', '2026-06-12 11:30:13.116535', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (23, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\e8e26411-1502-4487-be52-324a2083c8ae_c0cbdd5cfdc49895201c251399384ea.png', 138255, 'c0cbdd5cfdc49895201c251399384ea.png', 'e8e26411-1502-4487-be52-324a2083c8ae_c0cbdd5cfdc49895201c251399384ea.png', '2026-06-12 11:44:02.323876', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (24, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\ce79252b-4d8b-4306-91bb-0cdb58e127aa_个人中心-获取密钥.png', 72654, '个人中心-获取密钥.png', 'ce79252b-4d8b-4306-91bb-0cdb58e127aa_个人中心-获取密钥.png', '2026-06-12 11:45:14.334761', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (25, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\35ca2c86-5997-4d7c-b406-aa9ae600a1f8_MDI激活码.png', 131638, 'MDI激活码.png', '35ca2c86-5997-4d7c-b406-aa9ae600a1f8_MDI激活码.png', '2026-06-12 11:47:27.383644', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (26, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\0d4ef52f-13fe-48f3-861d-83141cfff247_API-激活码.png', 309021, 'API-激活码.png', '0d4ef52f-13fe-48f3-861d-83141cfff247_API-激活码.png', '2026-06-12 12:00:15.097583', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (27, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\65054d32-bd21-4ce2-93a8-48e95bbea7e0_数据源填写.png', 259083, '数据源填写.png', '65054d32-bd21-4ce2-93a8-48e95bbea7e0_数据源填写.png', '2026-06-12 14:07:14.884754', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (28, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\292ec73d-df19-4bc1-ba73-c2f7840890e3_数据源填写.png', 259083, '数据源填写.png', '292ec73d-df19-4bc1-ba73-c2f7840890e3_数据源填写.png', '2026-06-12 14:07:31.317821', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (29, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\20bfaa26-c5f2-4f87-ae73-b8a34bfe8a7c_接口数据源编辑.png', 92243, '接口数据源编辑.png', '20bfaa26-c5f2-4f87-ae73-b8a34bfe8a7c_接口数据源编辑.png', '2026-06-12 14:07:46.505136', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (30, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\b0db4c7f-0d74-4329-ad83-52fad16847a6_接口数据源编辑.png', 92243, '接口数据源编辑.png', 'b0db4c7f-0d74-4329-ad83-52fad16847a6_接口数据源编辑.png', '2026-06-12 14:07:56.165946', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (31, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\51585d22-404c-40d0-9984-63f559c125fb_数据源填写.png', 259083, '数据源填写.png', '51585d22-404c-40d0-9984-63f559c125fb_数据源填写.png', '2026-06-12 14:08:10.485251', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (32, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\5a2c59c8-460c-4528-afcd-8eba2bf6f850_租户管理-填写密钥.png', 208363, '租户管理-填写密钥.png', '5a2c59c8-460c-4528-afcd-8eba2bf6f850_租户管理-填写密钥.png', '2026-06-12 14:12:44.605675', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (33, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\21e44456-e722-4acf-9fbf-9968d938de70_填写密钥.png', 146181, '填写密钥.png', '21e44456-e722-4acf-9fbf-9968d938de70_填写密钥.png', '2026-06-12 14:13:02.767913', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (34, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\e07231ce-2286-4956-8eeb-549b70b29b73_业务系统修改IP.png', 340259, '业务系统修改IP.png', 'e07231ce-2286-4956-8eeb-549b70b29b73_业务系统修改IP.png', '2026-06-12 14:15:22.663715', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (35, 'image/png', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\754c79f1-f43b-40c5-a474-0b1bef05c588_刷新权限.png', 259626, '刷新权限.png', '754c79f1-f43b-40c5-a474-0b1bef05c588_刷新权限.png', '2026-06-12 14:18:35.901669', '系统管理员', 12);
INSERT INTO `kp_attachment` VALUES (36, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\11f51e17-5911-4000-8abc-c4203ca777dc_急诊急救一体化解决方案0506.pptx', 63894513, '急诊急救一体化解决方案0506.pptx', '11f51e17-5911-4000-8abc-c4203ca777dc_急诊急救一体化解决方案0506.pptx', '2026-06-29 15:40:27.866237', '系统管理员', 14);
INSERT INTO `kp_attachment` VALUES (38, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\b0a7cb3e-e97c-46cf-a416-4df36694396b_成本报价-手麻&重症&急诊急救.xlsx', 61436, '成本报价-手麻&重症&急诊急救.xlsx', 'b0a7cb3e-e97c-46cf-a416-4df36694396b_成本报价-手麻&重症&急诊急救.xlsx', '2026-07-01 10:13:06.119079', '系统管理员', 15);
INSERT INTO `kp_attachment` VALUES (39, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\06d88040-d551-4ee2-924e-6579b32428c1_【模板】医院手麻&重症报价.xlsx', 317852, '【模板】医院手麻&重症报价.xlsx', '06d88040-d551-4ee2-924e-6579b32428c1_【模板】医院手麻&重症报价.xlsx', '2026-07-01 10:17:38.693028', '系统管理员', 15);
INSERT INTO `kp_attachment` VALUES (41, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'C:\\Users\\Administrator\\Documents\\Projects\\Nexus\\backend\\uploads\\0d0e349f-b9fb-4040-ae3e-57981a2cb414_售前工作分工讨论会.pptx', 1485299, '售前工作分工讨论会.pptx', '0d0e349f-b9fb-4040-ae3e-57981a2cb414_售前工作分工讨论会.pptx', '2026-07-10 15:28:03.337398', '赵立文', 16);

-- ----------------------------
-- Table structure for kp_category
-- ----------------------------
DROP TABLE IF EXISTS `kp_category`;
CREATE TABLE `kp_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK4mijtxt75uh398at9979biamo`(`name` ASC) USING BTREE,
  INDEX `FKod5okn8ffyqvpm994whtd5p5s`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `FKod5okn8ffyqvpm994whtd5p5s` FOREIGN KEY (`parent_id`) REFERENCES `kp_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_category
-- ----------------------------
INSERT INTO `kp_category` VALUES (1, 'standard', '2026-04-21 22:07:15.839227', '标准化资料', '01_标准化资料', 10, '2026-04-24 11:57:23.847053', NULL);
INSERT INTO `kp_category` VALUES (2, 'project', '2026-04-21 22:07:15.894242', '02_项目资料库', '02_项目资料库', 20, '2026-04-24 11:55:07.812698', NULL);
INSERT INTO `kp_category` VALUES (3, 'business', '2026-04-21 22:07:15.905242', '03_商务与资质', '03_商务与资质', 30, '2026-04-24 11:54:59.418777', NULL);
INSERT INTO `kp_category` VALUES (4, 'manual', '2026-04-21 22:07:15.909243', '04_实施运维手册', '04_实施运维手册', 40, '2026-04-24 11:56:09.585202', NULL);
INSERT INTO `kp_category` VALUES (5, 'anes', '2026-04-21 22:07:15.916243', 'ANES_手术麻醉', '手术麻醉', 10, '2026-06-29 15:39:47.340361', 1);
INSERT INTO `kp_category` VALUES (6, 'icu', '2026-04-21 22:07:15.920244', '实施规范分类', '重症监护', 20, '2026-04-24 12:00:45.455805', 1);
INSERT INTO `kp_category` VALUES (7, 'clzk', '2026-04-21 22:07:15.923245', '创联资料', '创联资料', 10, '2026-04-24 11:59:48.714384', 3);
INSERT INTO `kp_category` VALUES (8, 'research', '2026-04-21 22:07:15.931247', '需求调研', '需求调研', 10, '2026-04-24 12:00:14.746070', 4);
INSERT INTO `kp_category` VALUES (9, 'zing', '2026-05-25 15:19:10.389769', '', '杭州泽进', 0, '2026-07-01 10:00:22.792374', 1);
INSERT INTO `kp_category` VALUES (10, '', '2026-06-02 17:28:32.621735', '', '环境部署文档', 10, '2026-06-02 17:28:32.621735', 4);
INSERT INTO `kp_category` VALUES (11, 'ems', '2026-06-29 15:39:40.650787', '', '急诊急救', 30, '2026-06-29 15:39:40.650787', 1);

-- ----------------------------
-- Table structure for kp_customer_company
-- ----------------------------
DROP TABLE IF EXISTS `kp_customer_company`;
CREATE TABLE `kp_customer_company`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cooperation_stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `customer_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `industry` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `main_phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `notes` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `region` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `short_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tags` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `website` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_customer_company
-- ----------------------------
INSERT INTO `kp_customer_company` VALUES (1, '上海市浦东新区示例路 100 号', '商务洽谈', '2026-04-21 22:07:17.098483', '系统', '医院', 'contact@hospital-a.com', '医疗', 'A', '15810708464', '市第一人民医院', '本季度重点推进 ICU 平台升级与维保续签。', '周敏', '华东', '市一院', '老客户转介绍', '跟进中', '重点客户,ICU,三甲医院', '2026-06-29 15:42:57.894355', '系统管理员', NULL);
INSERT INTO `kp_customer_company` VALUES (2, '', '初步接触', '2026-06-30 09:07:09.900008', '系统管理员', '医院', '', '医疗', 'A', '', '山西医科大学第一医院', '三甲医院，年度预算充足', '曹亚利', '太原', '山大一', '其他', '跟进中', '', '2026-06-30 09:15:06.837685', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (3, '', '初步接触', '2026-06-30 09:08:13.382155', '系统管理员', '医院', '', '医疗', 'A', '', '山西医科大学第二医院西院区', '', '曹亚利', '太原', '山大二西院', '其他', '跟进中', '', '2026-06-30 09:14:58.001755', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (4, '', '初步接触', '2026-06-30 09:14:42.642156', '系统管理员', '医院', '', '医疗', 'A', '', '忻州市人民医院', '', '曹亚利', '其他', '市人民', '其他', '跟进中', '', '2026-06-30 09:16:13.793112', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (5, '', '初步接触', '2026-06-30 09:17:36.607444', '系统管理员', '医院', '', '医疗', 'A', '', '太原市中心医院', '', '曹亚利', '太原', '汾东院区', '', '跟进中', '', '2026-06-30 09:18:32.145409', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (6, '', '初步接触', '2026-06-30 09:25:37.547634', '系统管理员', '医院', '', '医疗', 'A', '', '吴忠市人民医院', '', '王瑞会', '西北', '', '', '跟进中', '', '2026-06-30 09:31:46.254882', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (7, '', '初步接触', '2026-06-30 09:33:14.075347', '系统管理员', '医院', '', '医疗', 'A', '', '渭南市中心医院', '', '王瑞会', '西北', '', '', '跟进中', '', '2026-06-30 09:36:00.787520', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (8, '', '初步接触', '2026-06-30 09:36:44.548504', '系统管理员', '医院', '', '医疗', 'A', '', '紫阳县人民医院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 09:40:50.393707', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (9, '', '初步接触', '2026-06-30 09:41:40.091996', '系统管理员', '医院', '', '医疗', 'A', '', '蒲城县医院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 09:47:28.788440', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (10, '', '初步接触', '2026-06-30 09:48:36.323220', '系统管理员', '医院', '', '医疗', 'A', '', '镇安县医院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 10:05:27.299127', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (11, '', '初步接触', '2026-06-30 10:06:43.060000', '系统管理员', '医院', '', '医疗', 'A', '', '镇安县中医医院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 10:13:33.897486', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (12, '', '初步接触', '2026-06-30 10:15:48.917991', '系统管理员', '医院', '', '医疗', 'A', '', '西北妇女儿童医院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 10:16:22.881094', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (13, '', '初步接触', '2026-06-30 10:19:43.043902', '系统管理员', '', '', '', 'A', '', '汉阴县妇幼保健院', '', '王瑞会', '', '', '', '跟进中', '', '2026-06-30 10:21:03.048161', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (14, '', '初步接触', '2026-06-30 10:20:56.167191', '系统管理员', '', '', '医疗', 'A', '', '旬阳市妇幼保健院', '', '', '', '', '', '跟进中', '', '2026-06-30 10:21:47.757267', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (15, '', '初步接触', '2026-06-30 10:22:00.056987', '系统管理员', '', '', '医疗', 'A', '', '紫阳县妇幼保健院', '', '', '', '', '', '跟进中', '', '2026-06-30 10:22:41.997945', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (16, 'https://www.ycsdyyy.com/	线下拜访', '商务洽谈', '2026-06-30 10:26:15.646998', '系统管理员', '医院', '', '医疗', 'A', '', '山西盈康一生总医院', '私立三甲医院，钱已到位，院方正在审参数', '董思涵', '其他', '运城第一医院', '其他', '跟进中', '', '2026-06-30 10:28:13.625782', '系统管理员', 'https://www.ycsdyyy.com/');
INSERT INTO `kp_customer_company` VALUES (17, '', '已签约', '2026-06-30 10:30:23.598851', '系统管理员', '医院', '', '医疗', 'B', '', '运城市盐湖区人民医院', '', '董思涵', '', '盐湖区人民医院', '其他', '跟进中', '', '2026-06-30 10:32:49.657767', '系统管理员', 'http://www.sxjcdyy.com/');
INSERT INTO `kp_customer_company` VALUES (18, '', '需求确认', '2026-06-30 10:32:42.366595', '系统管理员', '医院', '', '医疗', 'A', '', '晋城大医院', '', '董思涵', '其他', '晋煤总院', '', '跟进中', '', '2026-06-30 10:35:09.606151', '系统管理员', 'http://www.sxjcdyy.com/');
INSERT INTO `kp_customer_company` VALUES (19, '山西省长治市壶关县古城路145号', '方案交流', '2026-06-30 10:36:22.741855', '系统管理员', '医院', '', '医疗', 'A', '', '壶关县人民医院', '急诊急救系统已出方案，手麻系统和重症系统可能放在明年建设', '董思涵', '', '壶关县人民医院', '', '跟进中', '急诊科，重症医学科，手术室', '2026-06-30 10:43:03.729162', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (20, '山西省吕梁市汾阳市城内胜利街186号', '需求确认', '2026-06-30 10:44:31.095301', '系统管理员', '医院', '', '医疗', 'A', '', '山西省汾阳医院（山西医科大学附属汾阳医院）', '询价挂网，信息科了解到7月底可能会招标', '董思涵', '', '汾阳医院', '', '跟进中', '重症医学科', '2026-06-30 10:45:53.825261', '系统管理员', 'http://www.sxsfyyy.com/');
INSERT INTO `kp_customer_company` VALUES (21, '山西省晋城市城区白水东街1666号', '方案交流', '2026-06-30 10:46:57.252533', '系统管理员', '医院', '', '医疗', 'A', '', '晋城市人民医院', '科教科需求已确认，后续找系统负责人再对接一次', '董思涵', '', '晋城市人民医院', '', '跟进中', '科教科', '2026-06-30 10:47:36.103689', '系统管理员', 'http://www.jcsrmyy.com/');
INSERT INTO `kp_customer_company` VALUES (22, '', '初步接触', '2026-07-01 15:32:50.578809', '系统管理员', '', '', '', 'A', '', '长治市中医研究所附属医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:32:50.578809', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (23, '', '初步接触', '2026-07-01 15:33:25.383883', '系统管理员', '', '', '', 'A', '', '山阴县人民医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:33:25.383883', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (24, '', '初步接触', '2026-07-01 15:33:42.566051', '系统管理员', '', '', '', 'A', '', '清徐县第二人民医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:33:42.566051', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (25, '', '初步接触', '2026-07-01 15:33:53.181381', '系统管理员', '', '', '', 'A', '', '丹凤县医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:33:53.181381', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (26, '', '初步接触', '2026-07-01 15:34:03.257108', '系统管理员', '', '', '', 'A', '', '永寿县人民医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:34:03.257108', '系统管理员', '');
INSERT INTO `kp_customer_company` VALUES (27, '', '初步接触', '2026-07-01 15:35:24.768234', '系统管理员', '', '', '', 'A', '', '山西中医药大学附属医院', '', '', '', '', '', '跟进中', '', '2026-07-01 15:35:24.768234', '系统管理员', '');

-- ----------------------------
-- Table structure for kp_customer_contact
-- ----------------------------
DROP TABLE IF EXISTS `kp_customer_contact`;
CREATE TABLE `kp_customer_contact`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `decision_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `mobile` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `notes` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `office_phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `primary_contact` bit(1) NULL DEFAULT NULL,
  `qq` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `wechat` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKl0epah1kyagvpwqwrrobv53f`(`customer_id` ASC) USING BTREE,
  CONSTRAINT `FKl0epah1kyagvpwqwrrobv53f` FOREIGN KEY (`customer_id`) REFERENCES `kp_customer_company` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_customer_contact
-- ----------------------------
INSERT INTO `kp_customer_contact` VALUES (1, '2026-04-21 22:07:17.105484', '系统', '决策者', '重症医学科', NULL, '男', '13800001111', '王主任', NULL, NULL, 'ICU 主任', b'1', NULL, '2026-06-29 15:42:50.461298', '系统管理员', NULL, 1);
INSERT INTO `kp_customer_contact` VALUES (2, '2026-04-21 22:07:17.109483', '系统', '影响者', '信息科', NULL, '女', '13800002222', '李老师', NULL, NULL, '信息科工程师', b'0', NULL, '2026-06-29 15:42:57.894355', '系统管理员', NULL, 1);
INSERT INTO `kp_customer_contact` VALUES (5, '2026-06-30 09:09:55.874935', '系统管理员', '决策者', '重症医学科', '', '女', '', '曹静', '科室负责人，核心决策人', '681473', 'ICU主任', b'0', '', '2026-06-30 09:09:55.874935', '系统管理员', '', 2);
INSERT INTO `kp_customer_contact` VALUES (6, '2026-06-30 09:11:16.964909', '系统管理员', '决策者', '手术麻醉', '', '女', '13593131366', '吕洁萍', '科室负责人，核心决策人', '', '主任', b'0', '', '2026-06-30 09:11:16.964909', '系统管理员', '', 2);
INSERT INTO `kp_customer_contact` VALUES (7, '2026-06-30 09:13:18.171603', '系统管理员', '影响者', '信息科', '', '', '18734574455', '闫主任', '科室负责人，核心决策人，认可我方方案', '0351-3365400', '信息科主任', b'0', '', '2026-06-30 09:13:18.171603', '系统管理员', '', 3);
INSERT INTO `kp_customer_contact` VALUES (8, '2026-06-30 09:16:13.789003', '系统管理员', '影响者', '手麻醉室', '', '女', '13203507171', '安美玲', '科室负责人，核心决策人', '', '麻醉主任', b'0', '', '2026-06-30 09:16:13.789003', '系统管理员', '', 4);
INSERT INTO `kp_customer_contact` VALUES (9, '2026-06-30 09:18:10.392813', '系统管理员', '影响者', '手麻醉室', '', '', '13503518082', '丁主任', '', '', '麻醉主任', b'0', '', '2026-06-30 09:18:10.392813', '系统管理员', '', 5);
INSERT INTO `kp_customer_contact` VALUES (10, '2026-06-30 09:26:21.569385', '系统管理员', '影响者', '信息科', '', '', '13995358166', '刘森', '', '', '信息科主任', b'0', '', '2026-06-30 09:26:39.402699', '系统管理员', 'wzyyls', 6);
INSERT INTO `kp_customer_contact` VALUES (11, '2026-06-30 09:27:21.086422', '系统管理员', '影响者', '信息科', '', '', '', '张杨', '', '', '信息科工程师', b'0', '', '2026-06-30 09:27:21.086422', '系统管理员', 'onlyyinhuman', 6);
INSERT INTO `kp_customer_contact` VALUES (12, '2026-06-30 09:33:54.105566', '系统管理员', '影响者', '重症医学科', '', '', '', '李媛媛', '', '', '主任', b'0', '', '2026-06-30 09:33:54.105566', '系统管理员', 'wxid_ei73dkawzzrb21', 7);
INSERT INTO `kp_customer_contact` VALUES (13, '2026-06-30 09:34:20.845651', '系统管理员', '影响者', '信息科', '', '', '15929039215', '刘瑞泉', '', '', '信息科主任', b'0', '', '2026-06-30 09:34:42.352584', '系统管理员', 'liuruiquan', 7);
INSERT INTO `kp_customer_contact` VALUES (14, '2026-06-30 09:37:33.757956', '系统管理员', '影响者', '信息科', '', '', '', '魏靖', '', '', '主任', b'0', '', '2026-06-30 09:37:33.757956', '系统管理员', 'wxid_z8f8bua73u6z21', 8);
INSERT INTO `kp_customer_contact` VALUES (15, '2026-06-30 09:38:08.034757', '系统管理员', '影响者', '重症医学科', '', '', '', '琚贻鹏', '', '', '主任', b'0', '', '2026-06-30 09:38:08.034757', '系统管理员', 'wxid_u9mslavw9l8o22', 8);
INSERT INTO `kp_customer_contact` VALUES (16, '2026-06-30 09:39:44.678227', '系统管理员', '影响者', '手术麻醉科', '', '', '13098039493', '左宏平', '', '', '主任', b'0', '', '2026-06-30 09:39:44.678227', '系统管理员', '', 8);
INSERT INTO `kp_customer_contact` VALUES (17, '2026-06-30 09:42:57.012980', '系统管理员', '影响者', '信息科', '', '', '', '蔺旭辉', '', '', '信息科工程师', b'0', '', '2026-06-30 09:42:57.012980', '系统管理员', 'king_417914641', 9);
INSERT INTO `kp_customer_contact` VALUES (18, '2026-06-30 09:45:31.301309', '系统管理员', '影响者', '信息科', '', '', '', '赵主任', '', '', '主任', b'0', '', '2026-06-30 09:45:31.301309', '系统管理员', 'zwg363813079', 9);
INSERT INTO `kp_customer_contact` VALUES (19, '2026-06-30 09:46:01.249731', '系统管理员', '影响者', '手术麻醉科', '', '', '13891458977', '党主任', '', '', '主任', b'0', '', '2026-06-30 09:46:28.857159', '系统管理员', 'wxid_55ubyfjhx0wv21', 9);
INSERT INTO `kp_customer_contact` VALUES (20, '2026-06-30 10:03:01.708016', '系统管理员', '影响者', '信息科', '', '', '13572865434', '张朗', '', '', '主任', b'0', '', '2026-06-30 10:03:01.708016', '系统管理员', 'L88888888__88888888', 10);
INSERT INTO `kp_customer_contact` VALUES (21, '2026-06-30 10:03:42.717619', '系统管理员', '影响者', '手术麻醉科', '', '', '13992452108', '易永华', '', '', '主任', b'0', '', '2026-06-30 10:03:42.717619', '系统管理员', 'yyonghua0810', 10);
INSERT INTO `kp_customer_contact` VALUES (22, '2026-06-30 10:04:33.238230', '系统管理员', '影响者', '重症医学科', '', '', '15109143225', '李主任', '', '', '主任', b'0', '', '2026-06-30 10:04:33.238230', '系统管理员', 'wxid_hjvelujtamkg22', 10);
INSERT INTO `kp_customer_contact` VALUES (23, '2026-06-30 10:12:45.558864', '系统管理员', '影响者', '信息科', '', '', '', '柯主任', '', '', '主任', b'0', '', '2026-06-30 10:12:45.558864', '系统管理员', 'woshiketie', 11);
INSERT INTO `kp_customer_contact` VALUES (24, '2026-06-30 10:16:22.876638', '系统管理员', '影响者', '信息科', '', '', '', '李承蓬', '', '18682929283', '信息科工程师', b'0', '', '2026-06-30 10:16:22.876638', '系统管理员', 'baoleebao', 12);
INSERT INTO `kp_customer_contact` VALUES (25, '2026-06-30 10:28:13.622694', '系统管理员', '影响者', '信息科', '', '', '17835731666', '杜主任', '科室负责人', '', '信息科主任', b'0', '', '2026-06-30 10:28:13.622694', '系统管理员', 'duwenyuanwo', 16);
INSERT INTO `kp_customer_contact` VALUES (26, '2026-06-30 10:31:13.639760', '系统管理员', '决策者', '信息科', '', '', '15503597658', '祁主任', '科室负责人，认可我方系统', '', '信息科主任', b'0', '', '2026-06-30 10:31:13.639760', '系统管理员', 'renjunhui0', 17);
INSERT INTO `kp_customer_contact` VALUES (27, '2026-06-30 10:33:36.402315', '系统管理员', '影响者', '信息科', '', '', '18903569669', '张主任', '科室负责人', '', '信息科主任', b'0', '', '2026-06-30 10:33:36.402315', '系统管理员', 'adonith', 18);
INSERT INTO `kp_customer_contact` VALUES (28, '2026-06-30 10:37:17.875940', '系统管理员', '执行者', '信息科', '', '', '15235585177', '李主任', '科室负责人，认可我方系统', '', '信息科主任', b'0', '', '2026-06-30 10:37:17.875940', '系统管理员', 'xiaolizi202896', 19);
INSERT INTO `kp_customer_contact` VALUES (29, '2026-06-30 10:37:58.404538', '系统管理员', '影响者', '急诊科', '', '', '18235594048', '王主任', '科室负责人，认可我方系统', '', '急诊科主任', b'0', '', '2026-06-30 10:37:58.404538', '系统管理员', 'ww616086055ww', 19);
INSERT INTO `kp_customer_contact` VALUES (30, '2026-06-30 10:45:07.589186', '系统管理员', '影响者', '重症医学科', '', '', '13835835935', '王主任', '', '', '重症医学科主任', b'0', '', '2026-06-30 10:45:07.589186', '系统管理员', '', 20);
INSERT INTO `kp_customer_contact` VALUES (31, '2026-06-30 10:45:53.820122', '系统管理员', '执行者', '信息科', '', '', '18636421316', '侯主任', '科室负责人', '', '信息科主任', b'0', '', '2026-06-30 10:45:53.820122', '系统管理员', 'Hyd5973', 20);
INSERT INTO `kp_customer_contact` VALUES (32, '2026-06-30 10:47:36.098709', '系统管理员', '影响者', '科教科', '', '', '15934177850', '闻主任', '科室负责人', '', '科教科主任', b'0', '', '2026-06-30 10:47:36.098709', '系统管理员', '', 21);

-- ----------------------------
-- Table structure for kp_customer_followup
-- ----------------------------
DROP TABLE IF EXISTS `kp_customer_followup`;
CREATE TABLE `kp_customer_followup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `followup_time` datetime(6) NOT NULL,
  `followup_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `next_followup_date` date NULL DEFAULT NULL,
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `project_id` bigint NULL DEFAULT NULL,
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `result_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKrmqv0766f2cpitxths6t560pd`(`customer_id` ASC) USING BTREE,
  CONSTRAINT `FKrmqv0766f2cpitxths6t560pd` FOREIGN KEY (`customer_id`) REFERENCES `kp_customer_company` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_customer_followup
-- ----------------------------
INSERT INTO `kp_customer_followup` VALUES (1, '完成 ICU 场景调研，客户认可整体方案', '2026-04-21 22:07:17.113485', '系统', '2026-04-21 22:07:17.113485', '面访', '2026-04-28', '周敏', NULL, NULL, '高', 1);
INSERT INTO `kp_customer_followup` VALUES (2, '确认挂网参数、评分标准。推进客户挂采购意向。', '2026-06-30 09:31:46.247805', '系统管理员', '2026-07-01 09:19:27.000000', '电话', '2026-07-28', '王瑞会', NULL, '', '高', 6);
INSERT INTO `kp_customer_followup` VALUES (3, '7月1日投标。', '2026-06-30 09:35:21.598445', '系统管理员', '2026-07-01 09:19:27.000000', '电话', NULL, '王瑞会', NULL, '', '低', 7);
INSERT INTO `kp_customer_followup` VALUES (4, '手麻升级已上报，待过会通过。', '2026-06-30 09:40:50.389552', '系统管理员', '2026-06-02 00:00:00.000000', '电话', '2026-07-15', '王瑞会', NULL, '', '高', 8);
INSERT INTO `kp_customer_followup` VALUES (5, '手麻升级科室已上报。', '2026-06-30 09:47:28.783566', '系统管理员', '2026-05-13 09:19:27.000000', '面访', '2026-07-10', '王瑞会', NULL, '', '高', 9);
INSERT INTO `kp_customer_followup` VALUES (6, '新增点位，医院只愿出2.2W，与渠道确认最终价格', '2026-06-30 10:05:21.423365', '系统管理员', '2026-06-29 00:00:00.000000', '电话', '2026-07-03', '王瑞会', NULL, '', '高', 10);
INSERT INTO `kp_customer_followup` VALUES (7, '医院更倾向山东众阳，计划联系山东众阳销售沟通一下合作', '2026-06-30 10:13:33.892182', '系统管理员', '2026-06-24 00:00:00.000000', '电话', '2026-07-03', '王瑞会', NULL, '', '中', 11);
INSERT INTO `kp_customer_followup` VALUES (8, '只初步沟通了一下，医院手麻已过会，因资金问题暂时搁置', '2026-06-30 10:20:16.114574', '系统管理员', '2026-06-10 00:00:00.000000', '电话', '2026-07-15', '王瑞会', NULL, '', '低', 13);
INSERT INTO `kp_customer_followup` VALUES (9, '已给科室演示系统，计划出个方案', '2026-06-30 10:21:33.890772', '系统管理员', '2026-06-25 00:00:00.000000', '面访', '2026-07-15', '王瑞会', NULL, '', '中', 14);
INSERT INTO `kp_customer_followup` VALUES (10, '医院计划换HIS，建设手麻系统。', '2026-06-30 10:22:41.993843', '系统管理员', '2026-06-03 10:13:07.000000', '面访', '2026-07-13', '王瑞会', NULL, '', '中', 15);
INSERT INTO `kp_customer_followup` VALUES (11, '与客户沟通参观细节', '2026-06-30 10:35:09.601536', '系统管理员', '2026-06-29 00:00:00.000000', '电话', '2026-07-02', '董思涵', NULL, '', '中', 18);
INSERT INTO `kp_customer_followup` VALUES (12, '沟通系统流程，准备议题急诊科主任提申请', '2026-06-30 10:42:47.769729', '系统管理员', '2026-06-29 00:00:00.000000', '面访', '2026-07-09', '董思涵', NULL, '', '中', 19);

-- ----------------------------
-- Table structure for kp_dictionary_item
-- ----------------------------
DROP TABLE IF EXISTS `kp_dictionary_item`;
CREATE TABLE `kp_dictionary_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enabled` bit(1) NOT NULL,
  `item_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `item_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 101 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_dictionary_item
-- ----------------------------
INSERT INTO `kp_dictionary_item` VALUES (1, '2026-04-21 22:07:16.013267', 'customer_status', b'1', '跟进中', '跟进中', NULL, 10, '2026-04-21 22:07:16.013267');
INSERT INTO `kp_dictionary_item` VALUES (2, '2026-04-21 22:07:16.018266', 'customer_status', b'1', '商机确认', '商机确认', NULL, 20, '2026-04-21 22:07:16.018266');
INSERT INTO `kp_dictionary_item` VALUES (3, '2026-04-21 22:07:16.022267', 'customer_status', b'1', '商务洽谈', '商务洽谈', NULL, 30, '2026-04-21 22:07:16.022267');
INSERT INTO `kp_dictionary_item` VALUES (4, '2026-04-21 22:07:16.028269', 'customer_status', b'1', '成交', '成交', NULL, 40, '2026-04-21 22:07:16.028269');
INSERT INTO `kp_dictionary_item` VALUES (5, '2026-04-21 22:07:16.035270', 'customer_status', b'1', '暂停', '暂停', NULL, 50, '2026-04-21 22:07:16.035270');
INSERT INTO `kp_dictionary_item` VALUES (6, '2026-04-21 22:07:16.039271', 'customer_status', b'1', '流失', '流失', NULL, 60, '2026-04-21 22:07:16.039271');
INSERT INTO `kp_dictionary_item` VALUES (7, '2026-04-21 22:07:16.047274', 'customer_level', b'1', 'A', 'A', NULL, 10, '2026-04-21 22:07:16.047274');
INSERT INTO `kp_dictionary_item` VALUES (8, '2026-04-21 22:07:16.051273', 'customer_level', b'1', 'B', 'B', NULL, 20, '2026-04-21 22:07:16.051273');
INSERT INTO `kp_dictionary_item` VALUES (9, '2026-04-21 22:07:16.055275', 'customer_level', b'1', 'C', 'C', NULL, 30, '2026-04-21 22:07:16.055275');
INSERT INTO `kp_dictionary_item` VALUES (10, '2026-04-21 22:07:16.061277', 'customer_level', b'1', 'D', 'D', NULL, 40, '2026-04-21 22:07:16.062280');
INSERT INTO `kp_dictionary_item` VALUES (11, '2026-04-21 22:07:16.071279', 'customer_region', b'1', '太原', '太原', '', 10, '2026-06-29 14:34:16.210050');
INSERT INTO `kp_dictionary_item` VALUES (12, '2026-04-21 22:07:16.075280', 'customer_region', b'1', '长治', '长治', '', 20, '2026-06-29 14:34:29.305252');
INSERT INTO `kp_dictionary_item` VALUES (13, '2026-04-21 22:07:16.079282', 'customer_region', b'1', '晋中', '晋中', '', 30, '2026-06-29 14:35:00.023315');
INSERT INTO `kp_dictionary_item` VALUES (14, '2026-04-21 22:07:16.084281', 'customer_region', b'1', '西安', '西安', '', 50, '2026-07-03 09:49:40.451989');
INSERT INTO `kp_dictionary_item` VALUES (15, '2026-04-21 22:07:16.087284', 'customer_region', b'1', '西南', '西南', '', 60, '2026-07-03 09:49:45.499870');
INSERT INTO `kp_dictionary_item` VALUES (16, '2026-04-21 22:07:16.092284', 'customer_region', b'1', '西北', '西北', '', 70, '2026-07-03 09:49:50.706641');
INSERT INTO `kp_dictionary_item` VALUES (17, '2026-04-21 22:07:16.101287', 'customer_region', b'1', '东北', '东北', NULL, 70, '2026-04-21 22:07:16.101287');
INSERT INTO `kp_dictionary_item` VALUES (18, '2026-04-21 22:07:16.105286', 'customer_region', b'1', '其他', '其他', '', 100, '2026-07-03 09:49:31.888139');
INSERT INTO `kp_dictionary_item` VALUES (19, '2026-04-21 22:07:16.113289', 'customer_industry', b'1', '医疗', '医疗', NULL, 10, '2026-04-21 22:07:16.113289');
INSERT INTO `kp_dictionary_item` VALUES (20, '2026-04-21 22:07:16.116288', 'customer_industry', b'1', '教育', '教育', NULL, 20, '2026-04-21 22:07:16.116288');
INSERT INTO `kp_dictionary_item` VALUES (21, '2026-04-21 22:07:16.120289', 'customer_industry', b'1', '政府', '政府', NULL, 30, '2026-04-21 22:07:16.120289');
INSERT INTO `kp_dictionary_item` VALUES (22, '2026-04-21 22:07:16.124291', 'customer_industry', b'1', '制造', '制造', NULL, 40, '2026-04-21 22:07:16.124291');
INSERT INTO `kp_dictionary_item` VALUES (23, '2026-04-21 22:07:16.132294', 'customer_industry', b'1', '互联网', '互联网', NULL, 50, '2026-04-21 22:07:16.132294');
INSERT INTO `kp_dictionary_item` VALUES (24, '2026-04-21 22:07:16.137293', 'customer_industry', b'1', '金融', '金融', NULL, 60, '2026-04-21 22:07:16.137293');
INSERT INTO `kp_dictionary_item` VALUES (25, '2026-04-21 22:07:16.141294', 'customer_industry', b'1', '能源', '能源', NULL, 70, '2026-04-21 22:07:16.141294');
INSERT INTO `kp_dictionary_item` VALUES (26, '2026-04-21 22:07:16.147297', 'customer_industry', b'1', '其他', '其他', NULL, 80, '2026-04-21 22:07:16.147297');
INSERT INTO `kp_dictionary_item` VALUES (27, '2026-04-21 22:07:16.152297', 'customer_type', b'1', '医院', '医院', NULL, 10, '2026-04-21 22:07:16.152297');
INSERT INTO `kp_dictionary_item` VALUES (28, '2026-04-21 22:07:16.156298', 'customer_type', b'1', 'HIS厂商', 'HIS厂商', NULL, 20, '2026-04-21 22:07:16.156298');
INSERT INTO `kp_dictionary_item` VALUES (29, '2026-04-21 22:07:16.161300', 'customer_type', b'1', '集成商', '集成商', NULL, 30, '2026-04-21 22:07:16.161300');
INSERT INTO `kp_dictionary_item` VALUES (30, '2026-04-21 22:07:16.168300', 'customer_type', b'1', '代理商', '代理商', NULL, 40, '2026-04-21 22:07:16.168300');
INSERT INTO `kp_dictionary_item` VALUES (31, '2026-04-21 22:07:16.172301', 'customer_type', b'1', '渠道商', '渠道商', NULL, 50, '2026-04-21 22:07:16.172301');
INSERT INTO `kp_dictionary_item` VALUES (32, '2026-04-21 22:07:16.177303', 'customer_type', b'1', '科研院所', '科研院所', NULL, 60, '2026-04-21 22:07:16.177303');
INSERT INTO `kp_dictionary_item` VALUES (33, '2026-04-21 22:07:16.182303', 'customer_type', b'1', '政府机构', '政府机构', NULL, 70, '2026-04-21 22:07:16.182303');
INSERT INTO `kp_dictionary_item` VALUES (34, '2026-04-21 22:07:16.186304', 'customer_type', b'1', '其他', '其他', NULL, 80, '2026-04-21 22:07:16.186304');
INSERT INTO `kp_dictionary_item` VALUES (35, '2026-04-21 22:07:16.192306', 'customer_source', b'1', '老客户转介绍', '老客户转介绍', NULL, 10, '2026-04-21 22:07:16.192306');
INSERT INTO `kp_dictionary_item` VALUES (36, '2026-04-21 22:07:16.201308', 'customer_source', b'1', '市场活动', '市场活动', NULL, 20, '2026-04-21 22:07:16.201308');
INSERT INTO `kp_dictionary_item` VALUES (37, '2026-04-21 22:07:16.205308', 'customer_source', b'1', '电话拓展', '电话拓展', NULL, 30, '2026-04-21 22:07:16.205308');
INSERT INTO `kp_dictionary_item` VALUES (38, '2026-04-21 22:07:16.209309', 'customer_source', b'1', '线上线索', '线上线索', NULL, 40, '2026-04-21 22:07:16.209309');
INSERT INTO `kp_dictionary_item` VALUES (39, '2026-04-21 22:07:16.213310', 'customer_source', b'1', '合作伙伴', '合作伙伴', NULL, 50, '2026-04-21 22:07:16.213310');
INSERT INTO `kp_dictionary_item` VALUES (40, '2026-04-21 22:07:16.217312', 'customer_source', b'1', '其他', '其他', NULL, 60, '2026-04-21 22:07:16.217312');
INSERT INTO `kp_dictionary_item` VALUES (41, '2026-04-21 22:07:16.224619', 'customer_stage', b'1', '初步接触', '初步接触', NULL, 10, '2026-04-21 22:07:16.224619');
INSERT INTO `kp_dictionary_item` VALUES (42, '2026-04-21 22:07:16.235026', 'customer_stage', b'1', '需求确认', '需求确认', NULL, 20, '2026-04-21 22:07:16.235026');
INSERT INTO `kp_dictionary_item` VALUES (43, '2026-04-21 22:07:16.239714', 'customer_stage', b'1', '方案交流', '方案交流', NULL, 30, '2026-04-21 22:07:16.239714');
INSERT INTO `kp_dictionary_item` VALUES (44, '2026-04-21 22:07:16.245018', 'customer_stage', b'1', '商务洽谈', '商务洽谈', NULL, 40, '2026-04-21 22:07:16.245018');
INSERT INTO `kp_dictionary_item` VALUES (45, '2026-04-21 22:07:16.250290', 'customer_stage', b'1', '合同推进', '合同推进', NULL, 50, '2026-04-21 22:07:16.250290');
INSERT INTO `kp_dictionary_item` VALUES (46, '2026-04-21 22:07:16.254291', 'customer_stage', b'1', '已签约', '已签约', NULL, 60, '2026-04-21 22:07:16.254291');
INSERT INTO `kp_dictionary_item` VALUES (47, '2026-04-21 22:07:16.264294', 'contact_decision_level', b'1', '决策者', '决策者', NULL, 10, '2026-04-21 22:07:16.264294');
INSERT INTO `kp_dictionary_item` VALUES (48, '2026-04-21 22:07:16.271294', 'contact_decision_level', b'1', '影响者', '影响者', NULL, 20, '2026-04-21 22:07:16.271294');
INSERT INTO `kp_dictionary_item` VALUES (49, '2026-04-21 22:07:16.276296', 'contact_decision_level', b'1', '执行者', '执行者', NULL, 30, '2026-04-21 22:07:16.276296');
INSERT INTO `kp_dictionary_item` VALUES (50, '2026-04-21 22:07:16.286300', 'contact_gender', b'1', '男', '男', NULL, 10, '2026-04-21 22:07:16.286300');
INSERT INTO `kp_dictionary_item` VALUES (51, '2026-04-21 22:07:16.292301', 'contact_gender', b'1', '女', '女', NULL, 20, '2026-04-21 22:07:16.292301');
INSERT INTO `kp_dictionary_item` VALUES (52, '2026-04-21 22:07:16.303303', 'followup_type', b'1', '电话', '电话', NULL, 10, '2026-04-21 22:07:16.304302');
INSERT INTO `kp_dictionary_item` VALUES (53, '2026-04-21 22:07:16.309303', 'followup_type', b'1', '微信', '微信', NULL, 20, '2026-04-21 22:07:16.309303');
INSERT INTO `kp_dictionary_item` VALUES (54, '2026-04-21 22:07:16.315305', 'followup_type', b'1', '面访', '面访', NULL, 30, '2026-04-21 22:07:16.315305');
INSERT INTO `kp_dictionary_item` VALUES (55, '2026-04-21 22:07:16.319306', 'followup_type', b'1', '会议', '会议', NULL, 40, '2026-04-21 22:07:16.319306');
INSERT INTO `kp_dictionary_item` VALUES (56, '2026-04-21 22:07:16.324307', 'followup_result', b'1', '高', '高', NULL, 10, '2026-04-21 22:07:16.324307');
INSERT INTO `kp_dictionary_item` VALUES (57, '2026-04-21 22:07:16.334312', 'followup_result', b'1', '中', '中', NULL, 20, '2026-04-21 22:07:16.334312');
INSERT INTO `kp_dictionary_item` VALUES (58, '2026-04-21 22:07:16.340312', 'followup_result', b'1', '低', '低', NULL, 30, '2026-04-21 22:07:16.340312');
INSERT INTO `kp_dictionary_item` VALUES (59, '2026-04-21 22:07:16.348314', 'project_stage', b'1', '商机立项', '商机立项', NULL, 10, '2026-04-21 22:07:16.348314');
INSERT INTO `kp_dictionary_item` VALUES (60, '2026-04-21 22:07:16.351312', 'project_stage', b'1', '合同执行', '合同执行', NULL, 20, '2026-04-21 22:07:16.351312');
INSERT INTO `kp_dictionary_item` VALUES (61, '2026-04-21 22:07:16.355314', 'project_stage', b'1', '实施交付', '实施交付', NULL, 30, '2026-04-21 22:07:16.355314');
INSERT INTO `kp_dictionary_item` VALUES (62, '2026-04-21 22:07:16.358315', 'project_stage', b'1', '验收结算', '验收结算', NULL, 40, '2026-04-21 22:07:16.358315');
INSERT INTO `kp_dictionary_item` VALUES (63, '2026-04-21 22:07:16.366316', 'project_stage', b'1', '售后维保', '售后维保', NULL, 50, '2026-04-21 22:07:16.366316');
INSERT INTO `kp_dictionary_item` VALUES (64, '2026-04-21 22:07:16.373318', 'project_status', b'1', '进行中', '进行中', NULL, 10, '2026-04-21 22:07:16.373318');
INSERT INTO `kp_dictionary_item` VALUES (65, '2026-04-21 22:07:16.377318', 'project_status', b'1', '已完成', '已完成', NULL, 20, '2026-04-21 22:07:16.377318');
INSERT INTO `kp_dictionary_item` VALUES (66, '2026-04-21 22:07:16.381320', 'project_status', b'1', '暂停', '暂停', NULL, 30, '2026-04-21 22:07:16.381320');
INSERT INTO `kp_dictionary_item` VALUES (67, '2026-04-21 22:07:16.386321', 'project_risk', b'1', '低', '低', NULL, 10, '2026-04-21 22:07:16.386321');
INSERT INTO `kp_dictionary_item` VALUES (68, '2026-04-21 22:07:16.389321', 'project_risk', b'1', '中', '中', NULL, 20, '2026-04-21 22:07:16.389321');
INSERT INTO `kp_dictionary_item` VALUES (69, '2026-04-21 22:07:16.397324', 'project_risk', b'1', '高', '高', NULL, 30, '2026-04-21 22:07:16.397324');
INSERT INTO `kp_dictionary_item` VALUES (70, '2026-04-21 22:07:16.404326', 'project_activity_type', b'1', '普通记录', '普通记录', NULL, 10, '2026-04-21 22:07:16.404326');
INSERT INTO `kp_dictionary_item` VALUES (71, '2026-04-21 22:07:16.407325', 'project_activity_type', b'1', '合同', '合同', NULL, 20, '2026-04-21 22:07:16.407325');
INSERT INTO `kp_dictionary_item` VALUES (72, '2026-04-21 22:07:16.411327', 'project_activity_type', b'1', '资料', '资料', NULL, 30, '2026-04-21 22:07:16.411327');
INSERT INTO `kp_dictionary_item` VALUES (73, '2026-04-21 22:07:16.416328', 'project_activity_type', b'1', '沟通', '沟通', NULL, 40, '2026-04-21 22:07:16.416328');
INSERT INTO `kp_dictionary_item` VALUES (74, '2026-04-21 22:07:16.419328', 'project_activity_type', b'1', '售后', '售后', NULL, 50, '2026-04-21 22:07:16.419328');
INSERT INTO `kp_dictionary_item` VALUES (75, '2026-04-21 22:07:16.425330', 'project_contract_status', b'1', '未签约', '未签约', NULL, 10, '2026-04-21 22:07:16.425330');
INSERT INTO `kp_dictionary_item` VALUES (76, '2026-04-21 22:07:16.433331', 'project_contract_status', b'1', '签约中', '签约中', NULL, 20, '2026-04-21 22:07:16.433331');
INSERT INTO `kp_dictionary_item` VALUES (77, '2026-04-21 22:07:16.437331', 'project_contract_status', b'1', '已签约', '已签约', NULL, 30, '2026-04-21 22:07:16.437331');
INSERT INTO `kp_dictionary_item` VALUES (78, '2026-04-21 22:07:16.441333', 'project_contract_status', b'1', '合同变更中', '合同变更中', NULL, 40, '2026-04-21 22:07:16.441333');
INSERT INTO `kp_dictionary_item` VALUES (79, '2026-04-21 22:07:16.446338', 'project_contract_status', b'1', '合同终止', '合同终止', NULL, 50, '2026-04-21 22:07:16.446338');
INSERT INTO `kp_dictionary_item` VALUES (80, '2026-04-21 22:07:16.452336', 'project_payment_status', b'1', '未回款', '未回款', NULL, 10, '2026-04-21 22:07:16.452336');
INSERT INTO `kp_dictionary_item` VALUES (81, '2026-04-21 22:07:16.455336', 'project_payment_status', b'1', '部分回款', '部分回款', NULL, 20, '2026-04-21 22:07:16.456336');
INSERT INTO `kp_dictionary_item` VALUES (82, '2026-04-21 22:07:16.460337', 'project_payment_status', b'1', '按计划回款', '按计划回款', NULL, 30, '2026-04-21 22:07:16.460337');
INSERT INTO `kp_dictionary_item` VALUES (83, '2026-04-21 22:07:16.467339', 'project_payment_status', b'1', '回款逾期', '回款逾期', NULL, 40, '2026-04-21 22:07:16.467339');
INSERT INTO `kp_dictionary_item` VALUES (84, '2026-04-21 22:07:16.471339', 'project_payment_status', b'1', '回款完成', '回款完成', NULL, 50, '2026-04-21 22:07:16.471339');
INSERT INTO `kp_dictionary_item` VALUES (85, '2026-04-21 22:07:16.476341', 'project_acceptance_status', b'1', '未开始', '未开始', NULL, 10, '2026-04-21 22:07:16.476341');
INSERT INTO `kp_dictionary_item` VALUES (86, '2026-04-21 22:07:16.481342', 'project_acceptance_status', b'1', '验收准备中', '验收准备中', NULL, 20, '2026-04-21 22:07:16.481342');
INSERT INTO `kp_dictionary_item` VALUES (87, '2026-04-21 22:07:16.484342', 'project_acceptance_status', b'1', '验收中', '验收中', NULL, 30, '2026-04-21 22:07:16.484342');
INSERT INTO `kp_dictionary_item` VALUES (88, '2026-04-21 22:07:16.487343', 'project_acceptance_status', b'1', '验收通过', '验收通过', NULL, 40, '2026-04-21 22:07:16.487343');
INSERT INTO `kp_dictionary_item` VALUES (89, '2026-04-21 22:07:16.490344', 'project_acceptance_status', b'1', '验收未通过', '验收未通过', NULL, 50, '2026-04-21 22:07:16.490344');
INSERT INTO `kp_dictionary_item` VALUES (90, '2026-04-21 22:07:16.500346', 'project_service_status', b'1', '未启动', '未启动', NULL, 10, '2026-04-21 22:07:16.500346');
INSERT INTO `kp_dictionary_item` VALUES (91, '2026-04-21 22:07:16.504347', 'project_service_status', b'1', '服务中', '服务中', NULL, 20, '2026-04-21 22:07:16.504347');
INSERT INTO `kp_dictionary_item` VALUES (92, '2026-04-21 22:07:16.507347', 'project_service_status', b'1', '稳定运行', '稳定运行', NULL, 30, '2026-04-21 22:07:16.507347');
INSERT INTO `kp_dictionary_item` VALUES (93, '2026-04-21 22:07:16.511350', 'project_service_status', b'1', '服务预警', '服务预警', NULL, 40, '2026-04-21 22:07:16.511350');
INSERT INTO `kp_dictionary_item` VALUES (94, '2026-04-21 22:07:16.516350', 'project_service_status', b'1', '服务结束', '服务结束', NULL, 50, '2026-04-21 22:07:16.516350');
INSERT INTO `kp_dictionary_item` VALUES (95, '2026-04-21 22:07:16.521351', 'knowledge_type', b'1', '文档', '文档', NULL, 10, '2026-04-21 22:07:16.521351');
INSERT INTO `kp_dictionary_item` VALUES (96, '2026-04-21 22:07:16.525355', 'knowledge_type', b'1', '制度', '制度', NULL, 20, '2026-04-21 22:07:16.525355');
INSERT INTO `kp_dictionary_item` VALUES (97, '2026-04-21 22:07:16.534355', 'knowledge_type', b'1', '方案', '方案', NULL, 30, '2026-04-21 22:07:16.534355');
INSERT INTO `kp_dictionary_item` VALUES (98, '2026-04-21 22:07:16.539357', 'knowledge_type', b'1', '培训', '培训', NULL, 40, '2026-04-21 22:07:16.539357');
INSERT INTO `kp_dictionary_item` VALUES (99, '2026-07-03 09:49:20.054050', 'customer_region', b'1', '晋城', '晋城', '', 40, '2026-07-03 09:50:00.541428');
INSERT INTO `kp_dictionary_item` VALUES (100, '2026-07-03 09:50:18.569954', 'customer_region', b'1', '运城', '运城', '', 50, '2026-07-03 09:50:18.569954');

-- ----------------------------
-- Table structure for kp_knowledge_item
-- ----------------------------
DROP TABLE IF EXISTS `kp_knowledge_item`;
CREATE TABLE `kp_knowledge_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operation_log` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `project_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKg5aauouiluovc8kxh3h9lni0s`(`category_id` ASC) USING BTREE,
  INDEX `FKexo6n5ci5qa4frjh6xrw9221`(`project_id` ASC) USING BTREE,
  CONSTRAINT `FKexo6n5ci5qa4frjh6xrw9221` FOREIGN KEY (`project_id`) REFERENCES `kp_project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKg5aauouiluovc8kxh3h9lni0s` FOREIGN KEY (`category_id`) REFERENCES `kp_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_knowledge_item
-- ----------------------------
INSERT INTO `kp_knowledge_item` VALUES (9, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台场地\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n> - 进行环境部署 详见环境安装部署文档\n> - 环境部署成功之后进行系统授权 授权成功之后激活\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书\n> - 接口对接\n   >   1.将我们的功能参数需求发给对应负责人 获取对应的接口文档\n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照附件1模板记录 ', '2026-05-22 14:21:23.399317', '系统管理员', '[2026-05-22T14:21:23.399317] 内容保存 - 内容已更新\n[2026-05-22T14:27:26.002633400] 内容保存 - 内容已更新\n[2026-05-22T14:32:39.228665400] 内容保存 - 内容已更新\n[2026-05-22T14:35:54.053943400] 内容保存 - 内容已更新\n[2026-05-22T14:39:11.685487500] 内容保存 - 内容已更新\n[2026-05-28T17:59:15.694180100] 内容保存 - 内容已更新\n[2026-06-02T17:26:26.903147] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '前期调研方案', '文档', '2026-06-02 17:26:26.903147', '系统管理员', 8, NULL);
INSERT INTO `kp_knowledge_item` VALUES (10, '', '2026-05-25 15:20:04.572036', '系统管理员', '[2026-05-25T15:20:04.572036400] 内容保存 - 内容已更新\n[2026-05-25T15:22:39.373995600] 内容保存 - 内容已更新\n[2026-05-25T15:22:56.295779900] 内容保存 - 内容已更新\n[2026-06-29T09:31:09.817774300] 内容保存 - 内容已更新', NULL, '', '正常', '彩页、宣传视频', '', '公司宣传资料', '文档', '2026-06-29 09:31:09.819881', '系统管理员', 9, NULL);
INSERT INTO `kp_knowledge_item` VALUES (11, '# 环境部署安装  \n## 安装步骤  \n> - 查看IP是否连接成功  \n`ping + IP`  \n> - 传输手麻环境部署需要的环境部署包和工具  \n    > 1.MobaXterm_chs  \n        利用文件传输工具创建session对话  （IP  端口号 用户名 密码）  *his视图连接oracle*  \n      2.准备相关环境部署包  \n        a.docker.zip  \n        b.navicat  \n        c.zing-anes-update.zip  \n        d.zing-api-update.zip  \n        e.zing-mysql.zip  \n        f.zing-redis  \n        g.mdi_install.tar.gz  \n> - 安装docker.zip  \n    > 1 .`unzip docker.zip -d/data` (解压在/data目录下)  \n    > 2 . 进入data/docker,执行` dpkg -i *.deb `  \n    > 3 . 设置开机自启动 `systemctl enable docker`  \n    > 4 . 启动服务 `systemctl start docker`  \n    > 5 . 执行命令 `docker ps`，确认是否安装成功\n> - 建立zingnet网络  \n    > 1 . `docker network create zingnet` 建立zingnet网络  \n    > 2 . `docker network inspect zingnet` 查询zing网络  \n    > 3 . `docker network ls\' docker网络显示  \n    > 4 . `docker network disconnect (网络名称：zingnet)(需要连接的名称 zing-mysql)` docker网络断开命令  \n> - 安装数据库  \n    > 1 . 解压数据库`unzip zing-mysql.zip -d/data `  \n    > 2 . `chmod -R 777 zing-mysql/*` 赋予脚本最高权限命令  \n    > 3 . `sh zing-mysql.install.sh` 运行shell自动化脚本安装mysql  \n    *(快捷键 tab 补全 双击tab显示)*  \n> - 安装redis  \n    > 1 . 解压redis `unzip zing-redis -d/data`  \n    > 2 . `chmod -R 777 zing-redis/*` 赋予脚本最高权限命令   \n    > 3 . `sh zing-redis.install.sh` 运行shell自动化脚本安装  \n> - 安装anes  \n    > 1 . 解压redis `unzip zing-anes -d/data`  \n    > 2 . `chmod -R 777 zing-anes/*` 赋予脚本最高权限命令   \n    > 3 . `sh zing-anes.install.sh` 运行shell自动化脚本安装    \n> - 安装api  \n    > 1 . 解压redis `unzip zing-api -d/data`  \n    > 2 . `chmod -R 777 zing-api/*` 赋予脚本最高权限命令   \n    > 3 . `sh zing-api.sh` 运行shell自动化脚本安装 \n> - 安装mdi  \n    > 1 . 解压redis `tar -xzvf mdi_install_V4.1.tar.gz -d/data` \n    > 2 . `mkdir -p data/db`创建db文件夹 *（mdi的数据一定要给绝对路径/data/db)*  \n    > 2 . `chmod -R 777 mdi/*` 赋予脚本最高权限命令   \n    > 3 . `sh mdi.install.sh` 运行shell自动化脚本安装     \n> - 安装navicat并将zing_anes_db_prod.sql和zing_api_db_prod.sql导入navicat *(注意核对数据库名称)*  \n    > 1 . 建立session对话，连接我们的数据库 （MYSQL IP 23302 root zing@123）  \n    > 2 . 导入zing_anes_db_prod.sql和zing_api_db_prod.sql，字符集选择utf8mb4 排序规则utf8mb_general_ci  \n> - 测试安装是否成功  \n    > 1 . 浏览器输入: 服务器地址:12202  测试API平台是否可以启动  \n    > 2 . 浏览器输入: 服务器地址:13202  测试手麻业务系统是否启动  \n    > 3 . 浏览器输入: 服务器地址:17202 测试重症业务系统是否可以启动  \n    > 4 . 浏览器输入: 服务器地址:7799 测试MDI平台是否启动  \n        *账号密码都为admin   1234568*  \n        *外网登录：开放平台：https://ops.zingsys.com/login 账号 密码  项目协同管路平台：https://pcmp.zingsys.com/login *  \n> - 常用命令  \n    >  \n## 激活系统  \n  > - 激活顺序  \n    *开放平台生成激活码——>MDI——> 业务系统*  \n## 制作相关的文书  \n> - 开放平台上根据自己的需要进行进行文书制作  \n    >1.进行文书制作\n> - 文书制作成功之后导入业务系统  \n    > 1.制作完成之后点击发布导出后导入业务系统  \n    > 2.上传成功之后，第一版不需要进行启用 如果是第二版则需要进行启用确定  \n    > 3 .将做发布的文书做赋予权限  \n    > 4 .刷新缓存并重新登录进行查看  \n    > 5 .在文书上传时选择对应的文书名称 不建议进行新增文书  \n    > 6 .如果没有对应的文书名称可以将不需要的文书进行修改，并点击配置进行路径修改  \n    > 7 .文书上传成功之后要进行点击 （*可能存在字符内存不够大的情况 可以进入对应的数据库进行修改字符串大小，也可以进行文书的修改*）\n## 制作手麻业务系统所需的接口(ZING-ANES 11个)\n> - 获取手术申请接口-ANES-OPERINFO \n> - 获取职工接口-ANES_EMP  \n> - 获取科室接口-ANES_DEPT  \n> - 获取检验报告接口-  \n> - 获取检验报告明细接口-ANES_LIS_REPITEMS（两个为一起的）  \n> - 获取心电图报告接口-ANES_ECG  \n> - 获取PACS报告接口-ANES_PACE  \n> - 获取电子病历数据接口-ANES-EMP  \n> - 获取手术操作名称字典接口-ANES_DICT_OPERS\n> - 获取疾病诊断字典接口-ANES_DICT_DIAGNOSIS  \n> - 获取药品字典接口-ANES_DICT_DRUGS  \n> - 单点登录（这个看需求）\n> - MDI数据接口（根据数据采集器的数量进行接口数量的确定）\n----#在接口流程中 发现body{}中为空 则证明接口不正确 找出对应的问题进行修改\n## 业务系统配置  \n> - 查看数据是否可以获取  \n> - 检验获取的数据是否正确  \n--- 医生的编号=工号  \n--- 病案号 = 住院号  \n--- detail_status_code 不做字段映射  \n--- 传入编号的字段，需要用子查询来进行查询对应编号的都有名称  \n--eg：select realname from *', '2026-06-02 17:52:38.143026', '系统管理员', '[2026-06-02T17:52:38.143026] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '环境部署实施文档', '文档', '2026-06-02 17:52:38.143026', '系统管理员', 10, NULL);
INSERT INTO `kp_knowledge_item` VALUES (12, '#  系统激活\n## 前提条件\n>- 在服务器上部署好MDI、api、业务系统（手麻、重症）\n>- 在开放平台申请授权码\n   > 1.申请场地账号\n   > 2.登录场地账号，在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n>  ![](/api/attachments/21/download)\n   > 3.等待授权通过 通过后进行系统激活\n## 系统激活流程  \n> ### 登录MDI\n>- 1.登录MDI系统 (需要先进入位置管理 完成基本组织类型填写）\n>- 2.选择运维-注册管理 复制机器码（如果是远程支持，堡垒机不能复制选择微信二维码扫码复制）\n  ![](/api/attachments/22/download)\n>- 3.讲复制的机器码粘贴到开放平台上 生成激活码\n>- ![](/api/attachments/23/download)\n>- 4.进入开放平台个人中心生成激活码\n>- ![](/api/attachments/24/download)\n>- 5.讲生成的激活码 填入MDI系统 完成之后成功激活\n>- ![](/api/attachments/25/download)\n> ### 登录API\n>- 1.登录API系统，输入开放平台上生成的激活码\n>  ![](/api/attachments/26/download)\n>- 2.进行数据源编写 系统数据源 回写his数据源\n>- ![](/api/attachments/29/download)\n>- ![](/api/attachments/31/download)\n>- 3.上传接口则可正常运行调试\n> ### 登录业务系统-手麻系统\n>- 1.登录业务系统\n>- 2.点击配置中心 - 系统管理 - 租户管理 将开放平台的密钥填写到这里\n>- ![](/api/attachments/32/download)\n>- ![](/api/attachments/33/download)\n>- 3.点击配置中心-参数配置-Ctrl+F 搜索系统授权-  把地址修改成MDI地址，端口不动\n>- ![](/api/attachments/34/download)\n>- 4. 配置中心-运维管理 - 授权管理 进行点击刷新\n>-![](/api/attachments/35/download)\n\n \n\n\n\n', '2026-06-03 17:38:24.657232', '系统管理员', '[2026-06-03T17:38:24.657231600] 内容保存 - 内容已更新\n[2026-06-03T17:40:29.557529600] 内容保存 - 内容已更新\n[2026-06-03T17:46:18.101387300] 内容保存 - 内容已更新\n[2026-06-12T12:27:07.520499800] 内容保存 - 内容已更新\n[2026-06-12T14:13:11.661277400] 内容保存 - 内容已更新\n[2026-06-12T14:19:01.600241200] 内容保存 - 内容已更新\n[2026-06-12T14:19:36.511041] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '系统授权激活', '文档', '2026-06-12 14:19:36.512075', '系统管理员', 10, NULL);
INSERT INTO `kp_knowledge_item` VALUES (13, '', '2026-06-05 15:50:46.776738', '系统管理员', '[2026-06-05T15:50:46.776738200] 内容保存 - 内容已更新\n[2026-06-05T16:54:17.670232900] 内容保存 - 内容已更新\n[2026-06-07T12:22:27.917972400] 内容保存 - 内容已更新', NULL, '厂家资料', '正常', '', '数据库', '数据库运维边界的纪律规范', '制度', '2026-06-07 12:22:27.922179', '系统管理员', 4, NULL);
INSERT INTO `kp_knowledge_item` VALUES (14, '', '2026-06-29 15:40:24.293124', '系统管理员', '[2026-06-29T15:40:24.293124] 内容保存 - 内容已更新\n[2026-06-29T15:40:46.733572100] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '急诊急救-售前资料', '文档', '2026-06-29 15:40:46.733572', '系统管理员', 11, NULL);
INSERT INTO `kp_knowledge_item` VALUES (15, '', '2026-07-01 10:01:38.809781', '系统管理员', '[2026-07-01T10:01:38.810803700] 内容保存 - 内容已更新\n[2026-07-01T10:13:06.055073200] 内容保存 - 内容已更新\n[2026-07-01T10:13:06.711237800] 内容保存 - 内容已更新\n[2026-07-01T10:17:38.628144100] 内容保存 - 内容已更新\n[2026-07-01T10:19:08.094218300] 内容保存 - 内容已更新\n[2026-07-01T10:19:18.050823800] 内容保存 - 内容已更新', NULL, '内部整理', '正常', '', '报价', '重症、手麻、急诊急救标准报价（参考）', '文档', '2026-07-01 10:19:18.050824', '系统管理员', 9, NULL);
INSERT INTO `kp_knowledge_item` VALUES (16, '', '2026-07-10 15:28:02.787885', '赵立文', '[2026-07-10T15:28:02.791061300] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '售前工作-职责规范', '文档', '2026-07-10 15:28:02.791061', '赵立文', 9, NULL);
INSERT INTO `kp_knowledge_item` VALUES (17, '吴鹏\n13313541484\n284992818@qq.com\n山西省太原市小店区龙城大街75号鸿泰国际大厦B座9层915室', '2026-09-04 15:09:54.481974', '系统管理员', '[2026-09-04T15:09:54.487191] 内容保存 - 内容已更新\n[2026-09-04T15:10:57.694827900] 内容保存 - 内容已更新', NULL, '', '正常', '', '', '中科兴龙-信息', '文档', '2026-09-04 15:10:57.694828', '系统管理员', 3, NULL);

-- ----------------------------
-- Table structure for kp_knowledge_item_version
-- ----------------------------
DROP TABLE IF EXISTS `kp_knowledge_item_version`;
CREATE TABLE `kp_knowledge_item_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKbe937o7l1jbc2f5jvqsb3w19w`(`item_id` ASC) USING BTREE,
  CONSTRAINT `FKbe937o7l1jbc2f5jvqsb3w19w` FOREIGN KEY (`item_id`) REFERENCES `kp_knowledge_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_knowledge_item_version
-- ----------------------------
INSERT INTO `kp_knowledge_item_version` VALUES (25, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n>-', '2026-05-22 14:27:25.966188', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (26, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书  \n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照模板记录  ', '2026-05-22 14:32:39.201873', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (27, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书  \n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照附件1模板记录 ', '2026-05-22 14:35:54.032923', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (28, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n   >  2.申请成功后，进行手麻/重症环境的搭建\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书  \n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照附件1模板记录 ', '2026-05-22 14:39:11.656747', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (29, '', '2026-05-25 15:22:39.357358', '系统管理员', '', '', '公司宣传资料', 10);
INSERT INTO `kp_knowledge_item_version` VALUES (30, '', '2026-05-25 15:22:56.272293', '系统管理员', '', '', '公司宣传资料', 10);
INSERT INTO `kp_knowledge_item_version` VALUES (31, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n   >  2.申请成功后，进行手麻/重症环境的搭建\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书  \n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照附件1模板记录 ', '2026-05-28 17:59:15.678202', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (32, '# 前期调研\n## 账号申请\n>- 申请对应单位的开放平台账号\n## 申请服务器资源 \n>- 向信息科申请服务器资源\n   > 1.CPU：4核及以上，主频2.4G及以上；\n    内存：32G及以上；\n    存储空间：1T及以上。\n    服务器操作系统需求:Debian11\n>  堡垒机进行远程登录\n   >  2.申请成功后，进行手麻/重症环境的搭建\n## 业务系统调研  \n>- 文书收集\n1.根据功能参数收集对应的文书以及在实际操作过程中医院所需要的文书  \n## 硬件调研\n>- 记录监护仪、麻醉机、呼吸机等相关采集数据设备的基本信息，按照附件1模板记录 ', '2026-06-02 17:26:26.887338', '系统管理员', '', '', '前期调研方案', 9);
INSERT INTO `kp_knowledge_item_version` VALUES (33, '# 系统激活  \n- 1. 前提条件：环境部署成功\n\n', '2026-06-03 17:40:29.543649', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (34, '# 系统激活  \n- 1. 前提条件：环境部署成功\n  2. 在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi （授权时间都为12个月）\n![](/api/attachments/18/download)\n\n', '2026-06-03 17:46:18.101387', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (35, '', '2026-06-05 16:54:17.648635', '系统管理员', '', '数据库', '数据库运维边界的纪律规范', 13);
INSERT INTO `kp_knowledge_item_version` VALUES (36, '', '2026-06-07 12:22:27.860509', '系统管理员', '', '数据库', '数据库运维边界的纪律规范', 13);
INSERT INTO `kp_knowledge_item_version` VALUES (37, '# 系统激活  \n- 1. 前提条件：环境部署成功\n  2. 在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n![](/api/attachments/18/download)\n\n', '2026-06-12 12:27:07.512797', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (38, '# 系统激活  \n- 1. 前提条件：环境部署成功\n  2. 在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n![](/api/attachments/18/download)\n\n\n#  系统激活\n## 前提条件\n>- 在服务器上部署好MDI、api、业务系统（手麻、重症）\n>- 在开放平台申请授权码\n   > 1.申请场地账号\n   > 2.登录场地账号，在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n>  ![](/api/attachments/21/download)\n   > 3.等待授权通过 通过后进行系统激活\n## 系统激活流程  \n> ### 登录MDI\n>- 1.登录MDI系统 (需要先进入位置管理 完成基本组织类型填写）\n>- 2.选择运维-注册管理 复制机器码（如果是远程支持，堡垒机不能复制选择微信二维码扫码复制）\n  ![](/api/attachments/22/download)\n>- 3.讲复制的机器码粘贴到开放平台上 生成激活码\n>- ![](/api/attachments/23/download)\n>- 4.进入开放平台个人中心生成激活码\n>- ![](/api/attachments/24/download)\n>- 5.讲生成的激活码 填入MDI系统 完成之后成功激活\n>- ![](/api/attachments/25/download)\n> ### 登录API\n>- 1.登录API系统，输入灶开放平台上生成的\n>  ![](/api/attachments/26/download)\n', '2026-06-12 14:13:11.639959', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (39, '# 系统激活  \n- 1. 前提条件：环境部署成功\n  2. 在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n![](/api/attachments/18/download)\n\n\n#  系统激活\n## 前提条件\n>- 在服务器上部署好MDI、api、业务系统（手麻、重症）\n>- 在开放平台申请授权码\n   > 1.申请场地账号\n   > 2.登录场地账号，在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n>  ![](/api/attachments/21/download)\n   > 3.等待授权通过 通过后进行系统激活\n## 系统激活流程  \n> ### 登录MDI\n>- 1.登录MDI系统 (需要先进入位置管理 完成基本组织类型填写）\n>- 2.选择运维-注册管理 复制机器码（如果是远程支持，堡垒机不能复制选择微信二维码扫码复制）\n  ![](/api/attachments/22/download)\n>- 3.讲复制的机器码粘贴到开放平台上 生成激活码\n>- ![](/api/attachments/23/download)\n>- 4.进入开放平台个人中心生成激活码\n>- ![](/api/attachments/24/download)\n>- 5.讲生成的激活码 填入MDI系统 完成之后成功激活\n>- ![](/api/attachments/25/download)\n> ### 登录API\n>- 1.登录API系统，输入开放平台上生成的激活码\n>  ![](/api/attachments/26/download)\n>- 2.进行数据源编写 系统数据源 回写his数据源\n>- ![](/api/attachments/29/download)\n>- ![](/api/attachments/31/download)\n>- 3.上传接口则可正常运行调试\n> ### 登录业务系统-手麻系统\n>- 1.登录业务系统\n>- 2.点击配置中心 - 系统管理 - 租户管理 将开放平台的密钥填写到这里\n>- ![](/api/attachments/32/download)\n>- ![](/api/attachments/33/download)\n \n\n\n\n', '2026-06-12 14:19:01.579370', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (40, '#  系统激活\n## 前提条件\n>- 在服务器上部署好MDI、api、业务系统（手麻、重症）\n>- 在开放平台申请授权码\n   > 1.申请场地账号\n   > 2.登录场地账号，在开放平台上进行授权申请 按照医院所需的手麻/重症/mdi选择授权产品 并根据实际所需要实施的床位和连接的设备进行填写（授权时间都为12个月）\n>  ![](/api/attachments/21/download)\n   > 3.等待授权通过 通过后进行系统激活\n## 系统激活流程  \n> ### 登录MDI\n>- 1.登录MDI系统 (需要先进入位置管理 完成基本组织类型填写）\n>- 2.选择运维-注册管理 复制机器码（如果是远程支持，堡垒机不能复制选择微信二维码扫码复制）\n  ![](/api/attachments/22/download)\n>- 3.讲复制的机器码粘贴到开放平台上 生成激活码\n>- ![](/api/attachments/23/download)\n>- 4.进入开放平台个人中心生成激活码\n>- ![](/api/attachments/24/download)\n>- 5.讲生成的激活码 填入MDI系统 完成之后成功激活\n>- ![](/api/attachments/25/download)\n> ### 登录API\n>- 1.登录API系统，输入开放平台上生成的激活码\n>  ![](/api/attachments/26/download)\n>- 2.进行数据源编写 系统数据源 回写his数据源\n>- ![](/api/attachments/29/download)\n>- ![](/api/attachments/31/download)\n>- 3.上传接口则可正常运行调试\n> ### 登录业务系统-手麻系统\n>- 1.登录业务系统\n>- 2.点击配置中心 - 系统管理 - 租户管理 将开放平台的密钥填写到这里\n>- ![](/api/attachments/32/download)\n>- ![](/api/attachments/33/download)\n>- 3.点击配置中心-参数配置-Ctrl+F 搜索系统授权-  把地址修改成MDI地址，端口不动\n>- ![](/api/attachments/34/download)\n>- 4. 配置中心-运维管理 - 授权管理 进行点击刷新\n>-![](/api/attachments/35/download)\n\n \n\n\n\n', '2026-06-12 14:19:36.489272', '系统管理员', '', '', '系统授权激活', 12);
INSERT INTO `kp_knowledge_item_version` VALUES (41, '', '2026-06-29 09:31:09.784059', '系统管理员', '', '', '公司宣传资料', 10);
INSERT INTO `kp_knowledge_item_version` VALUES (42, '', '2026-06-29 15:40:46.722341', '系统管理员', '', '', '售前资料', 14);
INSERT INTO `kp_knowledge_item_version` VALUES (43, '', '2026-07-01 10:13:06.030868', '系统管理员', '', '报价', '重症、手麻、急诊急救标准报价（参考）', 15);
INSERT INTO `kp_knowledge_item_version` VALUES (44, '', '2026-07-01 10:13:06.703854', '系统管理员', '', '报价', '重症、手麻、急诊急救标准报价（参考）', 15);
INSERT INTO `kp_knowledge_item_version` VALUES (45, '', '2026-07-01 10:17:38.619496', '系统管理员', '', '报价', '重症、手麻、急诊急救标准报价（参考）', 15);
INSERT INTO `kp_knowledge_item_version` VALUES (46, '', '2026-07-01 10:19:08.088786', '系统管理员', '', '报价', '重症、手麻、急诊急救标准报价（参考）', 15);
INSERT INTO `kp_knowledge_item_version` VALUES (47, '', '2026-07-01 10:19:18.044925', '系统管理员', '', '报价', '重症、手麻、急诊急救标准报价（参考）', 15);
INSERT INTO `kp_knowledge_item_version` VALUES (48, '吴鹏\n13313541484\n284992818@qq.com\n山西省太原市小店区龙城大街75号鸿泰国际大厦B座9层915室', '2026-09-04 15:10:57.674426', '系统管理员', '', '', '中科兴龙-信息', 17);

-- ----------------------------
-- Table structure for kp_project
-- ----------------------------
DROP TABLE IF EXISTS `kp_project`;
CREATE TABLE `kp_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `acceptance_date` date NULL DEFAULT NULL,
  `acceptance_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contract_amount` decimal(14, 2) NULL DEFAULT NULL,
  `contract_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `customer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `document_owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `implementation_owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `payment_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `planned_end_date` date NULL DEFAULT NULL,
  `progress` int NOT NULL,
  `project_contact_ids` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `project_contact_links` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `project_manager` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `received_amount` decimal(14, 2) NULL DEFAULT NULL,
  `related_contact_notes` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `risk_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sales_owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `start_date` date NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `warranty_until` date NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_project
-- ----------------------------
INSERT INTO `kp_project` VALUES (44, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:36:07.122395', '系统管理员', '长治市中医研究所附属医院', '', '', '', '长治市中医研究所附属医院-手麻重症', '未回款', NULL, 100, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '已完成', '2026-07-03 18:07:40.282324', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (45, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:36:45.746241', '系统管理员', '山阴县人民医院', '', '', '', '山阴县人民医院-手麻', '未回款', NULL, 100, '', '[]', '', 0.00, '', '低', '', '', '未启动', '验收结算', NULL, '已完成', '2026-07-03 17:33:57.198458', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (46, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:37:14.404977', '系统管理员', '清徐县第二人民医院', '', '', '', '清徐县第二人民医院-手麻', '未回款', NULL, 100, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '已完成', '2026-07-03 18:07:43.448975', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (47, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:38:02.745584', '系统管理员', '丹凤县医院', '', '', '', '丹凤县医院-手麻重症', '未回款', NULL, 100, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '已完成', '2026-08-07 19:12:35.245812', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (48, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:38:31.879378', '系统管理员', '永寿县人民医院', '', '', '', '永寿县人民医院-手麻', '未回款', NULL, 95, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '进行中', '2026-08-31 10:32:49.076609', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (49, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:39:45.015862', '系统管理员', '运城市盐湖区人民医院', '', '', '', '盐湖区人民医院-手麻', '未回款', NULL, 90, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '进行中', '2026-08-31 10:32:23.353803', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (50, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:40:07.779309', '系统管理员', '紫阳县人民医院', '', '', '', '紫阳县人民医院-手麻', '未回款', NULL, 95, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '进行中', '2026-08-31 10:32:32.737063', '系统管理员', NULL);
INSERT INTO `kp_project` VALUES (51, NULL, '未开始', 0.00, '未签约', '2026-07-01 15:40:57.033856', '系统管理员', '山西中医药大学附属医院', '', '', '', '中医附-重症', '未回款', NULL, 95, '', '[]', '', 0.00, '', '低', '', '', '未启动', '实施交付', NULL, '进行中', '2026-08-31 10:32:58.049261', '系统管理员', NULL);

-- ----------------------------
-- Table structure for kp_project_activity
-- ----------------------------
DROP TABLE IF EXISTS `kp_project_activity`;
CREATE TABLE `kp_project_activity`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `record_time` datetime(6) NOT NULL,
  `record_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKo0e8fhgxhpesnk2a9dek2ftf`(`project_id` ASC) USING BTREE,
  CONSTRAINT `FKo0e8fhgxhpesnk2a9dek2ftf` FOREIGN KEY (`project_id`) REFERENCES `kp_project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_project_activity
-- ----------------------------

-- ----------------------------
-- Table structure for kp_project_progress_record
-- ----------------------------
DROP TABLE IF EXISTS `kp_project_progress_record`;
CREATE TABLE `kp_project_progress_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `next_action` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `next_action_due_date` date NULL DEFAULT NULL,
  `owner_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `progress` int NOT NULL,
  `record_time` datetime(6) NOT NULL,
  `risk_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `stage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKqy08s1gceud36dxegjpvfg0os`(`project_id` ASC) USING BTREE,
  CONSTRAINT `FKqy08s1gceud36dxegjpvfg0os` FOREIGN KEY (`project_id`) REFERENCES `kp_project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_project_progress_record
-- ----------------------------
INSERT INTO `kp_project_progress_record` VALUES (8, '2026-07-03 17:25:06.821371', '系统管理员', '完成文书调整，科室医疗设备需接入采集平台', '2026-07-10', '梁家璇', 60, '2026-07-03 17:20:49.000000', '低', '实施交付', '进行中', '接口调试：对接卫宁接口已完成\n文书：基础文书已完成，科室提出需要调整，', 51);
INSERT INTO `kp_project_progress_record` VALUES (9, '2026-07-03 17:28:01.411301', '系统管理员', '完善剩余接口及文书', '2026-07-17', '梁家璇', 50, '2026-07-03 17:25:53.000000', '低', '实施交付', '进行中', '接口：完成一半，基础字典完成，手术申请单接口正在调试\n文书：完成一半，剩余文书需医院提供后继续完成', 49);
INSERT INTO `kp_project_progress_record` VALUES (14, '2026-07-03 17:52:02.441657', '系统管理员', '调试出入转接口及特护单调配', '2026-07-17', '梁家璇', 60, '2026-07-03 17:49:09.000000', '低', '实施交付', '进行中', '手麻：\n需实施内容已完成，待培训、上线\n重症：出入转接口正在调试，医嘱执行接口已完成，基础字典已完成\n文书：剩余特护单调配，其余文书均已完成', 47);
INSERT INTO `kp_project_progress_record` VALUES (15, '2026-07-03 17:53:23.266758', '系统管理员', '配合his厂家进行视图调试，检验接口调试', '2026-07-17', '梁家璇', 80, '2026-07-03 17:52:16.000000', '低', '实施交付', '进行中', '接口、文书、采集、培训均已完成，', 48);
INSERT INTO `kp_project_progress_record` VALUES (16, '2026-07-03 17:55:25.384445', '系统管理员', '', NULL, '梁家璇', 75, '2026-07-03 17:53:24.000000', '低', '实施交付', '进行中', '该项目是旧系统更换新系统，接口已完成，文书需医院查验后调整，新增文书需总部研发接入，开发组件进行调试，采集平台等待医院消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (17, '2026-07-13 09:35:58.818654', '系统管理员', '', '2026-07-17', '系统管理员', 60, '2026-07-10 09:34:18.000000', '低', '实施交付', '进行中', '医院院内改造icu网络，下周进行接入采集', 51);
INSERT INTO `kp_project_progress_record` VALUES (18, '2026-07-13 09:36:51.219625', '系统管理员', '', '2026-07-17', '系统管理员', 90, '2026-07-10 09:36:05.000000', '低', '实施交付', '进行中', '预计7.16系统上线', 48);
INSERT INTO `kp_project_progress_record` VALUES (19, '2026-07-13 09:37:34.895923', '系统管理员', '', '2026-07-17', '系统管理员', 70, '2026-07-10 09:36:55.000000', '低', '实施交付', '进行中', '完成接口对接，需要跟科室确认信息，', 47);
INSERT INTO `kp_project_progress_record` VALUES (20, '2026-07-13 09:40:29.619441', '系统管理员', '', '2026-07-17', '系统管理员', 85, '2026-07-10 09:39:41.000000', '低', '实施交付', '进行中', '目前院内正常使用，文书定制正在做，总部排期17号开始做定制文书', 50);
INSERT INTO `kp_project_progress_record` VALUES (21, '2026-07-13 09:42:59.445851', '系统管理员', '', '2026-07-17', '系统管理员', 70, '2026-07-10 09:41:27.000000', '低', '实施交付', '进行中', '基本流程接口已完成，剩余字典类接口调试，文书类已提供的已完成，需调试', 49);
INSERT INTO `kp_project_progress_record` VALUES (22, '2026-07-17 18:22:12.961242', '系统管理员', '等待通知上线时间', '2026-07-24', '梁家璇', 90, '2026-07-17 18:19:12.000000', '低', '实施交付', '进行中', 'his要求统一上线  时间等通知，这周系统性培训，文书调整，测试流程，配置电脑及网络，科里目前正在熟悉系统', 48);
INSERT INTO `kp_project_progress_record` VALUES (23, '2026-07-17 18:24:48.455812', '系统管理员', '需要等待销售的消息才能推进', '2026-07-24', '梁家璇', 85, '2026-07-17 18:22:19.000000', '低', '实施交付', '进行中', '目前暂无进度', 50);
INSERT INTO `kp_project_progress_record` VALUES (24, '2026-07-17 18:27:03.136539', '系统管理员', '接入采集平台', '2026-07-24', '系统管理员', 60, '2026-07-17 18:26:01.000000', '低', '实施交付', '进行中', '这周给可是测试网口，测试接入设备', 51);
INSERT INTO `kp_project_progress_record` VALUES (25, '2026-07-17 18:33:34.467396', '系统管理员', '等采集器到货，正常接入采集平台', '2026-07-24', '梁家璇', 70, '2026-07-17 18:27:10.000000', '低', '实施交付', '进行中', '正在文书微调，采集器已发货，权限正在调整，', 49);
INSERT INTO `kp_project_progress_record` VALUES (26, '2026-07-17 18:36:38.076910', '系统管理员', '需要跟科室沟通', '2026-07-24', '系统管理员', 70, '2026-07-17 18:36:11.000000', '低', '实施交付', '进行中', '暂无进度', 47);
INSERT INTO `kp_project_progress_record` VALUES (27, '2026-07-24 17:43:05.493414', '系统管理员', '预计8月1日上线', '2026-08-01', '梁家璇', 95, '2026-07-24 17:41:05.000000', '低', '实施交付', '进行中', '正在重症文书格式调整', 47);
INSERT INTO `kp_project_progress_record` VALUES (28, '2026-07-24 17:49:05.813620', '系统管理员', '待确定上线日期', '2026-07-31', '梁家璇', 95, '2026-07-24 17:43:08.000000', '低', '实施交付', '进行中', '实施已完成，待上线', 51);
INSERT INTO `kp_project_progress_record` VALUES (29, '2026-07-24 17:49:40.377367', '系统管理员', '需要等待销售消息', '2026-07-31', '梁家璇', 95, '2026-07-24 17:49:17.000000', '低', '实施交付', '进行中', '需要等待销售消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (30, '2026-07-24 17:50:54.729990', '系统管理员', '需要等待his统一上线', '2026-07-31', '梁家璇', 95, '2026-07-24 17:50:13.000000', '低', '实施交付', '进行中', '需要等待his统一上线', 48);
INSERT INTO `kp_project_progress_record` VALUES (31, '2026-07-24 17:58:08.962319', '系统管理员', '与科室确定具体功能及文书，主任提及需新增文书，等待主任通知去现场', '2026-07-31', '梁家璇', 85, '2026-07-24 17:50:58.000000', '低', '实施交付', '进行中', '文书格式调整，与主任确定功能，采集已经全部接完，但采集器设备2台有问题，已经跟总部联系，需要重新发货，到货后，需要等待主任通知后在进行接入。', 49);
INSERT INTO `kp_project_progress_record` VALUES (32, '2026-07-31 17:31:57.618163', '系统管理员', '等待主任通知到现场', '2026-08-07', '系统管理员', 88, '2026-07-31 17:24:35.000000', '低', '实施交付', '进行中', '实施基本完成 接口 剩余给其它系统做的视图 新增文书正在做剩余两个 采集器已发货到公司 需要等医院通知后去现场', 49);
INSERT INTO `kp_project_progress_record` VALUES (33, '2026-07-31 18:18:41.488744', '系统管理员', '8.1日0点正式切换系统', '2026-08-07', '梁家璇', 95, '2026-07-31 18:14:38.000000', '低', '实施交付', '进行中', '重症以培训完成 并且调试 8.1日0点 正式切换系统 \n手麻 科室向院里申请电脑被院长拒绝 先上线重症后 确定手麻系统是否上线', 47);
INSERT INTO `kp_project_progress_record` VALUES (34, '2026-07-31 18:23:21.614611', '系统管理员', '需要等待销售的消息', '2026-08-07', '系统管理员', 95, '2026-07-31 18:23:01.000000', '低', '实施交付', '进行中', '需要等待销售的消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (35, '2026-07-31 18:23:55.765087', '系统管理员', '待确定上线时间', '2026-08-07', '梁家璇', 95, '2026-07-31 18:23:27.000000', '低', '实施交付', '进行中', '待确定上线时间', 51);
INSERT INTO `kp_project_progress_record` VALUES (36, '2026-07-31 18:24:16.337280', '系统管理员', '待确定上线时间', '2026-08-07', '梁家璇', 95, '2026-07-31 18:23:59.000000', '低', '实施交付', '进行中', '待确定上线时间', 48);
INSERT INTO `kp_project_progress_record` VALUES (37, '2026-08-07 19:12:35.186044', '系统管理员', '', NULL, '梁家璇', 100, '2026-08-07 19:11:40.000000', '低', '实施交付', '已完成', '8.1日重症手麻已完成上线', 47);
INSERT INTO `kp_project_progress_record` VALUES (38, '2026-08-07 19:13:19.569043', '系统管理员', '待确定上线日期', '2026-08-14', '梁家璇', 95, '2026-08-07 19:12:43.000000', '低', '实施交付', '进行中', '待确定上线日期', 48);
INSERT INTO `kp_project_progress_record` VALUES (39, '2026-08-07 19:13:48.385188', '系统管理员', '待确定上线日期', '2026-08-14', '梁家璇', 95, '2026-08-07 19:13:34.000000', '低', '实施交付', '进行中', '待确定上线日期', 51);
INSERT INTO `kp_project_progress_record` VALUES (40, '2026-08-07 19:14:07.962411', '系统管理员', '需要等待销售的消息', '2026-08-14', '梁家璇', 95, '2026-08-07 19:13:57.000000', '低', '实施交付', '进行中', '需要等待销售的消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (41, '2026-08-07 19:15:07.471520', '系统管理员', '等主任通知到现场', '2026-08-14', '梁家璇', 90, '2026-08-07 19:14:16.000000', '低', '实施交付', '进行中', '基本实施完成 正在对接其它系统接口', 49);
INSERT INTO `kp_project_progress_record` VALUES (42, '2026-08-17 15:47:44.186123', '系统管理员', '待确定上线日期', '2026-08-21', '梁家璇', 95, '2026-08-14 15:47:26.000000', '低', '实施交付', '进行中', '待确定上线日期', 48);
INSERT INTO `kp_project_progress_record` VALUES (43, '2026-08-17 15:48:11.115648', '系统管理员', '待确定上线日期', '2026-08-21', '梁家璇', 95, '2026-08-14 15:48:01.000000', '低', '实施交付', '进行中', '待确定上线日期', 51);
INSERT INTO `kp_project_progress_record` VALUES (44, '2026-08-17 15:48:36.238278', '系统管理员', '需要等待销售的消息', '2026-08-21', '系统管理员', 95, '2026-08-14 15:48:24.000000', '低', '实施交付', '进行中', '需要等待销售的消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (45, '2026-08-17 16:03:36.490251', '系统管理员', '等待主任通知到现场', '2026-08-21', '梁家璇', 90, '2026-08-14 16:03:05.000000', '低', '实施交付', '进行中', '正在对接其它系统接口', 49);
INSERT INTO `kp_project_progress_record` VALUES (46, '2026-08-21 22:45:43.943156', '系统管理员', '待确定上线日期', '2026-08-28', '梁家璇', 95, '2026-08-21 22:45:20.000000', '低', '实施交付', '进行中', '待确定上线日期', 48);
INSERT INTO `kp_project_progress_record` VALUES (47, '2026-08-21 22:46:15.733940', '系统管理员', '需要等待销售的消息', '2026-08-28', '梁家璇', 95, '2026-08-21 22:45:55.000000', '低', '实施交付', '进行中', '需要等待销售的消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (48, '2026-08-21 22:46:43.255603', '系统管理员', '待确定上线日期', '2026-08-28', '梁家璇', 95, '2026-08-21 22:46:28.000000', '低', '实施交付', '进行中', '待确定上线日期', 51);
INSERT INTO `kp_project_progress_record` VALUES (49, '2026-08-21 22:47:09.658538', '系统管理员', '正在对接其它系统接口', '2026-08-28', '梁家璇', 90, '2026-08-21 22:46:54.000000', '低', '实施交付', '进行中', '正在对接其它系统接口', 49);
INSERT INTO `kp_project_progress_record` VALUES (50, '2026-08-31 10:28:14.918706', '系统管理员', '待确定上线日期', '2026-09-04', '梁家璇', 95, '2026-08-28 10:27:37.000000', '低', '实施交付', '进行中', '待确定上线日期', 51);
INSERT INTO `kp_project_progress_record` VALUES (51, '2026-08-31 10:29:42.578682', '系统管理员', '暂定9.1正式上线', '2026-09-04', '梁家璇', 95, '2026-08-28 10:28:36.000000', '低', '实施交付', '进行中', '暂定9.1正式上线', 48);
INSERT INTO `kp_project_progress_record` VALUES (52, '2026-08-31 10:29:55.296826', '系统管理员', '需要等待销售的消息', '2026-09-04', '梁家璇', 95, '2026-08-28 10:29:47.000000', '低', '实施交付', '进行中', '需要等待销售的消息', 50);
INSERT INTO `kp_project_progress_record` VALUES (53, '2026-08-31 10:31:29.626731', '系统管理员', '基本调试完毕 需要确认上线日期', '2026-09-04', '梁家璇', 90, '2026-08-28 10:31:21.000000', '低', '实施交付', '进行中', '基本调试完毕 需要确认上线日期', 49);

-- ----------------------------
-- Table structure for kp_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `kp_role_permission`;
CREATE TABLE `kp_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `can_create_content` bit(1) NOT NULL,
  `can_delete_content` bit(1) NOT NULL,
  `can_edit_content` bit(1) NOT NULL,
  `can_manage_categories` bit(1) NOT NULL,
  `can_manage_roles` bit(1) NOT NULL,
  `can_manage_users` bit(1) NOT NULL,
  `can_preview_office` bit(1) NOT NULL,
  `can_view_library` bit(1) NOT NULL,
  `role` enum('ADMIN','DELIVERY_OPS','FINANCE','PRESALES','REVIEWER','SALES','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK5c57w51knvnpjx2ai9xpq9pg4`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_role_permission
-- ----------------------------
INSERT INTO `kp_role_permission` VALUES (1, b'1', b'1', b'1', b'1', b'1', b'1', b'1', b'1', 'ADMIN', '2026-04-21 22:36:18.635529');
INSERT INTO `kp_role_permission` VALUES (2, b'1', b'1', b'1', b'0', b'0', b'0', b'1', b'1', 'SALES', '2026-04-21 22:36:18.765557');
INSERT INTO `kp_role_permission` VALUES (3, b'1', b'1', b'1', b'0', b'0', b'0', b'1', b'1', 'PRESALES', '2026-04-21 22:36:18.779559');
INSERT INTO `kp_role_permission` VALUES (4, b'1', b'1', b'1', b'0', b'0', b'0', b'1', b'1', 'DELIVERY_OPS', '2026-04-21 22:36:18.789562');
INSERT INTO `kp_role_permission` VALUES (5, b'0', b'0', b'0', b'0', b'0', b'0', b'1', b'1', 'FINANCE', '2026-04-21 22:36:18.805568');
INSERT INTO `kp_role_permission` VALUES (6, b'1', b'0', b'0', b'0', b'0', b'0', b'1', b'1', 'USER', '2026-04-21 22:36:18.814569');
INSERT INTO `kp_role_permission` VALUES (7, b'1', b'1', b'1', b'0', b'0', b'0', b'1', b'1', 'REVIEWER', '2026-04-21 22:36:18.823571');

-- ----------------------------
-- Table structure for kp_training_record
-- ----------------------------
DROP TABLE IF EXISTS `kp_training_record`;
CREATE TABLE `kp_training_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attachment_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `participant_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `remarks` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `trainer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `training_date` datetime(6) NOT NULL,
  `training_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_training_record
-- ----------------------------
INSERT INTO `kp_training_record` VALUES (1, NULL, '手麻产品讲解', '2026-06-03 12:53:34.107985', '系统管理员', '', '手麻产品讲解', '手麻产品讲解', '白利利', '2026-06-02 20:53:00.000000', '会议', '2026-06-03 12:53:34.107985', '系统管理员');

-- ----------------------------
-- Table structure for kp_user_account
-- ----------------------------
DROP TABLE IF EXISTS `kp_user_account`;
CREATE TABLE `kp_user_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enabled` bit(1) NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` enum('ADMIN','DELIVERY_OPS','FINANCE','PRESALES','REVIEWER','SALES','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKsuqqvj2djw40a6c8clmfso9ew`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kp_user_account
-- ----------------------------
INSERT INTO `kp_user_account` VALUES (1, '2026-04-21 22:07:16.547358', '系统管理员', b'1', '$2a$10$KKtaOiwajucVvdnQ7.DkDuk5lYhqi73IybzqEb.flFGldcil7OqP.', 'ADMIN', 'admin');
INSERT INTO `kp_user_account` VALUES (6, '2026-06-29 09:26:04.578190', '赵立文', b'1', '$2a$10$4pcIkvGMbpOTQY9qWwx1uOqrGI0i/QPcao6E2Bsc8uKiD5Zbz9iAe', 'ADMIN', 'zlw');
INSERT INTO `kp_user_account` VALUES (7, '2026-06-29 09:26:39.132712', '梁相', b'1', '$2a$10$ap1cp/2Z88qS2NevSXJNu.laetQKoAJxpaEp4VDR1w/43pgbd9q5O', 'ADMIN', 'lx');
INSERT INTO `kp_user_account` VALUES (8, '2026-06-29 09:27:03.876313', '李龙强', b'1', '$2a$10$L4R6Si6FItXGEIj84WtOy.5p5QzFUpgrVnYt7aYBafp06cGhC5sFK', 'ADMIN', 'llq');
INSERT INTO `kp_user_account` VALUES (9, '2026-06-29 09:27:20.937773', '梁家璇', b'1', '$2a$10$KBEyFDVvi7aTJG78GBT7NuXseDTtsZiWXEa8Tho9ZC7OjjfMgIlSK', 'ADMIN', 'llj');
INSERT INTO `kp_user_account` VALUES (10, '2026-06-29 09:27:33.843558', '曹亚利', b'1', '$2a$10$yHCXYDH.ty6dT0cuBihFbeLQ77MNsARypCL5mBZgHuBngBhyAg7Lq', 'ADMIN', 'cyl');
INSERT INTO `kp_user_account` VALUES (11, '2026-06-29 09:27:43.453667', '董思涵', b'1', '$2a$10$LRz5FpI.f/xu3lxc1mG/GOp./ko1YGK5ZsBWCPSGZp7vp1PaiHciK', 'ADMIN', 'dsh');
INSERT INTO `kp_user_account` VALUES (12, '2026-06-29 09:28:08.531816', '白利利', b'1', '$2a$10$aEXOf/ojUk406rynzifeVe4AQ.waOXoNguaM0GhWEIkk92YsYBe.C', 'ADMIN', 'bll');
INSERT INTO `kp_user_account` VALUES (13, '2026-06-29 09:28:23.354196', '陈星宇', b'1', '$2a$10$j4ux99pJUYiLhK2hVAWQdO66XM2lLXjYnoZJELkMuHwqJkbhmk6Ze', 'ADMIN', 'cxy');
INSERT INTO `kp_user_account` VALUES (14, '2026-06-29 09:28:44.369692', '李雪健', b'1', '$2a$10$E1US2iuZOZdc.Sdq3ywCUu8n6ckrJC.fdBaz3IednWKv3ES724fWe', 'ADMIN', 'lxj');
INSERT INTO `kp_user_account` VALUES (15, '2026-06-29 09:29:01.794290', '种博辉', b'1', '$2a$10$erMmjTuTGnrGL205/EkqcuZGhOo3qGm0Wzl5q9sFkdhrAkBoa3Mia', 'ADMIN', 'cbh');
INSERT INTO `kp_user_account` VALUES (16, '2026-06-29 09:29:21.298375', '栗丽萍', b'1', '$2a$10$l/IJ7hIVZQ8cdSHbL2Y9zO7WjfeebGZAoTbseSzHYusTAyHaREIsa', 'ADMIN', 'llp');
INSERT INTO `kp_user_account` VALUES (17, '2026-06-29 09:29:31.756391', '刘芳', b'1', '$2a$10$FrLZXmeKqlPbiCaBNxV/oONOyHFfDWr.SDpHUy89jH8HKnWv1m/R.', 'ADMIN', 'lf');
INSERT INTO `kp_user_account` VALUES (18, '2026-06-29 09:29:42.412654', '周文杰', b'1', '$2a$10$FA.xG4IWpTkPn1QewzUO/e5V4LISesG/s4/vOdvCnG0i6okSnr8h.', 'ADMIN', 'zwj');
INSERT INTO `kp_user_account` VALUES (19, '2026-06-29 09:30:08.970672', '王瑞会', b'1', '$2a$10$Zh6O69G5Z0kq4EFO4J6JKuO49ez9MUotVLbMBuswsWvtPULXXKG1a', 'ADMIN', 'wrh');
INSERT INTO `kp_user_account` VALUES (20, '2026-06-29 11:32:23.782281', 'test', b'1', '$2a$10$kONAUauNkDkZ4bP0mbasPunfzbumnmjE4LyKUxSJFPkn8snRsvCiW', 'SALES', 'test');

-- ----------------------------
-- Table structure for training_chapter
-- ----------------------------
DROP TABLE IF EXISTS `training_chapter`;
CREATE TABLE `training_chapter`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `content_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `duration` int NOT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `course_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK4sibt2p30p3l36gfs4w2h11i8`(`course_id` ASC) USING BTREE,
  CONSTRAINT `FK4sibt2p30p3l36gfs4w2h11i8` FOREIGN KEY (`course_id`) REFERENCES `training_course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_chapter
-- ----------------------------

-- ----------------------------
-- Table structure for training_course
-- ----------------------------
DROP TABLE IF EXISTS `training_course`;
CREATE TABLE `training_course`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `chapter_count` int NOT NULL,
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `duration` int NOT NULL,
  `level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `published` bit(1) NOT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_course
-- ----------------------------
INSERT INTO `training_course` VALUES (1, '售前培训', 0, NULL, '2026-04-24 14:06:00.810316', '系统管理员', '', 30, '初级', b'1', 0, '手麻售前PPT培训', '2026-04-24 14:06:00.810316', '系统管理员');

-- ----------------------------
-- Table structure for training_plan
-- ----------------------------
DROP TABLE IF EXISTS `training_plan`;
CREATE TABLE `training_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `end_date` date NOT NULL,
  `mandatory` bit(1) NOT NULL,
  `start_date` date NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `course_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK6jv96xdei24kpky28qxswhhgc`(`course_id` ASC) USING BTREE,
  CONSTRAINT `FK6jv96xdei24kpky28qxswhhgc` FOREIGN KEY (`course_id`) REFERENCES `training_course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_plan
-- ----------------------------
INSERT INTO `training_plan` VALUES (1, '销售培训', '2026-04-24 14:08:29.065947', '系统管理员', '', '2026-04-24', b'1', '2026-04-24', 'active', 'ALL', '手麻售前培训', '2026-04-24 14:08:29.065947', '系统管理员', NULL);

-- ----------------------------
-- Table structure for training_plan_users
-- ----------------------------
DROP TABLE IF EXISTS `training_plan_users`;
CREATE TABLE `training_plan_users`  (
  `plan_id` bigint NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  INDEX `FKqchdaptaq51hoh57ahacb2u0h`(`plan_id` ASC) USING BTREE,
  CONSTRAINT `FKqchdaptaq51hoh57ahacb2u0h` FOREIGN KEY (`plan_id`) REFERENCES `training_plan` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_plan_users
-- ----------------------------

-- ----------------------------
-- Table structure for training_record
-- ----------------------------
DROP TABLE IF EXISTS `training_record`;
CREATE TABLE `training_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `complete_time` datetime(6) NULL DEFAULT NULL,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `duration` int NOT NULL,
  `progress` int NOT NULL,
  `start_time` datetime(6) NULL DEFAULT NULL,
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `chapter_id` bigint NULL DEFAULT NULL,
  `course_id` bigint NOT NULL,
  `plan_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKtdauib20lj6if2vggvhfuh35m`(`chapter_id` ASC) USING BTREE,
  INDEX `FK46kkwu3bpgi9o7ia1ug8w3mmr`(`course_id` ASC) USING BTREE,
  INDEX `FKto3xvsru7jchlkt3751xjl8kp`(`plan_id` ASC) USING BTREE,
  CONSTRAINT `FK46kkwu3bpgi9o7ia1ug8w3mmr` FOREIGN KEY (`course_id`) REFERENCES `training_course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKtdauib20lj6if2vggvhfuh35m` FOREIGN KEY (`chapter_id`) REFERENCES `training_chapter` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKto3xvsru7jchlkt3751xjl8kp` FOREIGN KEY (`plan_id`) REFERENCES `training_plan` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_record
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
