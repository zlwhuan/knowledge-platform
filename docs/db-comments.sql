-- knowledge_platform 表/字段中文注释
-- 仅修改 COMMENT，不改结构、不改数据
-- 执行前请确认已连接 knowledge_platform

-- ========== exam_paper ==========
ALTER TABLE `exam_paper` COMMENT = '考试试卷';
ALTER TABLE `exam_paper` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `exam_paper` MODIFY COLUMN `category` varchar(50) NULL DEFAULT NULL COMMENT '考试分类';
ALTER TABLE `exam_paper` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `exam_paper` MODIFY COLUMN `created_by` varchar(100) NOT NULL COMMENT '创建人';
ALTER TABLE `exam_paper` MODIFY COLUMN `description` varchar(2000) NULL DEFAULT NULL COMMENT '试卷说明';
ALTER TABLE `exam_paper` MODIFY COLUMN `duration` int NOT NULL COMMENT '考试时长（分钟）';
ALTER TABLE `exam_paper` MODIFY COLUMN `enabled` bit(1) NOT NULL COMMENT '是否启用';
ALTER TABLE `exam_paper` MODIFY COLUMN `pass_score` int NOT NULL COMMENT '及格分数';
ALTER TABLE `exam_paper` MODIFY COLUMN `question_count` int NOT NULL COMMENT '题目数量';
ALTER TABLE `exam_paper` MODIFY COLUMN `question_ids` text NULL COMMENT '题目ID列表（JSON）';
ALTER TABLE `exam_paper` MODIFY COLUMN `random_order` bit(1) NOT NULL COMMENT '是否乱序出题';
ALTER TABLE `exam_paper` MODIFY COLUMN `show_answer` bit(1) NOT NULL COMMENT '是否显示答案';
ALTER TABLE `exam_paper` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '试卷标题';
ALTER TABLE `exam_paper` MODIFY COLUMN `total_score` int NOT NULL COMMENT '试卷总分';
ALTER TABLE `exam_paper` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `exam_paper` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';

-- ========== exam_question ==========
ALTER TABLE `exam_question` COMMENT = '考试题目';
ALTER TABLE `exam_question` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `exam_question` MODIFY COLUMN `answer` text NULL COMMENT '正确答案';
ALTER TABLE `exam_question` MODIFY COLUMN `category` varchar(50) NOT NULL COMMENT '题目分类';
ALTER TABLE `exam_question` MODIFY COLUMN `content` text NOT NULL COMMENT '题目内容';
ALTER TABLE `exam_question` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `exam_question` MODIFY COLUMN `created_by` varchar(100) NOT NULL COMMENT '创建人';
ALTER TABLE `exam_question` MODIFY COLUMN `difficulty` varchar(50) NOT NULL COMMENT '难度';
ALTER TABLE `exam_question` MODIFY COLUMN `enabled` bit(1) NOT NULL COMMENT '是否启用';
ALTER TABLE `exam_question` MODIFY COLUMN `explanation` text NULL COMMENT '答案解析';
ALTER TABLE `exam_question` MODIFY COLUMN `options` text NULL COMMENT '选项列表（JSON）';
ALTER TABLE `exam_question` MODIFY COLUMN `score` int NOT NULL COMMENT '分值';
ALTER TABLE `exam_question` MODIFY COLUMN `type` varchar(50) NOT NULL COMMENT '题型';
ALTER TABLE `exam_question` MODIFY COLUMN `use_count` int NOT NULL COMMENT '被引用次数';

