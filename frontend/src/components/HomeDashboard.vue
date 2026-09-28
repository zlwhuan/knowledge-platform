<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

/** KPI/指标数字 count-up */
const numState = reactive(new Map())
function displayNum(raw) {
  const s = String(raw ?? '')
  const m = s.match(/^-?\d[\d,]*(\.\d+)?/)
  if (!m) return s
  const target = Number(m[0].replace(/,/g, ''))
  if (!Number.isFinite(target)) return s
  const key = s
  if (!numState.has(key)) {
    numState.set(key, 0)
    const t0 = performance.now()
    const step = (now) => {
      const p = Math.min(1, (now - t0) / 700)
      const eased = 1 - Math.pow(1 - p, 3)
      numState.set(key, target * eased)
      if (p < 1) requestAnimationFrame(step)
      else numState.set(key, target)
    }
    requestAnimationFrame(step)
  }
  const cur = numState.get(key) ?? target
  const isInt = Number.isInteger(target)
  const body = isInt
    ? Math.round(cur).toLocaleString('zh-CN')
    : cur.toLocaleString('zh-CN', { maximumFractionDigits: 1 })
  return s.replace(m[0], body)
}
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
    color: ['var(--t-strong)', 'var(--t-body)', 'var(--t-muted)', 'var(--t-faint)', 'var(--line-strong)', 'var(--line)'],
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
        <h2>经营全景</h2>
        <p>合同回款 · 项目风险 · 客户跟进，一屏决策</p>
      </div>
      <div class="cockpit-actions">
        <button type="button" class="top-btn top-btn-ghost" @click="emit('open-project-view', 'project-tracker')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3v18h18"/><path d="M7 14l4-4 4 4 5-6"/></svg>
          项目追踪
        </button>
        <button type="button" class="top-btn top-btn-ghost" @click="emit('open-system-view', 'customers')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="8" r="4"/><path d="M2 21v-2a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v2"/><circle cx="18" cy="9" r="3"/></svg>
          客户管理
        </button>
        <button type="button" class="top-btn top-btn-ghost" @click="emit('open-all-library')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19V5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14"/><path d="M4 19h16"/><path d="M8 7h8M8 11h8"/></svg>
          知识库
        </button>
        <button v-if="canCreateContent" type="button" class="top-btn top-btn-solid" @click="emit('start-create-content')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
          新增资料
        </button>
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
        <strong class="kpi-value">{{ displayNum(item.value) }}</strong>
        <small class="kpi-hint">{{ item.hint }}</small>
      </div>
    </div>

    <div class="stat-strip">
      <div v-for="item in secondaryStats" :key="item.label" class="stat-item">
        <span>{{ item.label }}</span>
        <strong>{{ displayNum(item.value) }}</strong>
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
              达成 {{ displayNum(collectionRate + '%') }}
            </el-tag>
          </div>
          <div class="finance-body">
            <div class="finance-numbers">
              <div class="finance-metric">
                <label>累计合同额</label>
                <strong>¥{{ displayNum(contractAmount.toLocaleString('zh-CN')) }}</strong>
              </div>
              <div class="finance-metric">
                <label>累计回款</label>
                <strong class="is-received">¥{{ displayNum(receivedAmount.toLocaleString('zh-CN')) }}</strong>
              </div>
              <div class="finance-metric">
                <label>待回款缺口</label>
                <strong class="is-gap">¥{{ displayNum(collectionGap.toLocaleString('zh-CN')) }}</strong>
              </div>
            </div>
            <div class="finance-progress-block">
              <div class="finance-progress-top">
                <span>回款达成率</span>
                <strong>{{ displayNum(collectionRate + '%') }}</strong>
              </div>
              <div class="mini-track">
                <div
                  class="mini-track-fill"
                  :class="collectionRate >= 70 ? 'is-ok' : collectionRate >= 40 ? 'is-warn' : 'is-bad'"
                  :style="{ width: `${Math.min(100, collectionRate)}%` }"
                ></div>
              </div>
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
                  <div class="stage-row-bar">
                    <div class="stage-row-bar-inner" :class="`tone-${bucket.tone}`" :style="{ width: `${bucket.percent}%` }"></div>
                  </div>
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
/* ===== 高级感视觉规范 =====
   原则：近白画布、无重描边、阴影几乎不可见、
   去掉色条/渐变/虚线，数字与留白当主角 */
