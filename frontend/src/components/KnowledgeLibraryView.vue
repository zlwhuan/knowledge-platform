<script setup>
const props = defineProps({
  libraryLoading: { type: Boolean, default: false },
  categoriesLoading: { type: Boolean, default: false },
  deleting: { type: Boolean, default: false },
  deleteDialogOpen: { type: Boolean, default: false },
  selectedCategoryId: { default: null },
  libraryFilters: { type: Object, required: true },
  flatCategories: { type: Array, required: true },
  items: { type: Array, required: true },
  selectedSummary: { type: String, required: true },
  selectedCount: { type: Number, default: 0 },
  pendingDeleteItem: { type: Object, default: null },
  dictionaryOptions: { type: Object, default: () => ({}) },
  libraryPage: { type: Number, default: 1 },
  libraryPageSize: { type: Number, default: 20 },
  libraryTotal: { type: Number, default: 0 },
})

const emit = defineEmits(['open-all', 'select-category', 'reset-filters', 'filter', 'selection-change', 'ask-delete', 'bulk-delete', 'close-delete', 'confirm-delete', 'page-change', 'size-change'])

function governanceStatus(row) {
  if (!row.summary && (!row.attachments || !row.attachments.length)) return '缺摘要/附件'
  if (!row.summary) return '缺摘要'
  if (!row.attachments || !row.attachments.length) return '缺附件'
  return '完整'
}
</script>

<template>
  <section class="page-section knowledge-library-page">
    <el-row :gutter="12" class="compact-project-row">
      <el-col :xs="24" :lg="6">
        <el-card shadow="never" class="panel-card compact-dashboard-card">
          <template #header><div class="card-header-block"><h3>分类导航</h3></div></template>
          <div class="library-subnav-card">
            <button class="subnav-link" :class="{ active: selectedCategoryId === null }" @click="emit('open-all')">全部资料</button>
            <div class="category-scroll" v-loading="categoriesLoading">
              <button
                v-for="item in flatCategories"
                :key="item.id"
                class="subnav-link category-link"
                :style="{ paddingLeft: `${12 + item.depth * 18}px` }"
                :class="{ active: selectedCategoryId === item.id }"
                @click="emit('select-category', item.id)"
              >
                {{ item.name }}
              </button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="18">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block">
              <div>
                <h3>知识库资料</h3>
                <p>恢复全部资料、分类筛选、批量删除与稳定删除弹窗</p>
              </div>
              <div class="toolbar-actions">
                <el-button @click="emit('reset-filters')">重置</el-button>
                <el-button type="danger" :disabled="selectedCount === 0" :loading="deleting" @click="emit('bulk-delete')">批量删除</el-button>
              </div>
            </div>
          </template>

          <el-form inline class="project-filter-bar">
            <el-form-item label="关键词"><el-input v-model="libraryFilters.keyword" clearable /></el-form-item>
            <el-form-item label="类型"><el-select v-model="props.libraryFilters.type" clearable style="width:140px"><el-option v-for="item in (props.dictionaryOptions.knowledgeType || ['文档', '制度', '方案', '培训'])" :key="item" :label="item" :value="item" /></el-select></el-form-item>
            <el-form-item><el-button type="primary" @click="emit('filter')">筛选</el-button></el-form-item>
          </el-form>

          <div class="selection-summary-bar">
            <span>{{ selectedSummary }}</span>
            <small>支持分类筛选与批量删除</small>
          </div>

          <el-table :data="items" stripe border max-height="520" v-loading="libraryLoading" @selection-change="emit('selection-change', $event)">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="title" label="标题" min-width="220" sortable />
            <el-table-column prop="categoryName" label="分类" min-width="120" sortable />
            <el-table-column prop="type" label="类型" width="90" sortable />
            <el-table-column prop="source" label="来源" min-width="120" />
            <el-table-column label="治理状态" width="120">
              <template #default="scope">
                <el-tag size="small" :type="governanceStatus(scope.row) === '完整' ? 'success' : 'warning'">{{ governanceStatus(scope.row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" min-width="160" sortable />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button link type="danger" @click="emit('ask-delete', scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="library-pagination" style="margin-top: 16px; display: flex; justify-content: flex-end;">
            <el-pagination
              v-if="libraryTotal > 0"
              :current-page="libraryPage"
              :page-size="libraryPageSize"
              :total="libraryTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="emit('page-change', $event)"
              @size-change="emit('size-change', $event)"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog :model-value="deleteDialogOpen" title="删除确认" width="420px" @close="emit('close-delete')">
      <div class="delete-dialog-content">
        <p>确认删除这条资料吗？</p>
        <strong>{{ pendingDeleteItem?.title || '未选择资料' }}</strong>
      </div>
      <template #footer>
        <el-button @click="emit('close-delete')">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="emit('confirm-delete')">确认删除</el-button>
      </template>
    </el-dialog>
  </section>
</template>
