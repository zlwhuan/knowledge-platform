<script setup>
const props = defineProps({
  categoriesLoading: { type: Boolean, default: false },
  categoriesSaving: { type: Boolean, default: false },
  flatCategories: { type: Array, required: true },
  categoryDraft: { type: Object, required: true },
})

const emit = defineEmits(['edit-category', 'create-child-category', 'reset-category', 'save-category', 'delete-category'])
</script>

<template>
  <section class="page-section">
    <el-row :gutter="12" class="settings-section-row">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block"><div><h3>{{ categoryDraft.id ? '编辑分类' : '新增分类' }}</h3><p>维护分类层级、编码和排序规则</p></div></div>
          </template>
          <el-form label-position="top">
            <el-form-item label="分类名称"><el-input v-model="categoryDraft.name" placeholder="请输入分类名称" /></el-form-item>
            <el-form-item label="分类编码"><el-input v-model="categoryDraft.code" placeholder="例如：SURGERY_DOC" /></el-form-item>
            <el-form-item label="上级分类"><el-select v-model="categoryDraft.parentId" clearable placeholder="顶级分类"><el-option v-for="category in flatCategories" :key="category.id" :label="`${'-- '.repeat(category.depth)}${category.name}`" :value="category.id" /></el-select></el-form-item>
            <el-form-item label="排序"><el-input-number v-model="categoryDraft.sortOrder" :min="0" :max="9999" /></el-form-item>
            <el-form-item label="描述"><el-input v-model="categoryDraft.description" type="textarea" :rows="4" placeholder="描述该分类的用途、适用范围和边界" /></el-form-item>
            <el-space><el-button @click="emit('reset-category')">重置</el-button><el-button type="primary" :loading="categoriesSaving" @click="emit('save-category')">保存分类</el-button></el-space>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block"><div><h3>分类结构</h3><p>可直接查看层级、编码和排序，并支持编辑和新增子级</p></div></div>
          </template>
          <el-table :data="flatCategories" stripe border v-loading="categoriesLoading">
            <el-table-column label="分类名称" min-width="240"><template #default="scope"><button class="category-table-link" :style="{ paddingLeft: `${scope.row.depth * 18}px` }">{{ scope.row.name }}</button></template></el-table-column>
            <el-table-column prop="code" label="编码" width="160" />
            <el-table-column prop="sortOrder" label="排序" width="100" />
            <el-table-column label="操作" width="220"><template #default="scope"><el-space><el-button link type="primary" @click="emit('edit-category', scope.row)">编辑</el-button><el-button link @click="emit('create-child-category', scope.row.id)">新增子级</el-button><el-button link type="danger" @click="emit('delete-category', scope.row.id)">删除</el-button></el-space></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