-- ========== exam_record ==========
ALTER TABLE `exam_record` COMMENT = '考试作答记录';
ALTER TABLE `exam_record` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `exam_record` MODIFY COLUMN `answers` text NULL COMMENT '作答内容（JSON）';
ALTER TABLE `exam_record` MODIFY COLUMN `correct_count` int NOT NULL COMMENT '答对题数';
ALTER TABLE `exam_record` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `exam_record` MODIFY COLUMN `display_name` varchar(100) NOT NULL COMMENT '考生姓名';
ALTER TABLE `exam_record` MODIFY COLUMN `duration` int NULL DEFAULT NULL COMMENT '答题用时（秒）';
ALTER TABLE `exam_record` MODIFY COLUMN `passed` bit(1) NOT NULL COMMENT '是否及格';
ALTER TABLE `exam_record` MODIFY COLUMN `score` int NOT NULL COMMENT '得分';
ALTER TABLE `exam_record` MODIFY COLUMN `start_time` datetime(6) NULL DEFAULT NULL COMMENT '开考时间';
ALTER TABLE `exam_record` MODIFY COLUMN `status` varchar(50) NOT NULL COMMENT '作答状态';
ALTER TABLE `exam_record` MODIFY COLUMN `submit_time` datetime(6) NULL DEFAULT NULL COMMENT '交卷时间';
ALTER TABLE `exam_record` MODIFY COLUMN `total_questions` int NOT NULL COMMENT '总题数';
ALTER TABLE `exam_record` MODIFY COLUMN `username` varchar(100) NOT NULL COMMENT '考生账号';
ALTER TABLE `exam_record` MODIFY COLUMN `paper_id` bigint NOT NULL COMMENT '试卷ID';

-- ========== kp_assessment_record ==========
ALTER TABLE `kp_assessment_record` COMMENT = '培训考核记录';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `assessment_date` datetime(6) NOT NULL COMMENT '考核日期';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `assessment_type` varchar(50) NULL DEFAULT NULL COMMENT '考核类型';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `assessor_ids` text NULL COMMENT '考核人ID列表';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `created_by` varchar(100) NOT NULL COMMENT '创建人';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `evaluation` text NULL COMMENT '考核评语';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `grade` varchar(10) NULL DEFAULT NULL COMMENT '考核等级';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '考核标题';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `training_record_id` bigint NULL DEFAULT NULL COMMENT '关联培训记录ID';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_assessment_record` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';

-- ========== kp_attachment ==========
ALTER TABLE `kp_attachment` COMMENT = '知识条目附件';
ALTER TABLE `kp_attachment` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_attachment` MODIFY COLUMN `content_type` varchar(255) NULL DEFAULT NULL COMMENT '文件 MIME 类型';
ALTER TABLE `kp_attachment` MODIFY COLUMN `file_path` varchar(500) NOT NULL COMMENT '文件存储路径';
ALTER TABLE `kp_attachment` MODIFY COLUMN `file_size` bigint NOT NULL COMMENT '文件大小（字节）';
ALTER TABLE `kp_attachment` MODIFY COLUMN `original_file_name` varchar(255) NOT NULL COMMENT '原始文件名';
ALTER TABLE `kp_attachment` MODIFY COLUMN `stored_file_name` varchar(255) NOT NULL COMMENT '服务器存储文件名';
ALTER TABLE `kp_attachment` MODIFY COLUMN `uploaded_at` datetime(6) NOT NULL COMMENT '上传时间';
ALTER TABLE `kp_attachment` MODIFY COLUMN `uploaded_by` varchar(100) NOT NULL COMMENT '上传人';
ALTER TABLE `kp_attachment` MODIFY COLUMN `item_id` bigint NOT NULL COMMENT '所属知识条目ID';

