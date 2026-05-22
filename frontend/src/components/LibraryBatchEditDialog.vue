<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  batchEditDialog: { type: Object, required: true },
  selectedCount: { type: Number, default: 0 },
  flatCategoryOptions: { type: Array, required: true },
})

const emit = defineEmits(['update:modelValue', 'apply'])
</script>

<template>
  <el-dialog :model-value="modelValue" width="520px" destroy-on-close :close-on-press-escape="true" class="batch-edit-dialog" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="card-header-block">
        <div>
          <h3>批量编辑资料</h3>
          <p>已选 {{ selectedCount }} 条，可统一修改分类、来源与标签</p>
        </div>
      </div>
    </template>
    <el-form label-position="top" class="batch-edit-form">
      <el-form-item label="目标分类">
        <el-select v-model="batchEditDialog.categoryId" clearable placeholder="不修改分类">
          <el-option v-for="category in flatCategoryOptions" :key="category.id" :label="`${'-- '.repeat(category.level)}${category.name}`" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="统一来源">
        <el-input v-model="batchEditDialog.source" placeholder="留空表示不修改" />
      </el-form-item>
      <el-form-item label="统一标签">
        <el-input v-model="batchEditDialog.tags" placeholder="留空表示不修改，多个标签用逗号分隔" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="cms-delete-dialog-footer">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" :loading="batchEditDialog.saving" @click="emit('apply')">批量保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>
