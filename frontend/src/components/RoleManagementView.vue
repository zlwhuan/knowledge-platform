<script setup>
const props = defineProps({
  roleConfigs: { type: Array, required: true },
  rolesSaving: { type: Boolean, default: false },
  rolePermissionsLoading: { type: Boolean, default: false },
})

const emit = defineEmits(['save-role'])

const businessRoleProfiles = [
  { key: 'admin', label: '管理员', focus: '全局权限、账号、角色、配置' },
  { key: 'sales', label: '销售', focus: '商机推进、合同签约、客户关系' },
  { key: 'presales', label: '售前', focus: '方案支撑、投标材料、前期评估' },
  { key: 'deliveryOps', label: '实施运维', focus: '交付实施、验收、售后运维一体' },
  { key: 'finance', label: '财务', focus: '合同金额、回款进度、结算协同' },
]

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}
</script>

<template>
  <section class="page-section">
    <el-card shadow="never" class="panel-card" v-loading="rolePermissionsLoading">
      <template #header><div class="card-header-block"><div><h3>角色管理</h3><p>按公司五类角色（管理员/销售/售前/实施运维/财务）配置菜单与动作权限</p></div></div></template>
      <div class="business-role-grid">
        <el-card v-for="profile in businessRoleProfiles" :key="profile.key" shadow="never" class="role-config-card business-role-card">
          <template #header><div class="card-header-block"><div><h3>{{ profile.label }}</h3><p>{{ profile.focus }}</p></div><el-tag effect="plain">业务角色</el-tag></div></template>
        </el-card>
      </div>
      <div class="role-config-grid">
        <el-card v-for="config in roleConfigs" :key="config.role" shadow="never" class="role-config-card">
          <template #header><div class="card-header-block"><div><h3>{{ config.role }}</h3><p>角色权限配置</p></div><el-tag type="info">{{ config.role }}</el-tag></div></template>
          <div class="role-permission-list">
            <label class="role-permission-item"><span>允许查看知识库</span><el-switch v-model="config.canViewLibrary" /></label>
            <label class="role-permission-item"><span>允许新建内容</span><el-switch v-model="config.canCreateContent" /></label>
            <label class="role-permission-item"><span>允许编辑内容</span><el-switch v-model="config.canEditContent" /></label>
            <label class="role-permission-item"><span>允许删除内容</span><el-switch v-model="config.canDeleteContent" /></label>
            <label class="role-permission-item"><span>允许管理分类</span><el-switch v-model="config.canManageCategories" /></label>
            <label class="role-permission-item"><span>允许管理用户</span><el-switch v-model="config.canManageUsers" /></label>
            <label class="role-permission-item"><span>允许管理角色</span><el-switch v-model="config.canManageRoles" /></label>
            <label class="role-permission-item"><span>允许 Office 预览</span><el-switch v-model="config.canPreviewOffice" /></label>
          </div>
          <div class="role-config-footer"><span>最近更新：{{ formatDateTime(config.updatedAt) }}</span><el-button type="primary" :loading="rolesSaving" @click="emit('save-role', config)">保存权限</el-button></div>
        </el-card>
      </div>
    </el-card>
  </section>
</template>
