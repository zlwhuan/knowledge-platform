<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  homeQuickStats: { type: Array, default: () => [] },
  canCreateContent: { type: Boolean, default: false },
  projectTrackerSummary: { type: Array, default: () => [] },
  homeProjectFocus: { type: Array, default: () => [] },
  projectRecordFeed: { type: Array, default: () => [] },
  homeActionQueue: { type: Array, default: () => [] },
  projectMilestones: { type: Array, default: () => [] },
  customerFollowupAlerts: { type: Array, default: () => [] },
})

const emit = defineEmits(['open-project-view', 'open-project-progress', 'start-create-content', 'open-all-library', 'open-system-view'])

const summaryCards = computed(() => [...(props.projectTrackerSummary || []), ...(props.homeQuickStats || [])].slice(0, 8))
const focusProjects = computed(() => (props.homeProjectFocus || []).slice(0, 12))
const radarStageFilter = ref('全部阶段')
const radarRiskFilter = ref('全部风险')

const stageDistribution = computed(() => {
  const map = new Map()
  filteredRadarProjects.value.forEach((item) => {
    const key = String(item.stage || '未分组')
    map.set(key, (map.get(key) || 0) + 1)
  })
  const total = focusProjects.value.length || 1
  return Array.from(map.entries()).map(([stage, count]) => ({
    stage,
    count,
    percent: Math.round((count / total) * 100),
  })).sort((a, b) => b.count - a.count)
})

const enhancedFocusProjects = computed(() => focusProjects.value.map((item) => {
  const progress = Number(item.progress || 0)
  const risk = progress < 40 ? '高' : progress < 70 ? '中' : '低'
  return { ...item, progress, risk }
}))

const radarStageOptions = computed(() => ['全部阶段', ...Array.from(new Set(enhancedFocusProjects.value.map((item) => item.stage).filter(Boolean)))])
const radarRiskOptions = ['全部风险', '高', '中', '低']

const filteredRadarProjects = computed(() => enhancedFocusProjects.value.filter((item) => {
  if (radarStageFilter.value !== '全部阶段' && item.stage !== radarStageFilter.value) return false
  if (radarRiskFilter.value !== '全部风险' && item.risk !== radarRiskFilter.value) return false
  return true
}))

const progressBuckets = computed(() => {
  const total = filteredRadarProjects.value.length || 1
  const buckets = [
    { label: '0-39%', min: 0, max: 39, type: 'exception' },
    { label: '40-69%', min: 40, max: 69, type: 'warning' },
    { label: '70-99%', min: 70, max: 99, type: 'success' },
    { label: '100%', min: 100, max: 100, type: 'success' },
  ]
  return buckets.map((bucket) => {
    const count = filteredRadarProjects.value.filter((item) => item.progress >= bucket.min && item.progress <= bucket.max).length
    return {
      ...bucket,
      count,
      percent: Math.round((count / total) * 100),
    }
  })
})


function parseMoney(text) {
  const raw = String(text || '').replace(/[^\d.-]/g, '')
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const contractAmount = computed(() => {
  const hit = summaryCards.value.find((item) => String(item.label).includes('合同'))
  return parseMoney(hit?.value)
})

const receivedAmount = computed(() => {
  const hit = summaryCards.value.find((item) => String(item.label).includes('回款'))
  return parseMoney(hit?.value)
})

const collectionRate = computed(() => {
  if (!contractAmount.value) return 0
  return Math.min(100, Math.round((receivedAmount.value / contractAmount.value) * 100))
})

function handleFocusProjectClick(project) {
  if (!project?.id) return emit('open-project-view', { view: 'project-tracker' })
  emit('open-project-progress', { projectId: project.id })
}

const stageChartRef = ref(null)
let stageChart = null

function renderStageChart() {
  if (!stageChartRef.value) return
  if (!stageChart) stageChart = echarts.init(stageChartRef.value)
  const data = stageDistribution.value.map(item => ({ name: item.stage, value: item.count }))
  stageChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      label: { show: true, formatter: '{b}\n{d}%' },
      data,
    }]
  })
}

watch(stageDistribution, renderStageChart, { deep: true })
onMounted(() => nextTick(renderStageChart))
onBeforeUnmount(() => { stageChart?.dispose() })
</script>