.home-cockpit {
  --ink: var(--t-strong);
  --ink-2: var(--t-body);
  --muted: var(--t-muted);
  --faint: var(--t-faint);
  --line: rgba(17, 24, 39, 0.06);
  --line-2: rgba(17, 24, 39, 0.1);
  --surface: var(--bg-card);
  --canvas: var(--bg-page);
  --soft: var(--bg-soft);
  --accent: var(--t-muted);
  --accent-ink: var(--t-body);
  --danger: var(--sem-danger);
  --warning: var(--sem-warning);
  --success: var(--sem-success);
  --r: 14px;
  --r-sm: 10px;
  --shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  --shadow-2: 0 4px 16px rgba(16, 24, 40, 0.06);
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-family: -apple-system, "SF Pro Text", "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* —— 顶栏：无框，像产品页头 —— */
.cockpit-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 4px 2px 2px;
}

.cockpit-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 650;
  color: var(--ink);
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.cockpit-title p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--faint);
  letter-spacing: 0.01em;
}

.cockpit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
  align-items: center;
}

.top-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 11px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.top-btn-ghost {
  background: transparent;
  border: 1px solid var(--line-2);
  color: var(--ink-2);
}

.top-btn-ghost:hover {
  background: var(--soft);
  border-color: var(--t-muted);
}

.top-btn-solid {
  background: var(--ink);
  border: 1px solid var(--ink);
  color: #fff;
}

.top-btn-solid:hover {
  background: var(--t-strong);
}

.top-btn:focus-visible {
  outline: 2px solid rgba(59, 130, 246, 0.35);
  outline-offset: 2px;
}

/* —— KPI：无边框卡片，数字即视觉 —— */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.kpi-card {
  padding: 14px 16px 12px;
  border-radius: var(--r);
  background: var(--surface);
  box-shadow: var(--shadow);
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 2px;
  transition: box-shadow 0.2s ease;
}

.kpi-card:hover {
  box-shadow: var(--shadow-2);
}

/* 语义色只染数字，不再画左侧色条 */
.kpi-card.tone-danger .kpi-value { color: var(--danger); }
.kpi-card.tone-warning .kpi-value { color: var(--warning); }
.kpi-card.tone-accent .kpi-value { color: var(--ink); }

.kpi-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--faint);
  letter-spacing: 0.02em;
}

.kpi-value {
  font-size: 21px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
  margin: 2px 0;
}

.kpi-hint {
  font-size: 11px;
  color: var(--faint);
}

/* —— 次级指标：无框一行，靠字重分层 —— */
.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.stat-item {
  padding: 10px 16px;
  border-right: 1px solid var(--line);
}

.stat-item:last-child {
  border-right: none;
}

.stat-item span,
.stat-item small {
  display: block;
  color: var(--faint);
  font-size: 11px;
}

.stat-item strong {
  display: block;
  margin: 1px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

/* —— 布局 —— */
.cockpit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr);
  gap: 10px;
  align-items: start;
}

.cockpit-main,
.cockpit-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

/* —— 面板：白底、极细边、无阴影堆叠感 —— */
.hd-panel {
  border: 1px solid var(--line);
  border-radius: var(--r);
  background: var(--surface);
  box-shadow: var(--shadow);
  padding: 14px 16px;
}

.hd-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hd-panel-head h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.hd-panel-head p {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--faint);
}

.radar-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
}

.radar-count,
.side-count {
  font-size: 11px;
  color: var(--faint);
  font-variant-numeric: tabular-nums;
  background: var(--soft);
  padding: 2px 8px;
  border-radius: 999px;
}

/* —— 经营结果 —— */
.finance-body {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 10px;
}

.finance-numbers {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.finance-metric {
  padding: 12px 14px;
  border-radius: var(--r-sm);
  background: var(--soft);
}

.finance-metric label {
  display: block;
  font-size: 11px;
  color: var(--faint);
}

.finance-metric strong {
  display: block;
  margin-top: 3px;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.finance-metric strong.is-received { color: var(--success); }
.finance-metric strong.is-gap { color: var(--warning); }

.finance-progress-block {
  padding: 12px 14px;
  border-radius: var(--r-sm);
  background: var(--soft);
  border: 1px solid var(--line);
}

.finance-progress-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
}

.finance-progress-block .mini-track {
  margin-top: 2px;
  margin-bottom: 8px;
}

.finance-progress-top strong {
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.finance-caption {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--faint);
}

/* —— 雷达 —— */
.radar-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.radar-block {
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  padding: 12px 14px;
  background: var(--soft);
}

.radar-block h4 {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--faint);
  letter-spacing: 0.02em;
}