-- ========== kp_category ==========
ALTER TABLE `kp_category` COMMENT = '知识库分类树';
ALTER TABLE `kp_category` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_category` MODIFY COLUMN `code` varchar(100) NULL DEFAULT NULL COMMENT '分类编码';
ALTER TABLE `kp_category` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_category` MODIFY COLUMN `description` varchar(500) NULL DEFAULT NULL COMMENT '分类说明';
ALTER TABLE `kp_category` MODIFY COLUMN `name` varchar(100) NOT NULL COMMENT '分类名称（全表唯一）';
ALTER TABLE `kp_category` MODIFY COLUMN `sort_order` int NOT NULL COMMENT '排序号';
ALTER TABLE `kp_category` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_category` MODIFY COLUMN `parent_id` bigint NULL DEFAULT NULL COMMENT '父分类ID（空为顶级）';

-- ========== kp_customer_company ==========
ALTER TABLE `kp_customer_company` COMMENT = '客户公司';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `address` varchar(300) NULL DEFAULT NULL COMMENT '地址';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `cooperation_stage` varchar(50) NULL DEFAULT NULL COMMENT '合作阶段';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `customer_type` varchar(100) NULL DEFAULT NULL COMMENT '客户类型';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `email` varchar(200) NULL DEFAULT NULL COMMENT '邮箱';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `industry` varchar(100) NULL DEFAULT NULL COMMENT '所属行业';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `level` varchar(100) NULL DEFAULT NULL COMMENT '客户等级';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `main_phone` varchar(100) NULL DEFAULT NULL COMMENT '主要电话';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `name` varchar(200) NOT NULL COMMENT '客户名称';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `notes` varchar(2000) NULL DEFAULT NULL COMMENT '备注';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `owner_name` varchar(100) NULL DEFAULT NULL COMMENT '负责人';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `region` varchar(100) NULL DEFAULT NULL COMMENT '区域';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `short_name` varchar(100) NULL DEFAULT NULL COMMENT '客户简称';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `source` varchar(100) NULL DEFAULT NULL COMMENT '客户来源';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `status` varchar(50) NULL DEFAULT NULL COMMENT '客户状态';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `tags` varchar(1000) NULL DEFAULT NULL COMMENT '标签';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';
ALTER TABLE `kp_customer_company` MODIFY COLUMN `website` varchar(100) NULL DEFAULT NULL COMMENT '官网';

-- ========== kp_customer_contact ==========
ALTER TABLE `kp_customer_contact` COMMENT = '客户联系人';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `decision_level` varchar(20) NULL DEFAULT NULL COMMENT '决策层级';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `department` varchar(50) NULL DEFAULT NULL COMMENT '部门';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `email` varchar(200) NULL DEFAULT NULL COMMENT '邮箱';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `gender` varchar(20) NULL DEFAULT NULL COMMENT '性别';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `mobile` varchar(100) NULL DEFAULT NULL COMMENT '手机';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `name` varchar(100) NOT NULL COMMENT '姓名';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `notes` varchar(2000) NULL DEFAULT NULL COMMENT '备注';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `office_phone` varchar(100) NULL DEFAULT NULL COMMENT '办公电话';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `position` varchar(100) NULL DEFAULT NULL COMMENT '职位';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `primary_contact` bit(1) NULL DEFAULT NULL COMMENT '是否主联系人';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `qq` varchar(100) NULL DEFAULT NULL COMMENT 'QQ';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `wechat` varchar(100) NULL DEFAULT NULL COMMENT '微信';
ALTER TABLE `kp_customer_contact` MODIFY COLUMN `customer_id` bigint NOT NULL COMMENT '所属客户公司ID';

-- ========== kp_customer_followup ==========
ALTER TABLE `kp_customer_followup` COMMENT = '客户跟进记录';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `content` varchar(2000) NOT NULL COMMENT '跟进内容';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `followup_time` datetime(6) NOT NULL COMMENT '跟进时间';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `followup_type` varchar(50) NOT NULL COMMENT '跟进方式';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `next_followup_date` date NULL DEFAULT NULL COMMENT '下次跟进日期';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `owner_name` varchar(100) NULL DEFAULT NULL COMMENT '跟进人';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `project_id` bigint NULL DEFAULT NULL COMMENT '关联项目ID（冗余）';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `project_name` varchar(200) NULL DEFAULT NULL COMMENT '关联项目名称（冗余）';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `result_level` varchar(50) NULL DEFAULT NULL COMMENT '跟进结果';
ALTER TABLE `kp_customer_followup` MODIFY COLUMN `customer_id` bigint NOT NULL COMMENT '客户公司ID';

-- ========== kp_dictionary_item ==========
ALTER TABLE `kp_dictionary_item` COMMENT = '数据字典项';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `dict_type` varchar(100) NOT NULL COMMENT '字典类型编码';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `enabled` bit(1) NOT NULL COMMENT '是否启用';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `item_label` varchar(100) NOT NULL COMMENT '显示标签';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `item_value` varchar(100) NOT NULL COMMENT '字典值';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `remark` varchar(500) NULL DEFAULT NULL COMMENT '备注';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `sort_order` int NOT NULL COMMENT '排序号';
ALTER TABLE `kp_dictionary_item` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';

-- ========== kp_knowledge_item ==========
ALTER TABLE `kp_knowledge_item` COMMENT = '知识条目';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `content_markdown` text NULL COMMENT '正文（Markdown）';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `operation_log` text NULL COMMENT '操作日志';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `review_comment` varchar(1000) NULL DEFAULT NULL COMMENT '审核意见';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `source` varchar(200) NULL DEFAULT NULL COMMENT '资料来源';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `status` varchar(50) NULL DEFAULT NULL COMMENT '条目状态';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `summary` varchar(1000) NULL DEFAULT NULL COMMENT '摘要';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `tags` varchar(500) NULL DEFAULT NULL COMMENT '标签';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '标题';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `type` varchar(50) NOT NULL COMMENT '资料类型';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `category_id` bigint NOT NULL COMMENT '所属分类ID';
ALTER TABLE `kp_knowledge_item` MODIFY COLUMN `project_id` bigint NULL DEFAULT NULL COMMENT '关联项目ID';

-- ========== kp_knowledge_item_version ==========
ALTER TABLE `kp_knowledge_item_version` COMMENT = '知识条目版本快照';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `content_markdown` text NULL COMMENT '正文快照（Markdown）';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '快照时间';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `created_by` varchar(100) NOT NULL COMMENT '操作人';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `summary` varchar(1000) NULL DEFAULT NULL COMMENT '摘要快照';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `tags` varchar(500) NULL DEFAULT NULL COMMENT '标签快照';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '标题快照';
ALTER TABLE `kp_knowledge_item_version` MODIFY COLUMN `item_id` bigint NOT NULL COMMENT '所属知识条目ID';

-- ========== kp_project ==========
ALTER TABLE `kp_project` COMMENT = '项目台账';
ALTER TABLE `kp_project` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_project` MODIFY COLUMN `acceptance_date` date NULL DEFAULT NULL COMMENT '验收日期';
ALTER TABLE `kp_project` MODIFY COLUMN `acceptance_status` varchar(50) NULL DEFAULT NULL COMMENT '验收状态';
ALTER TABLE `kp_project` MODIFY COLUMN `contract_amount` decimal(14, 2) NULL DEFAULT NULL COMMENT '合同金额';
ALTER TABLE `kp_project` MODIFY COLUMN `contract_status` varchar(50) NULL DEFAULT NULL COMMENT '签约状态';
ALTER TABLE `kp_project` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_project` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_project` MODIFY COLUMN `customer_name` varchar(200) NOT NULL COMMENT '客户名称（冗余）';
ALTER TABLE `kp_project` MODIFY COLUMN `description` varchar(2000) NULL DEFAULT NULL COMMENT '项目描述';
ALTER TABLE `kp_project` MODIFY COLUMN `document_owner` varchar(100) NULL DEFAULT NULL COMMENT '文档负责人';
ALTER TABLE `kp_project` MODIFY COLUMN `implementation_owner` varchar(100) NULL DEFAULT NULL COMMENT '实施负责人';
ALTER TABLE `kp_project` MODIFY COLUMN `name` varchar(200) NOT NULL COMMENT '项目名称';
ALTER TABLE `kp_project` MODIFY COLUMN `payment_status` varchar(50) NULL DEFAULT NULL COMMENT '回款状态';
ALTER TABLE `kp_project` MODIFY COLUMN `planned_end_date` date NULL DEFAULT NULL COMMENT '计划结束日期';
ALTER TABLE `kp_project` MODIFY COLUMN `progress` int NOT NULL COMMENT '进度百分比';
ALTER TABLE `kp_project` MODIFY COLUMN `project_contact_ids` varchar(1000) NULL DEFAULT NULL COMMENT '项目联系人ID列表';
ALTER TABLE `kp_project` MODIFY COLUMN `project_contact_links` varchar(4000) NULL DEFAULT NULL COMMENT '项目联系人链接';
ALTER TABLE `kp_project` MODIFY COLUMN `project_manager` varchar(100) NULL DEFAULT NULL COMMENT '项目经理';
ALTER TABLE `kp_project` MODIFY COLUMN `received_amount` decimal(14, 2) NULL DEFAULT NULL COMMENT '已回款金额';
ALTER TABLE `kp_project` MODIFY COLUMN `related_contact_notes` varchar(2000) NULL DEFAULT NULL COMMENT '关联联系人备注';
ALTER TABLE `kp_project` MODIFY COLUMN `risk_level` varchar(50) NULL DEFAULT NULL COMMENT '风险等级';
ALTER TABLE `kp_project` MODIFY COLUMN `sales_owner` varchar(100) NULL DEFAULT NULL COMMENT '销售负责人';
ALTER TABLE `kp_project` MODIFY COLUMN `service_owner` varchar(100) NULL DEFAULT NULL COMMENT '售后负责人';
ALTER TABLE `kp_project` MODIFY COLUMN `service_status` varchar(50) NULL DEFAULT NULL COMMENT '维保状态';
ALTER TABLE `kp_project` MODIFY COLUMN `stage` varchar(50) NOT NULL COMMENT '项目阶段';
ALTER TABLE `kp_project` MODIFY COLUMN `start_date` date NULL DEFAULT NULL COMMENT '开始日期';
ALTER TABLE `kp_project` MODIFY COLUMN `status` varchar(50) NOT NULL COMMENT '项目状态';
ALTER TABLE `kp_project` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_project` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';
ALTER TABLE `kp_project` MODIFY COLUMN `warranty_until` date NULL DEFAULT NULL COMMENT '质保截止日期';

-- ========== kp_project_activity ==========
ALTER TABLE `kp_project_activity` COMMENT = '项目动态';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `content` varchar(1000) NOT NULL COMMENT '动态内容';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `owner_name` varchar(100) NULL DEFAULT NULL COMMENT '记录人';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `record_time` datetime(6) NOT NULL COMMENT '发生时间';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `record_type` varchar(50) NOT NULL COMMENT '动态类型';
ALTER TABLE `kp_project_activity` MODIFY COLUMN `project_id` bigint NOT NULL COMMENT '项目ID';

-- ========== kp_project_progress_record ==========
ALTER TABLE `kp_project_progress_record` COMMENT = '项目进度记录';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `next_action` varchar(500) NULL DEFAULT NULL COMMENT '下一步行动';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `next_action_due_date` date NULL DEFAULT NULL COMMENT '行动截止日期';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `owner_name` varchar(100) NULL DEFAULT NULL COMMENT '负责人';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `progress` int NOT NULL COMMENT '进度百分比';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `record_time` datetime(6) NOT NULL COMMENT '记录时间';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `risk_level` varchar(50) NULL DEFAULT NULL COMMENT '风险等级';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `stage` varchar(50) NOT NULL COMMENT '项目阶段';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `status` varchar(50) NOT NULL COMMENT '进度状态';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `summary` varchar(1000) NOT NULL COMMENT '进度摘要';
ALTER TABLE `kp_project_progress_record` MODIFY COLUMN `project_id` bigint NOT NULL COMMENT '项目ID';

-- ========== kp_role_permission ==========
ALTER TABLE `kp_role_permission` COMMENT = '角色权限配置';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_create_content` bit(1) NOT NULL COMMENT '允许创建知识内容';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_delete_content` bit(1) NOT NULL COMMENT '允许删除知识内容';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_edit_content` bit(1) NOT NULL COMMENT '允许编辑知识内容';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_manage_categories` bit(1) NOT NULL COMMENT '允许管理分类';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_manage_roles` bit(1) NOT NULL COMMENT '允许管理角色权限';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_manage_users` bit(1) NOT NULL COMMENT '允许管理用户';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_preview_office` bit(1) NOT NULL COMMENT '允许预览 Office 附件';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `can_view_library` bit(1) NOT NULL COMMENT '允许查看知识库';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `role` enum('ADMIN','DELIVERY_OPS','FINANCE','PRESALES','REVIEWER','SALES','USER') NOT NULL COMMENT '角色';
ALTER TABLE `kp_role_permission` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';

