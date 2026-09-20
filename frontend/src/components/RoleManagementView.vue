<script setup>
import { ref } from 'vue'

const props = defineProps({
  roleConfigs: { type: Array, required: true },
  rolesSaving: { type: Boolean, default: false },
  rolePermissionsLoading: { type: Boolean, default: false },
})

const emit = defineEmits(['save-role'])

const ROLE_LABELS = {
  ADMIN: '管理员',
  SALES: '销售',
  PRESALES: '售前',
  DELIVERY_OPS: '实施运维',
  FINANCE: '财务',
}

const ROLE_FOCUS = {
  ADMIN: '全局权限、账号、角色、配置',
  SALES: '商机推进、合同签约、客户关系',
  PRESALES: '方案支撑、投标材料、前期评估',
  DELIVERY_OPS: '交付实施、验收、售后运维',
  FINANCE: '合同金额、回款进度、结算协同',
}

const dialogVisible = ref(false)
const editingRole = ref(null)

function openEdit(config) {
  editingRole.value = config
  dialogVisible.value = true
}

function onSave() {
  if (editingRole.value) emit('save-role', editingRole.value)
  dialogVisible.value = false
}

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function roleLabel(role) {
  return ROLE_LABELS[role] || role
}

function roleFocus(role) {
  return ROLE_FOCUS[role] || ''
}

function enabledCount(config) {
  const keys = [
    'canViewLibrary',
    'canCreateContent',
    'canEditContent',
    'canDeleteContent',
    'canManageCategories',
    'canManageUsers',
    'canManageRoles',
    'canPreviewOffice',
  ]
  return keys.filter((key) => config?.[key]).length
}
</script>

<template>
  <section class="page-section sys-page">
    <el-card shadow="never" class="panel-card sys-page-card" v-loading="rolePermissionsLoading">
      <template #header>
        <div class="sys-page-head">
          <div>
            <h2>角色管理</h2>
            <p>按业务角色配置菜单与动作权限，点击「配置」弹窗修改</p>
          </div>
        </div>
      </template>

      <div class="sys-page-body">
        <el-table :data="roleConfigs" stripe border style="width: 100%" class="sys-table">
          <el-table-column label="角色" min-width="140">
            <template #default="scope">
              <div class="role-cell">
                <strong>{{ roleLabel(scope.row.role) }}</strong>
                <small>{{ scope.row.role }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="职责说明" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ roleFocus(scope.row.role) }}</template>
          </el-table-column>
          <el-table-column label="已开启权限" width="120" align="center">
            <template #default="scope">{{ enabledCount(scope.row) }} / 8</template>
          </el-table-column>
          <el-table-column label="最近更新" min-width="140">
            <template #default="scope">{{ formatDateTime(scope.row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" size="small" @click="openEdit(scope.row)">配置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`配置权限 · ${editingRole ? roleLabel(editingRole.role) : ''}`"
      width="min(420px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <div v-if="editingRole" class="sys-dialog-form role-permission-list">
        <label class="role-permission-item"><span>允许查看知识库</span><el-switch v-model="editingRole.canViewLibrary" /></label>
        <label class="role-permission-item"><span>允许新建内容</span><el-switch v-model="editingRole.canCreateContent" /></label>
        <label class="role-permission-item"><span>允许编辑内容</span><el-switch v-model="editingRole.canEditContent" /></label>
        <label class="role-permission-item"><span>允许删除内容</span><el-switch v-model="editingRole.canDeleteContent" /></label>
        <label class="role-permission-item"><span>允许管理分类</span><el-switch v-model="editingRole.canManageCategories" /></label>
        <label class="role-permission-item"><span>允许管理用户</span><el-switch v-model="editingRole.canManageUsers" /></label>
        <label class="role-permission-item"><span>允许管理角色</span><el-switch v-model="editingRole.canManageRoles" /></label>
        <label class="role-permission-item"><span>允许 Office 预览</span><el-switch v-model="editingRole.canPreviewOffice" /></label>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rolesSaving" @click="onSave">保存权限</el-button>
      </template>
    </el-dialog>
  </section>
</template>
