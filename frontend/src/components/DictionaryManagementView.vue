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
</script>

<template>
  <section class="page-section">
    <el-row :gutter="12" class="settings-section-row">
      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="panel-card" v-loading="dictionariesLoading">
          <template #header>
            <div class="card-header-block"><h3>字典类型</h3><p>按模块快速定位，不用长下拉查找</p></div>
          </template>

          <el-input v-model="typeKeyword" placeholder="搜索类型（如：客户/状态/知识）" clearable style="margin-bottom: 12px" />
          <div v-if="!moduleBlocks.length" style="color:#94a3b8;font-size:13px;">未匹配到字典类型</div>
          <div v-for="block in moduleBlocks" :key="block.module" style="margin-bottom: 14px;">
            <h4 style="margin: 0 0 8px; color: #334155;">{{ block.module }}</h4>
            <el-space wrap>
              <el-button
                v-for="item in block.types"
                :key="item.type"
                size="small"
                :type="dictionaryDraft.dictType === item.type ? 'primary' : 'default'"
                @click="emit('reset-dictionary', item.type)"
              >
                {{ item.label }}（{{ item.count }}）
              </el-button>
            </el-space>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="panel-card" v-loading="dictionariesLoading">
          <template #header>
            <div class="card-header-block"><h3>编辑字典项</h3><p>{{ currentMeta?.module || '系统' }} · {{ currentMeta?.label || '--' }}</p></div>
          </template>

          <el-form label-position="top">
            <el-form-item label="字典值"><el-input v-model="dictionaryDraft.itemValue" placeholder="建议与展示文案一致" /></el-form-item>
            <el-form-item label="显示名称"><el-input v-model="dictionaryDraft.itemLabel" placeholder="下拉展示名称" /></el-form-item>
            <el-form-item label="排序"><el-input-number v-model="dictionaryDraft.sortOrder" :min="0" :step="10" style="width: 100%" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="dictionaryDraft.remark" type="textarea" :rows="3" placeholder="可选说明" /></el-form-item>
            <el-form-item><el-switch v-model="dictionaryDraft.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
            <el-space>
              <el-button @click="emit('reset-dictionary', dictionaryDraft.dictType)">重置</el-button>
              <el-button type="primary" :loading="dictionariesSaving" @click="emit('save-dictionary')">{{ dictionaryDraft.id ? '更新' : '新增' }}</el-button>
            </el-space>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card" v-loading="dictionariesLoading">
          <template #header>
            <div class="card-header-block">
              <h3>{{ currentMeta?.label || '字典项列表' }}</h3>
              <p>{{ currentMeta?.module || '系统' }} · 共 {{ currentItems.length }} 项</p>
            </div>
          </template>

          <el-table :data="currentItems" stripe border>
            <el-table-column prop="itemLabel" label="名称" min-width="120" />
            <el-table-column prop="itemValue" label="值" min-width="120" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="scope"><el-tag size="small" :type="scope.row.enabled === false ? 'info' : 'success'">{{ scope.row.enabled === false ? '停用' : '启用' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="emit('edit-dictionary', scope.row)">编辑</el-button>
                <el-button link type="danger" @click="emit('delete-dictionary', scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