-- ========== kp_training_record ==========
ALTER TABLE `kp_training_record` COMMENT = '培训记录';
ALTER TABLE `kp_training_record` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_training_record` MODIFY COLUMN `attachment_ids` text NULL COMMENT '附件ID列表';
ALTER TABLE `kp_training_record` MODIFY COLUMN `content` text NULL COMMENT '培训内容';
ALTER TABLE `kp_training_record` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_training_record` MODIFY COLUMN `created_by` varchar(100) NOT NULL COMMENT '创建人';
ALTER TABLE `kp_training_record` MODIFY COLUMN `participant_ids` text NULL COMMENT '参训人ID列表';
ALTER TABLE `kp_training_record` MODIFY COLUMN `remarks` varchar(1000) NULL DEFAULT NULL COMMENT '备注';
ALTER TABLE `kp_training_record` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '培训标题';
ALTER TABLE `kp_training_record` MODIFY COLUMN `trainer` varchar(100) NULL DEFAULT NULL COMMENT '培训讲师';
ALTER TABLE `kp_training_record` MODIFY COLUMN `training_date` datetime(6) NOT NULL COMMENT '培训日期';
ALTER TABLE `kp_training_record` MODIFY COLUMN `training_type` varchar(50) NULL DEFAULT NULL COMMENT '培训类型';
ALTER TABLE `kp_training_record` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `kp_training_record` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';

