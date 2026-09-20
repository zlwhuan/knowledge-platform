<script setup>
import { ref } from 'vue'

const props = defineProps({
  categoriesLoading: { type: Boolean, default: false },
  categoriesSaving: { type: Boolean, default: false },
  flatCategories: { type: Array, required: true },
  categoryDraft: { type: Object, required: true },
})

const emit = defineEmits(['edit-category', 'create-child-category', 'reset-category', 'save-category', 'delete-category'])

const dialogVisible = ref(false)

function openCreate() {
  emit('reset-category')
  dialogVisible.value = true
}

function openEdit(row) {
  emit('edit-category', row)
  dialogVisible.value = true
}

function openChild(parentId) {
  emit('create-child-category', parentId)
  dialogVisible.value = true
}

function onSave() {
  emit('save-category')
  dialogVisible.value = false
}

function onReset() {
  emit('reset-category')
}
</script>

<template>
  <section class="page-section sys-page">
    <el-card shadow="never" class="panel-card sys-page-card">
      <template #header>
        <div class="sys-page-head">
          <div>
            <h2>分类配置</h2>
            <p>维护知识库分类层级、编码与排序</p>
          </div>
          <el-button type="primary" @click="openCreate">新建分类</el-button>
        </div>
      </template>

      <div class="sys-page-body">
        <el-table
          :data="flatCategories"
          stripe
          border
          v-loading="categoriesLoading"
          style="width: 100%"
          class="sys-table"
        >
          <el-table-column label="分类名称" min-width="200">
            <template #default="scope">
              <span class="sys-tree-name" :style="{ paddingLeft: `${scope.row.depth * 16}px` }">
                {{ scope.row.name }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编码" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.code || '—' }}</template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
          <el-table-column label="描述" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.description || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="scope">
              <div class="sys-row-actions">
                <el-button link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="openChild(scope.row.id)">子级</el-button>
                <el-button link type="danger" size="small" @click="emit('delete-category', scope.row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="categoryDraft.id ? '编辑分类' : '新建分类'"
      width="min(520px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form label-position="top" class="sys-dialog-form">
        <el-form-item label="分类名称" required>
          <el-input v-model="categoryDraft.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="categoryDraft.code" placeholder="例如：SURGERY_DOC" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="categoryDraft.parentId" clearable placeholder="顶级分类" style="width: 100%">
            <el-option
              v-for="category in flatCategories"
              :key="category.id"
              :label="`${'— '.repeat(category.depth)}${category.name}`"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryDraft.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="categoryDraft.description" type="textarea" :rows="3" placeholder="用途、适用范围等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="onReset">重置</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categoriesSaving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
