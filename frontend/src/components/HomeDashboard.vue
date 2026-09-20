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

const primaryKpis = computed(() => {
  const source = props.projectTrackerSummary || []
  return source.map((item) => {
    const label = String(item.label || '')
    let tone = 'neutral'
    if (label.includes('高风险')) tone = 'danger'
    else if (label.includes('待验收') || label.includes('待回款')) tone = 'warning'
    else if (label.includes('总数')) tone = 'accent'
    return { ...item, tone }
  })
})

const secondaryStats = computed(() => props.homeQuickStats || [])

const focusProjects = computed(() => (props.homeProjectFocus || []).slice(0, 12))
const radarStageFilter = ref('全部阶段')
const radarRiskFilter = ref('全部风险')

const enhancedFocusProjects = computed(() => focusProjects.value.map((item) => {
  const progress = Number(item.progress || 0)
  const risk = progress < 40 ? '高' : progress < 70 ? '中' : '低'
  return { ...item, progress, risk }
}))

const stageDistribution = computed(() => {
  const map = new Map()
  filteredRadarProjects.value.forEach((item) => {
    const key = String(item.stage || '未分组')
    map.set(key, (map.get(key) || 0) + 1)
  })
  const total = filteredRadarProjects.value.length || 1
  return Array.from(map.entries()).map(([stage, count]) => ({
    stage,
    count,
    percent: Math.round((count / total) * 100),
  })).sort((a, b) => b.count - a.count)
})

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
    { label: '0–39%', min: 0, max: 39, type: 'exception', tone: 'danger' },
    { label: '40–69%', min: 40, max: 69, type: 'warning', tone: 'warning' },
    { label: '70–99%', min: 70, max: 99, type: 'success', tone: 'success' },
    { label: '100%', min: 100, max: 100, type: 'success', tone: 'success' },
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
  const hit = [...primaryKpis.value, ...secondaryStats.value].find((item) => String(item.label || '').includes('合同'))
  return parseMoney(hit?.value)
})

const receivedAmount = computed(() => {
  const hit = [...primaryKpis.value, ...secondaryStats.value].find((item) => String(item.label || '').includes('回款'))
  return parseMoney(hit?.value)
})

const collectionRate = computed(() => {
  if (!contractAmount.value) return 0
  return Math.min(100, Math.round((receivedAmount.value / contractAmount.value) * 100))
})

const collectionGap = computed(() => Math.max(0, contractAmount.value - receivedAmount.value))

const followupAlerts = computed(() => (props.customerFollowupAlerts || []).slice(0, 8))
const actionQueue = computed(() => (props.homeActionQueue || []).slice(0, 6))
const recordFeed = computed(() => (props.projectRecordFeed || []).slice(0, 6))
const milestoneCount = computed(() => (props.projectMilestones || []).length)

const todayLabel = computed(() => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
})

function riskTagType(risk) {
  if (risk === '高') return 'danger'
  if (risk === '中') return 'warning'
  return 'success'
}

function handleFocusProjectClick(project) {
  if (!project?.id) return emit('open-project-view', 'project-tracker')
  emit('open-project-progress', { projectId: project.id })
}

function handleActionClick(item) {
  if (!item) return
  const target = item.target
  if (target && typeof target === 'object') {
    if (target.projectId) {
      emit('open-project-progress', { projectId: target.projectId })
      return
    }
    emit('open-project-view', target.view || 'project-tracker')
    return
  }
  if (target === 'library') {
    emit('open-all-library')
    return
  }
  emit('open-project-view', 'project-tracker')
}

function handleFeedClick(item) {
  if (!item?.project) return
  emit('open-project-view', 'project-tracker')
}

const stageChartRef = ref(null)
let stageChart = null

function renderStageChart() {
  if (!stageChartRef.value) return
  if (!stageChart) stageChart = echarts.init(stageChartRef.value)
  const data = stageDistribution.value.map((item) => ({ name: item.stage, value: item.count }))
  stageChart.setOption({
    color: ['#1e5aa8', '#3d7cc9', '#6ea0dc', '#9bc0e8', '#c5daf0', '#7f93ad'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', color: '#4a5d73', fontSize: 11 },
      labelLine: { length: 8, length2: 6 },
      data,
    }]
  })
}