-- ========== kp_user_account ==========
ALTER TABLE `kp_user_account` COMMENT = '系统用户账号';
ALTER TABLE `kp_user_account` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `kp_user_account` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `kp_user_account` MODIFY COLUMN `display_name` varchar(100) NOT NULL COMMENT '显示姓名';
ALTER TABLE `kp_user_account` MODIFY COLUMN `enabled` bit(1) NOT NULL COMMENT '是否启用';
ALTER TABLE `kp_user_account` MODIFY COLUMN `password_hash` varchar(255) NOT NULL COMMENT '密码哈希';
ALTER TABLE `kp_user_account` MODIFY COLUMN `role` enum('ADMIN','DELIVERY_OPS','FINANCE','PRESALES','REVIEWER','SALES','USER') NOT NULL COMMENT '角色';
ALTER TABLE `kp_user_account` MODIFY COLUMN `username` varchar(50) NOT NULL COMMENT '登录账号（唯一）';

-- ========== training_chapter ==========
ALTER TABLE `training_chapter` COMMENT = '培训课程章节';
ALTER TABLE `training_chapter` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `training_chapter` MODIFY COLUMN `content` text NULL COMMENT '章节内容';
ALTER TABLE `training_chapter` MODIFY COLUMN `content_url` varchar(1000) NULL DEFAULT NULL COMMENT '内容链接';
ALTER TABLE `training_chapter` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `training_chapter` MODIFY COLUMN `duration` int NOT NULL COMMENT '时长（分钟）';
ALTER TABLE `training_chapter` MODIFY COLUMN `sort_order` int NOT NULL COMMENT '排序号';
ALTER TABLE `training_chapter` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '章节标题';
ALTER TABLE `training_chapter` MODIFY COLUMN `type` varchar(50) NULL DEFAULT NULL COMMENT '章节类型';
ALTER TABLE `training_chapter` MODIFY COLUMN `course_id` bigint NOT NULL COMMENT '课程ID';

