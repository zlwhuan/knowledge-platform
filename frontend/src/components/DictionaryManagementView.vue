<script setup>
import { computed, ref } from 'vue'
import { dictionaryTypeMeta } from '../composables/useDictionaries'

const props = defineProps({
  dictionaryGroups: { type: Array, required: true },
  dictionaryDraft: { type: Object, required: true },
  dictionariesLoading: { type: Boolean, default: false },
  dictionariesSaving: { type: Boolean, default: false },
})

const emit = defineEmits(['edit-dictionary', 'reset-dictionary', 'save-dictionary', 'delete-dictionary'])

const groupMap = computed(() => Object.fromEntries(props.dictionaryGroups.map((item) => [item.type, item])))
const typeKeyword = ref('')
const dialogVisible = ref(false)

const moduleBlocks = computed(() => {
  const keyword = String(typeKeyword.value || '').trim().toLowerCase()
  const byModule = new Map()
  dictionaryTypeMeta.forEach((item) => {
    const text = `${item.module} ${item.label} ${item.type}`.toLowerCase()
    if (keyword && !text.includes(keyword)) return
    if (!byModule.has(item.module)) byModule.set(item.module, [])
    byModule.get(item.module).push({
      ...item,
      count: groupMap.value[item.type]?.items?.length || 0,
    })
  })
  return Array.from(byModule.entries()).map(([module, types]) => ({ module, types }))
})

const currentItems = computed(() => groupMap.value[props.dictionaryDraft.dictType]?.items || [])
const currentMeta = computed(() => dictionaryTypeMeta.find((item) => item.type === props.dictionaryDraft.dictType) || null)

function selectType(type) {
  emit('reset-dictionary', type)
}

function openCreate() {
  emit('reset-dictionary', props.dictionaryDraft.dictType)
  dialogVisible.value = true
}

function openEdit(row) {
  emit('edit-dictionary', row)
  dialogVisible.value = true
}

function onSave() {
  emit('save-dictionary')
  dialogVisible.value = false
}
</script>

<template>
  <section class="page-section sys-page">
    <el-card shadow="never" class="panel-card sys-page-card" v-loading="dictionariesLoading">
      <template #header>
        <div class="sys-page-head">
          <div>
            <h2>字典维护</h2>
            <p>统一管理业务下拉选项；先选类型，再维护该类型下的字典项</p>
          </div>
          <el-button type="primary" :disabled="!dictionaryDraft.dictType" @click="openCreate">新建字典项</el-button>
        </div>
      </template>

      <div class="dict-layout">
        <aside class="dict-type-panel">
          <el-input v-model="typeKeyword" placeholder="搜索类型" clearable size="small" class="dict-type-search" />
          <div class="dict-type-scroll">
            <div v-for="block in moduleBlocks" :key="block.module" class="dict-module">
              <div class="dict-module-title">{{ block.module }}</div>
              <button
                v-for="item in block.types"
                :key="item.type"
                type="button"
                class="dict-type-item"
                :class="{ active: dictionaryDraft.dictType === item.type }"
                @click="selectType(item.type)"
              >
                <span>{{ item.label }}</span>
                <em>{{ item.count }}</em>
              </button>
            </div>
            <div v-if="!moduleBlocks.length" class="sys-empty">未匹配到字典类型</div>
          </div>
        </aside>

        <div class="dict-main">
          <div class="dict-main-head">
            <div>
              <strong>{{ currentMeta?.label || '请选择字典类型' }}</strong>
              <span>{{ currentMeta?.module || '' }}</span>
            </div>
            <span class="dict-main-count">共 {{ currentItems.length }} 项</span>
          </div>

          <el-table
            :data="currentItems"
            stripe
            border
            v-loading="dictionariesLoading"
            style="width: 100%"
            class="sys-table"
          >
            <el-table-column prop="itemLabel" label="显示名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="itemValue" label="字典值" min-width="120" show-overflow-tooltip />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="scope">
                <el-tag size="small" :type="scope.row.enabled === false ? 'info' : 'success'">
                  {{ scope.row.enabled === false ? '停用' : '启用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="scope">
                <div class="sys-row-actions">
                  <el-button link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="emit('delete-dictionary', scope.row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dictionaryDraft.id ? '编辑字典项' : '新建字典项'"
      width="min(480px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form label-position="top" class="sys-dialog-form">
        <el-form-item label="所属类型">
          <el-input :model-value="currentMeta?.label || dictionaryDraft.dictType" disabled />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="dictionaryDraft.itemLabel" placeholder="下拉展示名称" />
        </el-form-item>
        <el-form-item label="字典值" required>
          <el-input v-model="dictionaryDraft.itemValue" placeholder="与展示文案一致或业务编码" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dictionaryDraft.sortOrder" :min="0" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dictionaryDraft.remark" type="textarea" :rows="2" placeholder="可选说明" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dictionaryDraft.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emit('reset-dictionary', dictionaryDraft.dictType)">重置</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dictionariesSaving" @click="onSave">
          {{ dictionaryDraft.id ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