watch(stageDistribution, renderStageChart, { deep: true })
onMounted(() => nextTick(renderStageChart))
onBeforeUnmount(() => { stageChart?.dispose() })
</script>

<template>
  <section class="home-cockpit">
    <header class="cockpit-topbar">
      <div class="cockpit-title">
        <div class="cockpit-eyebrow">经营驾驶舱 · {{ todayLabel }}</div>
        <h2>一屏看清经营与执行</h2>
        <p>合同回款、项目风险、客户跟进集中呈现，优先处理标红事项。</p>
      </div>
      <div class="cockpit-actions">
        <el-button type="primary" @click="emit('open-project-view', 'project-tracker')">项目追踪</el-button>
        <el-button @click="emit('open-system-view', 'customers')">客户管理</el-button>
        <el-button @click="emit('open-all-library')">知识库</el-button>
        <el-button v-if="canCreateContent" @click="emit('start-create-content')">新增资料</el-button>
      </div>
    </header>

    <div class="kpi-grid">
      <div
        v-for="item in primaryKpis"
        :key="`${item.label}-${item.value}`"
        class="kpi-card"
        :class="`tone-${item.tone}`"
      >
        <span class="kpi-label">{{ item.label }}</span>
        <strong class="kpi-value">{{ item.value }}</strong>
        <small class="kpi-hint">{{ item.hint }}</small>
      </div>
    </div>

    <div class="stat-strip">
      <div v-for="item in secondaryStats" :key="item.label" class="stat-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="cockpit-layout">
      <div class="cockpit-main">
        <section class="hd-panel finance-panel">
          <div class="hd-panel-head">
            <div>
              <h3>经营结果</h3>
              <p>合同与回款健康度，达成率优先看。</p>
            </div>
            <el-tag :type="collectionRate >= 70 ? 'success' : collectionRate >= 40 ? 'warning' : 'danger'" size="small">
              达成 {{ collectionRate }}%
            </el-tag>
          </div>
          <div class="finance-body">
            <div class="finance-numbers">
              <div class="finance-metric">
                <label>累计合同额</label>
                <strong>¥{{ contractAmount.toLocaleString('zh-CN') }}</strong>
              </div>
              <div class="finance-metric">
                <label>累计回款</label>
                <strong class="is-received">¥{{ receivedAmount.toLocaleString('zh-CN') }}</strong>
              </div>
              <div class="finance-metric">
                <label>待回款缺口</label>
                <strong class="is-gap">¥{{ collectionGap.toLocaleString('zh-CN') }}</strong>
              </div>
            </div>
            <div class="finance-progress-block">
              <div class="finance-progress-top">
                <span>回款达成率</span>
                <strong>{{ collectionRate }}%</strong>
              </div>
              <el-progress
                :percentage="collectionRate"
                :stroke-width="12"
                :status="collectionRate >= 70 ? 'success' : collectionRate >= 40 ? 'warning' : 'exception'"
                :show-text="false"
              />
              <p class="finance-caption">缺口金额用于财务催收排期，不替代项目维度明细。</p>
            </div>
          </div>
        </section>

        <section class="hd-panel radar-panel">
          <div class="hd-panel-head">
            <div>
              <h3>项目执行雷达</h3>
              <p>阶段分布、进度区间与焦点项目，点击卡片进入进度。</p>
            </div>
            <div class="radar-filters">
              <el-segmented v-model="radarStageFilter" :options="radarStageOptions" size="small" />
              <el-segmented v-model="radarRiskFilter" :options="radarRiskOptions" size="small" />
              <span class="radar-count">{{ filteredRadarProjects.length }} 个项目</span>
            </div>
          </div>

          <div class="radar-grid">
            <div class="radar-block">
              <h4>阶段分布</h4>
              <div class="stage-distribution">
                <div v-for="item in stageDistribution" :key="item.stage" class="stage-row">
                  <div class="stage-row-title">{{ item.stage }}</div>
                  <div class="stage-row-bar">
                    <div class="stage-row-bar-inner" :style="{ width: `${item.percent}%` }"></div>
                  </div>
                  <div class="stage-row-value">{{ item.count }} · {{ item.percent }}%</div>
                </div>
                <div v-if="!stageDistribution.length" class="hd-empty">暂无阶段数据</div>
              </div>
              <div ref="stageChartRef" class="stage-chart"></div>
            </div>
            <div class="radar-block">
              <h4>进度区间</h4>
              <div class="bucket-list">
                <div v-for="bucket in progressBuckets" :key="bucket.label" class="bucket-row">
                  <div class="bucket-label">{{ bucket.label }}</div>
                  <el-progress :percentage="bucket.percent" :stroke-width="8" :status="bucket.type" :show-text="false" />
                  <div class="bucket-value" :class="`tone-${bucket.tone}`">{{ bucket.count }}个</div>
                </div>
              </div>
            </div>
          </div>

          <div class="focus-grid">
            <button
              v-for="item in filteredRadarProjects"
              :key="item.id"
              type="button"
              class="focus-card"
              @click="handleFocusProjectClick(item)"
            >
              <div class="focus-head">
                <strong>{{ item.name }}</strong>
                <el-tag size="small" :type="riskTagType(item.risk)">{{ item.risk }}风险 · {{ item.progress }}%</el-tag>
              </div>
              <div class="focus-meta">{{ item.customerName || '未关联客户' }} · {{ item.stage || '未分阶段' }}</div>
              <div class="focus-owner">负责人 {{ item.owner || '--' }}</div>
              <p>{{ item.next || '暂无下一步动作' }}</p>
            </button>
            <div v-if="!filteredRadarProjects.length" class="hd-empty">当前筛选下暂无项目</div>
          </div>
        </section>
      </div>

      <aside class="cockpit-side">
        <section class="hd-panel">
          <div class="hd-panel-head">
            <div>
              <h3>客户跟进提醒</h3>
              <p>逾期与 7 日内到期</p>
            </div>
            <span class="side-count">{{ followupAlerts.length }}</span>
          </div>
          <div v-if="followupAlerts.length" class="side-list">
            <div v-for="alert in followupAlerts" :key="`${alert.customerId}-${alert.date}`" class="side-row">
              <div class="side-row-main">
                <strong>{{ alert.customerName || '--' }}</strong>
                <span>{{ alert.ownerName || '' }}</span>
              </div>
              <div class="side-row-meta">
                <el-tag size="small" :type="alert.diffDays < 0 ? 'danger' : 'warning'">
                  {{ alert.status || (alert.diffDays < 0 ? '已逾期' : '待跟进') }}
                </el-tag>
                <span>{{ alert.date }}</span>
              </div>
            </div>
          </div>
          <div v-else class="hd-empty">暂无近期待跟进客户</div>
        </section>

        <section class="hd-panel">
          <div class="hd-panel-head">
            <div>
              <h3>待办队列</h3>
              <p>优先处理的下一步动作</p>
            </div>
            <span class="side-count">{{ actionQueue.length }}</span>
          </div>
          <div v-if="actionQueue.length" class="side-list">
            <button v-for="(item, index) in actionQueue" :key="`${item.title}-${index}`" type="button" class="side-action" @click="handleActionClick(item)">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
          <div v-else class="hd-empty">暂无待办</div>
        </section>

        <section class="hd-panel">
          <div class="hd-panel-head">
            <div>
              <h3>最近项目动态</h3>
              <p>进度记录摘要</p>
            </div>
            <span class="side-count">{{ recordFeed.length }}<template v-if="milestoneCount"> / {{ milestoneCount }} 里程碑</template></span>
          </div>
          <div v-if="recordFeed.length" class="side-list">
            <button v-for="(item, index) in recordFeed" :key="`${item.project}-${item.time}-${index}`" type="button" class="side-feed" @click="handleFeedClick(item)">
              <div class="feed-top">
                <strong>{{ item.project }}</strong>
                <span>{{ item.stage }}</span>
              </div>
              <p>{{ item.content }}</p>
              <small>{{ item.owner }} · {{ item.time }}</small>
            </button>
          </div>
          <div v-else class="hd-empty">暂无最近动态</div>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.home-cockpit {
  --hd-ink: #0f2f5c;
  --hd-ink-soft: #334861;
  --hd-muted: #6b7c93;
  --hd-line: #d7e3f1;
  --hd-surface: #ffffff;
  --hd-soft: #f7fafd;
  --hd-accent: #1e5aa8;
  --hd-accent-soft: #eaf2fc;
  --hd-danger: #c45c5c;
  --hd-danger-soft: #fdf0f0;
  --hd-warning: #b7791f;
  --hd-warning-soft: #fbf5e9;
  --hd-success: #2f7d5a;
  --hd-success-soft: #edf7f1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cockpit-topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 22px;
  border: 1px solid var(--hd-line);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(30, 90, 168, 0.08), transparent 42%),
    var(--hd-surface);
  box-shadow: 0 8px 24px rgba(15, 47, 92, 0.04);
}