-- ========== training_course ==========
ALTER TABLE `training_course` COMMENT = '培训课程';
ALTER TABLE `training_course` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `training_course` MODIFY COLUMN `category` varchar(50) NULL DEFAULT NULL COMMENT '课程分类';
ALTER TABLE `training_course` MODIFY COLUMN `chapter_count` int NOT NULL COMMENT '章节数';
ALTER TABLE `training_course` MODIFY COLUMN `cover_url` varchar(500) NULL DEFAULT NULL COMMENT '封面链接';
ALTER TABLE `training_course` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `training_course` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `training_course` MODIFY COLUMN `description` varchar(2000) NULL DEFAULT NULL COMMENT '课程说明';
ALTER TABLE `training_course` MODIFY COLUMN `duration` int NOT NULL COMMENT '总时长（分钟）';
ALTER TABLE `training_course` MODIFY COLUMN `level` varchar(50) NULL DEFAULT NULL COMMENT '难度级别';
ALTER TABLE `training_course` MODIFY COLUMN `published` bit(1) NOT NULL COMMENT '是否发布';
ALTER TABLE `training_course` MODIFY COLUMN `sort_order` int NOT NULL COMMENT '排序号';
ALTER TABLE `training_course` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '课程标题';
ALTER TABLE `training_course` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `training_course` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';