.stage-distribution {
  display: flex;
  flex-direction: column;
  gap: 7px;
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
  font-size: 11px;
  color: var(--muted);
}

.stage-row-value,
.bucket-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.bucket-value.tone-danger { color: var(--danger); }
.bucket-value.tone-warning { color: var(--warning); }
.bucket-value.tone-success { color: var(--success); }

.stage-row-bar {
  height: 4px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.08);
  overflow: hidden;
}

.stage-row-bar-inner {
  height: 100%;
  border-radius: 999px;
  background: var(--ink);
  transition: width 0.5s ease;
}

.stage-row-bar-inner.tone-danger { background: var(--danger); }
.stage-row-bar-inner.tone-warning { background: var(--warning); }
.stage-row-bar-inner.tone-success { background: var(--success); }

.mini-track {
  height: 5px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.08);
  overflow: hidden;
}

.mini-track-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s ease;
}

.mini-track-fill.is-ok { background: var(--success); }
.mini-track-fill.is-warn { background: var(--warning); }
.mini-track-fill.is-bad { background: var(--danger); }

.finance-progress-block {
  position: relative;
}


.stage-chart {
  height: 210px;
  margin-top: 6px;
}

.bucket-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.bucket-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 48px;
  gap: 8px;
  align-items: center;
}

/* —— 焦点项目：靠 hover 表现，无强边框 —— */
.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.focus-card {
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  padding: 11px 13px;
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.focus-card:hover {
  border-color: var(--line-2);
  box-shadow: var(--shadow-2);
}

.focus-card:focus-visible {
  outline: 2px solid rgba(59, 130, 246, 0.35);
  outline-offset: 2px;
}

.focus-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.focus-head strong {
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.focus-meta,
.focus-owner {
  margin-top: 4px;
  font-size: 11px;
  color: var(--faint);
}

.focus-card p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

/* —— 侧栏列表：去盒子，行内分隔 —— */
.side-list {
  display: flex;
  flex-direction: column;
}

.side-row,
.side-action,
.side-feed {
  width: 100%;
  border: none;
  border-top: 1px solid var(--line);
  border-radius: 0;
  background: transparent;
  padding: 9px 2px;
}

.side-row:first-child,
.side-action:first-child,
.side-feed:first-child {
  border-top: none;
  padding-top: 2px;
}

.side-action,
.side-feed {
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
  border-radius: 8px;
  padding-left: 8px;
  padding-right: 8px;
}

.side-action:hover,
.side-feed:hover {
  background: var(--soft);
}

.side-action:focus-visible,
.side-feed:focus-visible {
  outline: 2px solid rgba(59, 130, 246, 0.35);
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
  gap: 1px;
}

.side-row-main strong,
.side-action strong,
.feed-top strong {
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
}

.side-row-main span,
.side-action span,
.side-feed small {
  color: var(--faint);
  font-size: 11px;
}

.side-row-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  color: var(--faint);
  font-size: 11px;
}

.side-action span {
  display: block;
  margin-top: 3px;
  line-height: 1.45;
}

.feed-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: baseline;
}

.feed-top span {
  color: var(--accent-ink);
  font-size: 11px;
}

.side-feed p {
  margin: 4px 0 2px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

.hd-empty {
  padding: 16px 8px;
  text-align: center;
  color: var(--faint);
  font-size: 12px;
  border: 1px dashed var(--line-2);
  border-radius: var(--r-sm);
  background: var(--soft);
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

  .stat-item:nth-child(2n) {
    border-right: none;
  }

  .stat-item:nth-child(n + 3) {
    border-top: 1px solid var(--line);
  }

  .cockpit-layout,
  .finance-body,
  .radar-grid,
  .focus-grid {
    grid-template-columns: 1fr;
  }

  .cockpit-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .cockpit-actions {
    justify-content: flex-start;
  }
}
</style>