.cockpit-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--hd-accent);
  margin-bottom: 6px;
}

.cockpit-title h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
  color: var(--hd-ink);
  letter-spacing: 0.01em;
}

.cockpit-title p {
  margin: 8px 0 0;
  color: var(--hd-muted);
  line-height: 1.6;
}

.cockpit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-card {
  position: relative;
  padding: 14px 16px 12px;
  border: 1px solid var(--hd-line);
  border-radius: 14px;
  background: var(--hd-surface);
  box-shadow: 0 6px 18px rgba(15, 47, 92, 0.03);
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: hidden;
}

.kpi-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--hd-accent);
}

.kpi-card.tone-danger::before { background: var(--hd-danger); }
.kpi-card.tone-warning::before { background: var(--hd-warning); }
.kpi-card.tone-accent::before { background: var(--hd-accent); }
.kpi-card.tone-neutral::before { background: #9bb4d0; }

.kpi-card.tone-danger .kpi-value { color: var(--hd-danger); }
.kpi-card.tone-warning .kpi-value { color: var(--hd-warning); }

.kpi-label {
  font-size: 12px;
  color: var(--hd-muted);
}

.kpi-value {
  font-size: 28px;
  font-weight: 650;
  line-height: 1.15;
  color: var(--hd-ink);
  font-variant-numeric: tabular-nums;
}

.kpi-hint {
  font-size: 12px;
  color: #8a98ab;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-item {
  padding: 10px 12px;
  border: 1px dashed var(--hd-line);
  border-radius: 12px;
  background: var(--hd-soft);
}

.stat-item span,
.stat-item small {
  display: block;
  color: var(--hd-muted);
  font-size: 12px;
}

.stat-item strong {
  display: block;
  margin: 4px 0 2px;
  font-size: 16px;
  color: var(--hd-ink-soft);
  font-variant-numeric: tabular-nums;
}

.cockpit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.cockpit-main,
.cockpit-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.hd-panel {
  border: 1px solid var(--hd-line);
  border-radius: 16px;
  background: var(--hd-surface);
  box-shadow: 0 6px 18px rgba(15, 47, 92, 0.03);
  padding: 16px;
}

.hd-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.hd-panel-head h3 {
  margin: 0;
  font-size: 16px;
  color: var(--hd-ink);
}

.hd-panel-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--hd-muted);
}

