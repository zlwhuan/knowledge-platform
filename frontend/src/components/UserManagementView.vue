<script setup>
const props = defineProps({
  users: { type: Array, required: true },
  roles: { type: Array, required: true },
  roleCards: { type: Array, required: true },
  userDraft: { type: Object, required: true },
  usersLoading: { type: Boolean, default: false },
  usersSaving: { type: Boolean, default: false },
})

const emit = defineEmits(['reset-user', 'save-user', 'edit-user', 'delete-user'])
</script>

<template>
  <section class="page-section">
    <el-row :gutter="12" class="users-section-row">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header><div class="card-header-block"><div><h3>{{ userDraft.id ? '编辑用户' : '新增用户' }}</h3><p>维护账号、角色和启停状态</p></div></div></template>
          <el-form label-position="top">
            <el-form-item label="用户名"><el-input v-model="userDraft.username" /></el-form-item>
            <el-form-item label="显示名"><el-input v-model="userDraft.displayName" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="userDraft.password" type="password" show-password placeholder="编辑时留空表示不修改" /></el-form-item>
            <el-form-item label="角色"><el-select v-model="userDraft.role"><el-option v-for="role in roles" :key="role.value" :label="role.label" :value="role.value" /></el-select></el-form-item>
            <el-form-item label="状态"><el-switch v-model="userDraft.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
            <el-space><el-button @click="emit('reset-user')">重置</el-button><el-button type="primary" :loading="usersSaving" @click="emit('save-user')">保存用户</el-button></el-space>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header><div class="card-header-block"><div><h3>角色概览</h3><p>可视化查看当前系统权限结构</p></div></div></template>
          <el-row :gutter="12" class="role-summary-grid">
            <el-col v-for="role in roleCards" :key="role.value" :xs="24" :sm="8"><div class="workflow-card role-summary-card"><strong>{{ role.label }}</strong><span>{{ role.count }} 人</span><small>{{ role.description }}</small></div></el-col>
          </el-row>
          <el-table :data="users" stripe border style="margin-top: 16px" v-loading="usersLoading">
            <el-table-column prop="displayName" label="显示名" min-width="140" />
            <el-table-column prop="username" label="用户名" min-width="140" />
            <el-table-column label="角色" width="140"><template #default="scope">{{ roles.find((role) => role.value === scope.row.role)?.label || scope.row.role }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="160"><template #default="scope"><el-space><el-button link type="primary" @click="emit('edit-user', scope.row)">编辑</el-button><el-button link type="danger" @click="emit('delete-user', scope.row.id)">删除</el-button></el-space></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