-- ========== training_plan ==========
ALTER TABLE `training_plan` COMMENT = '培训计划';
ALTER TABLE `training_plan` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `training_plan` MODIFY COLUMN `category` varchar(50) NULL DEFAULT NULL COMMENT '计划分类';
ALTER TABLE `training_plan` MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
ALTER TABLE `training_plan` MODIFY COLUMN `created_by` varchar(100) NULL DEFAULT NULL COMMENT '创建人';
ALTER TABLE `training_plan` MODIFY COLUMN `description` varchar(2000) NULL DEFAULT NULL COMMENT '计划说明';
ALTER TABLE `training_plan` MODIFY COLUMN `end_date` date NOT NULL COMMENT '结束日期';
ALTER TABLE `training_plan` MODIFY COLUMN `mandatory` bit(1) NOT NULL COMMENT '是否必修';
ALTER TABLE `training_plan` MODIFY COLUMN `start_date` date NOT NULL COMMENT '开始日期';
ALTER TABLE `training_plan` MODIFY COLUMN `status` varchar(255) NOT NULL COMMENT '计划状态';
ALTER TABLE `training_plan` MODIFY COLUMN `target_role` varchar(50) NULL DEFAULT NULL COMMENT '目标角色';
ALTER TABLE `training_plan` MODIFY COLUMN `title` varchar(200) NOT NULL COMMENT '计划标题';
ALTER TABLE `training_plan` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `training_plan` MODIFY COLUMN `updated_by` varchar(100) NULL DEFAULT NULL COMMENT '更新人';
ALTER TABLE `training_plan` MODIFY COLUMN `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID';

-- ========== training_plan_users ==========
ALTER TABLE `training_plan_users` COMMENT = '培训计划学员';
ALTER TABLE `training_plan_users` MODIFY COLUMN `plan_id` bigint NOT NULL COMMENT '培训计划ID';
ALTER TABLE `training_plan_users` MODIFY COLUMN `username` varchar(255) NULL DEFAULT NULL COMMENT '学员账号';

-- ========== training_record ==========
ALTER TABLE `training_record` COMMENT = '课程学习记录';
ALTER TABLE `training_record` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `training_record` MODIFY COLUMN `complete_time` datetime(6) NULL DEFAULT NULL COMMENT '完成时间';
ALTER TABLE `training_record` MODIFY COLUMN `display_name` varchar(100) NOT NULL COMMENT '学员姓名';
ALTER TABLE `training_record` MODIFY COLUMN `duration` int NOT NULL COMMENT '学习时长（分钟）';
ALTER TABLE `training_record` MODIFY COLUMN `progress` int NOT NULL COMMENT '学习进度百分比';
ALTER TABLE `training_record` MODIFY COLUMN `start_time` datetime(6) NULL DEFAULT NULL COMMENT '开始时间';
ALTER TABLE `training_record` MODIFY COLUMN `status` varchar(50) NULL DEFAULT NULL COMMENT '学习状态';
ALTER TABLE `training_record` MODIFY COLUMN `updated_at` datetime(6) NOT NULL COMMENT '更新时间';
ALTER TABLE `training_record` MODIFY COLUMN `username` varchar(100) NOT NULL COMMENT '学员账号';
ALTER TABLE `training_record` MODIFY COLUMN `chapter_id` bigint NULL DEFAULT NULL COMMENT '章节ID';
ALTER TABLE `training_record` MODIFY COLUMN `course_id` bigint NOT NULL COMMENT '课程ID';
ALTER TABLE `training_record` MODIFY COLUMN `plan_id` bigint NULL DEFAULT NULL COMMENT '培训计划ID';