.radar-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.radar-count,
.side-count {
  font-size: 12px;
  color: var(--hd-muted);
}

.finance-body {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 14px;
}

.finance-numbers {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.finance-metric {
  padding: 12px;
  border-radius: 12px;
  background: var(--hd-soft);
  border: 1px solid transparent;
}

.finance-metric label {
  display: block;
  font-size: 12px;
  color: var(--hd-muted);
}

.finance-metric strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--hd-ink);
  font-variant-numeric: tabular-nums;
}

.finance-metric strong.is-received { color: var(--hd-success); }
.finance-metric strong.is-gap { color: var(--hd-warning); }

.finance-progress-block {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--hd-line);
  background: linear-gradient(180deg, #f8fbff, #eef5ff);
}

.finance-progress-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--hd-ink-soft);
}

.finance-progress-top strong {
  font-size: 22px;
  color: var(--hd-ink);
  font-variant-numeric: tabular-nums;
}

.finance-caption {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--hd-muted);
}

.radar-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.radar-block {
  border: 1px solid #e7eef8;
  border-radius: 12px;
  padding: 12px;
  background: var(--hd-soft);
}

.radar-block h4 {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--hd-ink-soft);
}

.stage-distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stage-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 72px;
  gap: 8px;
  align-items: center;
}

