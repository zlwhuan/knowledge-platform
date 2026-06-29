<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  filters: { type: Object, required: true },
  flatCategoryOptions: { type: Array, required: true },
  projects: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  filteredItems: { type: Array, required: true },
  items: { type: Array, required: true },
  canCreateContent: { type: Boolean, default: false },
  canEditContent: { type: Boolean, default: false },
  canDeleteContent: { type: Boolean, default: false },
  selectedItemIds: { type: Array, required: true },
  selectedItems: { type: Array, required: true },
  allDisplayedSelected: { type: Boolean, default: false },
  formatDateTime: { type: Function, required: true },
  dictionaryOptions: { type: Object, default: () => ({}) },
  libraryPage: { type: Number, default: 1 },
  libraryPageSize: { type: Number, default: 20 },
  libraryTotal: { type: Number, default: 0 },
  libraryPages: { type: Number, default: 0 },
})

const emit = defineEmits([
  'reset-library-filters',
  'load-items',
  'start-create-content',
  'toggle-select-all-displayed',
  'open-batch-edit-dialog',
  'bulk-delete-items',
  'clear-selected-items',
  'toggle-select-item',
  'open-detail',
  'fill-form',
  'delete-item',
  'page-change',
  'size-change',
  'trigger-import',
  'export-items',
])

let autoFilterTimer = null
watch(
  () => [
    props.filters.keyword,
    props.filters.type,
    props.filters.projectId,
  ],
  () => {
    if (autoFilterTimer) clearTimeout(autoFilterTimer)
    autoFilterTimer = setTimeout(() => {
      emit('load-items')
    }, 180)
  },
)

const projectNameMap = computed(() => {
  const map = new Map()
  ;(props.projects || []).forEach((project) => {
    map.set(String(project.id), project.name)
  })
  return map
})

function resolveProjectName(row) {
  if (!row?.projectId) return '--'
  return projectNameMap.value.get(String(row.projectId)) || `项目#${row.projectId}`
}

function resolveTags(tags) {
  return String(tags || '')
    .split(/[，,、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 4)
}

onBeforeUnmount(() => {
  if (autoFilterTimer) clearTimeout(autoFilterTimer)
})
</script>

<template>
  <section class="page-section library-section">
    <el-card shadow="never" class="panel-card filter-card">
      <template #header>
        <div class="card-header-block">
          <div>
            <h3>资料检索</h3>
            <p>按关键字、分类和类型组合筛选当前内容</p>
          </div>
        </div>
      </template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="标题 / 摘要 / 标签" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" clearable placeholder="全部类型" style="width: 180px">
            <el-option v-for="item in (dictionaryOptions.knowledgeType || ['文档', '制度', '方案', '培训'])" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable placeholder="全部分类" style="width: 220px" @change="emit('load-items')">
            <el-option v-for="category in flatCategoryOptions" :key="category.id" :label="`${'-- '.repeat(category.level)}${category.name}`" :value="String(category.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="filters.projectId" clearable placeholder="全部项目" style="width: 220px">
            <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="String(project.id)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="emit('reset-library-filters')">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="panel-card library-table-card">
      <template #header>
        <div class="card-header-block">
          <div>
            <h3>资料列表</h3>
            <p>{{ loading ? '正在加载数据' : `当前显示 ${filteredItems.length} 条 / 总计 ${items.length} 条` }}</p>
          </div>
          <div class="toolbar-actions">
            <el-button v-if="canCreateContent" type="primary" @click="emit('start-create-content')">新建内容</el-button>
            <el-button @click="emit('toggle-select-all-displayed')">{{ allDisplayedSelected ? '取消全选' : '全选当前结果' }}</el-button>
            <el-button v-if="canEditContent" @click="emit('open-batch-edit-dialog')">批量编辑</el-button>
            <el-button @click="emit('load-items')">刷新列表</el-button>
            <el-button @click="emit('trigger-import')">导入Excel</el-button>
            <el-button @click="emit('export-items')">导出Excel</el-button>
            <el-button v-if="canDeleteContent" type="danger" plain @click="emit('bulk-delete-items')">批量删除</el-button>
          </div>
        </div>
      </template>
      <div v-if="selectedItemIds.length" class="selection-summary-bar">
        <div>
          <strong>已选 {{ selectedItemIds.length }} 条</strong>
          <span>{{ selectedItems.slice(0, 3).map((item) => item.title).join('、') }}<template v-if="selectedItemIds.length > 3"> 等内容</template></span>
        </div>
        <div class="selection-summary-actions">
          <el-button v-if="canEditContent" size="small" @click="emit('open-batch-edit-dialog')">批量编辑</el-button>
          <el-button size="small" @click="emit('clear-selected-items')">清空选择</el-button>
        </div>
      </div>
      <el-table :data="filteredItems" stripe border v-loading="loading" height="100%" @row-dblclick="emit('open-detail', $event)">
        <el-table-column width="56">
          <template #default="scope">
            <el-checkbox :model-value="selectedItemIds.includes(scope.row.id)" @change="emit('toggle-select-item', scope.row.id)" @click.stop />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="260" sortable />
        <el-table-column label="关联项目" min-width="180">
          <template #default="scope">{{ resolveProjectName(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="摘要" min-width="260" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.summary || '--' }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="200">
          <template #default="scope">
            <el-space wrap>
              <el-tag v-for="tag in resolveTags(scope.row.tags)" :key="`${scope.row.id}-${tag}`" size="small" type="info">{{ tag }}</el-tag>
              <span v-if="!resolveTags(scope.row.tags).length" style="color:#94a3b8;">--</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="140" sortable />
        <el-table-column label="附件数" width="100">
          <template #default="scope">{{ scope.row.attachments?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="上传人" prop="createdBy" width="120">
          <template #default="scope">{{ scope.row.createdBy || '--' }}</template>
        </el-table-column>
        <el-table-column label="最后编辑者" prop="updatedBy" width="130">
          <template #default="scope">{{ scope.row.updatedBy || '--' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="180" sortable>
          <template #default="scope">{{ formatDateTime(scope.row.updatedAt || scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button link @click.stop="emit('open-detail', scope.row)">详情</el-button>
              <el-button v-if="canEditContent" link type="primary" @click.stop="emit('fill-form', scope.row)">编辑</el-button>
              <el-button v-if="canDeleteContent" link type="danger" @click.stop="emit('delete-item', scope.row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !filteredItems.length" description="暂无知识条目，点击右上角新建" />
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
  </section>
</template>
