<script setup>
import { ref } from 'vue'

const props = defineProps({
  users: { type: Array, required: true },
  roles: { type: Array, required: true },
  roleCards: { type: Array, required: true },
  userDraft: { type: Object, required: true },
  usersLoading: { type: Boolean, default: false },
  usersSaving: { type: Boolean, default: false },
})

const emit = defineEmits(['reset-user', 'save-user', 'edit-user', 'delete-user'])

const dialogVisible = ref(false)

function openCreate() {
  emit('reset-user')
  dialogVisible.value = true
}

function openEdit(row) {
  emit('edit-user', row)
  dialogVisible.value = true
}

function onSave() {
  emit('save-user')
  dialogVisible.value = false
}

function onReset() {
  emit('reset-user')
}

function roleLabel(role) {
  return props.roles.find((item) => item.value === role)?.label || role || '--'
}
</script>

<template>
  <section class="page-section sys-page">
    <div class="sys-stat-row">
      <div v-for="role in roleCards" :key="role.value" class="sys-stat-card">
        <span>{{ role.label }}</span>
        <strong>{{ role.count }}</strong>
        <small>{{ role.description }}</small>
      </div>
    </div>

    <el-card shadow="never" class="panel-card sys-page-card">
      <template #header>
        <div class="sys-page-head">
          <div>
            <h2>用户管理</h2>
            <p>维护系统账号、角色与启用状态</p>
          </div>
          <el-button type="primary" @click="openCreate">新建用户</el-button>
        </div>
      </template>

      <div class="sys-page-body">
        <el-table :data="users" stripe border v-loading="usersLoading" style="width: 100%" class="sys-table">
          <el-table-column prop="displayName" label="显示名" min-width="140" show-overflow-tooltip />
          <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
          <el-table-column label="角色" width="130">
            <template #default="scope">{{ roleLabel(scope.row.role) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag size="small" :type="scope.row.enabled ? 'success' : 'info'">
                {{ scope.row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="scope">
              <div class="sys-row-actions">
                <el-button link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="emit('delete-user', scope.row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="userDraft.id ? '编辑用户' : '新建用户'"
      width="min(480px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form label-position="top" class="sys-dialog-form">
        <el-form-item label="用户名" required>
          <el-input v-model="userDraft.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="userDraft.displayName" placeholder="界面展示名称" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userDraft.password" type="password" show-password :placeholder="userDraft.id ? '留空表示不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="userDraft.role" style="width: 100%">
            <el-option v-for="role in roles" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userDraft.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="onReset">重置</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="usersSaving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