.stage-row-title,
.stage-row-value,
.bucket-label,
.bucket-value {
  font-size: 12px;
  color: var(--hd-muted);
}

.stage-row-value,
.bucket-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.bucket-value.tone-danger { color: var(--hd-danger); }
.bucket-value.tone-warning { color: var(--hd-warning); }
.bucket-value.tone-success { color: var(--hd-success); }

.stage-row-bar {
  height: 7px;
  border-radius: 999px;
  background: #e3ebf5;
  overflow: hidden;
}

.stage-row-bar-inner {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #1e5aa8, #6ea0dc);
}

.stage-chart {
  height: 220px;
  margin-top: 8px;
}

.bucket-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bucket-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 48px;
  gap: 8px;
  align-items: center;
}

.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.focus-card {
  border: 1px solid var(--hd-line);
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.focus-card:hover {
  border-color: #8fb6e4;
  box-shadow: 0 8px 18px rgba(30, 90, 168, 0.08);
  transform: translateY(-1px);
}

.focus-card:focus-visible {
  outline: 2px solid rgba(30, 90, 168, 0.45);
  outline-offset: 2px;
}

.focus-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.focus-head strong {
  color: var(--hd-ink);
  font-size: 14px;
  line-height: 1.35;
}

.focus-meta,
.focus-owner {
  margin-top: 6px;
  font-size: 12px;
  color: var(--hd-muted);
}

.focus-card p {
  margin: 8px 0 0;
  color: var(--hd-ink-soft);
  font-size: 12px;
  line-height: 1.55;
}

.side-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-row,
.side-action,
.side-feed {
  width: 100%;
  border: 1px solid #e7eef8;
  border-radius: 12px;
  background: var(--hd-soft);
  padding: 10px 12px;
}

.side-action,
.side-feed {
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.side-action:hover,
.side-feed:hover {
  border-color: #9ec0e8;
  background: #fff;
}

.side-action:focus-visible,
.side-feed:focus-visible {
  outline: 2px solid rgba(30, 90, 168, 0.45);
  outline-offset: 2px;
}

.side-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.side-row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-row-main strong,
.side-action strong,
.feed-top strong {
  color: var(--hd-ink);
  font-size: 13px;
}

.side-row-main span,
.side-action span,
.side-feed small {
  color: var(--hd-muted);
  font-size: 12px;
}

.side-row-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: var(--hd-muted);
  font-size: 12px;
}

.side-action span {
  display: block;
  margin-top: 4px;
  line-height: 1.45;
}

.feed-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: baseline;
}

.feed-top span {
  color: var(--hd-accent);
  font-size: 12px;
}

.side-feed p {
  margin: 6px 0 4px;
  color: var(--hd-ink-soft);
  font-size: 12px;
  line-height: 1.5;
}

.hd-empty {
  padding: 18px 12px;
  text-align: center;
  color: var(--hd-muted);
  font-size: 12px;
  border: 1px dashed var(--hd-line);
  border-radius: 12px;
  background: var(--hd-soft);
}

@media (max-width: 1400px) {
  .cockpit-layout {
    grid-template-columns: minmax(0, 1.45fr) minmax(260px, 0.9fr);
  }

  .finance-numbers {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .kpi-grid,
  .stat-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cockpit-layout,
  .finance-body,
  .radar-grid,
  .focus-grid {
    grid-template-columns: 1fr;
  }

  .cockpit-topbar {
    flex-direction: column;
  }

  .cockpit-actions {
    justify-content: flex-start;
  }
}
</style>