<template>
  <section class="page-section home-boss-cockpit">
    <el-card shadow="never" class="panel-card cockpit-hero">
      <div class="cockpit-hero-head">
        <div class="hero-brand-block">
          <div>
            <h2>公司运营驾驶舱</h2>
            <p>从经营结果、项目执行、客户风险三个层面，做老板视角的一屏把控。</p>
          </div>
        </div>
        <el-space>
          <el-button type="primary" @click="emit('open-project-view', 'project-tracker')">进入项目追踪</el-button>
          <el-button @click="emit('open-system-view', 'customers')">进入客户管理</el-button>
          <el-button @click="emit('open-all-library')">查看知识库</el-button>
          <el-button v-if="canCreateContent" @click="emit('start-create-content')">新增资料</el-button>
        </el-space>
      </div>

      <div class="cockpit-kpi-grid">
        <div v-for="item in summaryCards" :key="`${item.label}-${item.value}`" class="kpi-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>
    </el-card>

    <el-row :gutter="12" class="cockpit-row">
      <el-col :xs="24" :xl="24">
        <el-card shadow="never" class="panel-card cockpit-panel">
          <template #header>
            <div class="panel-head">
              <h3>经营结果看板</h3>
              <span>合同与回款健康度</span>
            </div>
          </template>
          <div class="finance-strip">
            <div class="finance-block">
              <label>累计合同额</label>
              <strong>¥{{ contractAmount.toLocaleString('zh-CN') }}</strong>
            </div>
            <div class="finance-block">
              <label>累计回款</label>
              <strong>¥{{ receivedAmount.toLocaleString('zh-CN') }}</strong>
            </div>
            <div class="finance-progress">
              <div class="finance-progress-top">
                <span>回款达成率</span>
                <strong>{{ collectionRate }}%</strong>
              </div>
              <el-progress :percentage="collectionRate" :stroke-width="10" status="success" />
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card cockpit-panel" style="margin-top: 12px;" v-if="customerFollowupAlerts.length">
          <template #header>
            <div class="panel-head">
              <h3>客户跟进提醒</h3>
              <span>逾期与近期待跟进</span>
            </div>
          </template>
          <div class="followup-alerts">
            <div v-for="alert in customerFollowupAlerts.slice(0, 8)" :key="`${alert.customerId}-${alert.date}`" class="followup-alert-row">
              <div class="followup-alert-info">
                <strong>{{ alert.customerName || '--' }}</strong>
                <span>{{ alert.ownerName || '' }}</span>
              </div>
              <div class="followup-alert-meta">
                <el-tag size="small" :type="alert.diffDays < 0 ? 'danger' : 'warning'">
                  {{ alert.status || (alert.diffDays < 0 ? '已逾期' : '待跟进') }}
                </el-tag>
                <span>{{ alert.date }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card cockpit-panel" style="margin-top: 12px;">
          <template #header>
            <div class="panel-head">
              <h3>项目执行雷达</h3>
              <el-space>
                <el-segmented v-model="radarStageFilter" :options="radarStageOptions" size="small" />
                <el-segmented v-model="radarRiskFilter" :options="radarRiskOptions" size="small" />
                <span>共 {{ filteredRadarProjects.length }} 个项目</span>
              </el-space>
            </div>
          </template>

          <div class="radar-grid">
            <div class="radar-panel">
              <h4>阶段分布</h4>
              <div class="stage-distribution">
                <div v-for="item in stageDistribution" :key="item.stage" class="stage-row">
                  <div class="stage-row-title">{{ item.stage }}</div>
                  <div class="stage-row-bar">
                    <div class="stage-row-bar-inner" :style="{ width: `${item.percent}%` }"></div>
                  </div>
                  <div class="stage-row-value">{{ item.count }}（{{ item.percent }}%）</div>
                </div>
              </div>
              <div ref="stageChartRef" style="height: 240px; margin-top: 12px;"></div>
            </div>
            <div class="radar-panel">
              <h4>进度区间分布</h4>
              <div class="bucket-list">
                <div v-for="bucket in progressBuckets" :key="bucket.label" class="bucket-row">
                  <div class="bucket-label">{{ bucket.label }}</div>
                  <el-progress :percentage="bucket.percent" :stroke-width="10" :status="bucket.type" />
                  <div class="bucket-value">{{ bucket.count }}个</div>
                </div>
              </div>
            </div>
          </div>

          <div class="focus-grid" style="margin-top: 12px;">
            <button v-for="item in filteredRadarProjects" :key="item.id" type="button" class="focus-card" @click="handleFocusProjectClick(item)">
              <div class="focus-head">
                <strong>{{ item.name }}</strong>
                <el-tag size="small" :type="item.risk === '高' ? 'danger' : item.risk === '中' ? 'warning' : 'success'">{{ item.progress }}%</el-tag>
              </div>
              <p>{{ item.customerName || '--' }} · {{ item.stage || '--' }}</p>
              <small>{{ item.next || '暂无下一步动作' }}</small>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.followup-alerts { display: flex; flex-direction: column; gap: 8px; }
.followup-alert-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #f8fafc; border-radius: 8px; }
.followup-alert-info { display: flex; gap: 8px; align-items: center; }
.followup-alert-meta { display: flex; gap: 8px; align-items: center; }
.home-boss-cockpit { display: flex; flex-direction: column; gap: 12px; }
.cockpit-hero-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.hero-brand-block { display: flex; align-items: center; gap: 12px; }
.cockpit-hero-head h2 { margin: 0; font-size: 20px; color: #143f7d; }
.cockpit-hero-head p { margin: 6px 0 0; color: #5f6f84; }
.cockpit-kpi-grid { margin-top: 12px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.kpi-card { border: 1px solid #dbe6f3; border-radius: 12px; padding: 10px 12px; background: linear-gradient(180deg,#f8fbff,#eef5ff); display: flex; flex-direction: column; gap: 4px; }
.kpi-card span { color: #6b7280; font-size: 12px; }
.kpi-card strong { font-size: 20px; color: #143f7d; line-height: 1.2; }
.kpi-card small { color: #7b8a9d; font-size: 12px; }
.cockpit-panel :deep(.el-card__header) { padding-bottom: 10px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.panel-head h3 { margin: 0; color: #143f7d; }
.panel-head span { color: #6b7280; font-size: 12px; }
.finance-strip { display: grid; grid-template-columns: 1fr 1fr 1.3fr; gap: 10px; }
.finance-block, .finance-progress { border: 1px solid #dbe6f3; border-radius: 10px; padding: 10px; background: #f9fbff; }
.finance-block label { color: #6b7280; font-size: 12px; display:block; }
.finance-block strong { margin-top: 4px; display:block; font-size: 22px; color: #163a70; }
.finance-progress-top { display:flex; justify-content: space-between; margin-bottom: 8px; color:#5f6f84; }
.stage-distribution { display:flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.stage-row { display:grid; grid-template-columns: 120px 1fr 90px; align-items:center; gap: 8px; }
.stage-row-title, .stage-row-value { font-size: 12px; color:#44566b; }
.stage-row-bar { height: 8px; background:#edf2f7; border-radius: 999px; overflow:hidden; }
.stage-row-bar-inner { height:100%; background: linear-gradient(90deg,#3b82f6,#60a5fa); }
.focus-grid { display:grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
.focus-card { border:1px solid #dbe6f3; border-radius: 10px; padding:10px; background:white; text-align:left; cursor:pointer; }
.focus-card:hover { border-color:#93c5fd; }
.focus-head { display:flex; justify-content: space-between; align-items:center; gap:8px; }
.focus-card p, .focus-card small { margin:6px 0 0; color:#5f6f84; display:block; }
.radar-grid { display: grid; grid-template-columns: 1.15fr 1fr; gap: 12px; }
.radar-panel { border: 1px solid #e5ecf6; border-radius: 10px; padding: 10px; background: #fbfdff; }
.radar-panel h4 { margin: 0 0 8px; color: #334155; }
.bucket-list { display: flex; flex-direction: column; gap: 8px; }
.bucket-row { display: grid; grid-template-columns: 64px 1fr 48px; align-items: center; gap: 8px; }
.bucket-label, .bucket-value { font-size: 12px; color: #64748b; }

@media (max-width: 1200px) {
  .cockpit-kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .finance-strip { grid-template-columns: 1fr; }
  .focus-grid, .radar-grid { grid-template-columns: 1fr; }
}
</style>
