<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

const props = defineProps({
  selectedProject: { type: Object, default: null },
})

const chartHeight = 240
const padding = { top: 24, right: 24, bottom: 40, left: 44 }
const seriesColors = ['#2563eb', '#16a34a', '#dc2626', '#9333ea', '#ea580c', '#0f766e']

// viewBox 宽度跟随容器像素宽，保证与显示尺寸 1:1，不纵向拉伸
const svgBoxRef = ref(null)
const chartWidth = ref(1200)
let resizeObserver = null

function syncChartWidth() {
  const el = svgBoxRef.value
  if (!el) return
  const w = Math.round(el.clientWidth || el.getBoundingClientRect().width || 1200)
  chartWidth.value = Math.max(720, w)
}

onMounted(() => {
  syncChartWidth()
  if (typeof ResizeObserver !== 'undefined' && svgBoxRef.value) {
    resizeObserver = new ResizeObserver(() => syncChartWidth())
    resizeObserver.observe(svgBoxRef.value)
  } else {
    window.addEventListener('resize', syncChartWidth)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  window.removeEventListener('resize', syncChartWidth)
})

const tooltip = reactive({
  visible: false,
  x: 0,
  y: 0,
  title: '',
  lines: [],
})

function showTooltip(event, title, lines = []) {
  tooltip.visible = true
  tooltip.x = event.clientX + 14
  tooltip.y = event.clientY + 14
  tooltip.title = title
  tooltip.lines = lines
}

function moveTooltip(event) {
  if (!tooltip.visible) return
  tooltip.x = event.clientX + 14
  tooltip.y = event.clientY + 14
}

function hideTooltip() {
  tooltip.visible = false
}

function toDate(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDateTime(value) {
  const date = toDate(value)
  if (!date) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const progressRecords = computed(() => {
  return (props.selectedProject?.progressRecords || [])
    .map((item) => ({
      ...item,
      date: toDate(item.recordTime || item.createdAt),
      progressValue: Number(item.progress ?? 0),
      phaseLabel: String(item.phase || item.stage || '未分组'),
    }))
    .filter((item) => item.date)
    .sort((a, b) => a.date - b.date)
})

const chartData = computed(() => {
  const records = progressRecords.value
  if (!records.length) {
    return {
      hasData: false,
      series: [],
      xTicks: [],
      gridLines: [],
    }
  }

  const minTs = records[0].date.getTime()
  const maxTs = records[records.length - 1].date.getTime()
  const plotWidth = chartWidth.value - padding.left - padding.right
  const plotHeight = chartHeight - padding.top - padding.bottom

  const xFromTs = (ts) => {
    if (maxTs === minTs) return padding.left + plotWidth / 2
    return padding.left + ((ts - minTs) / (maxTs - minTs)) * plotWidth
  }
  const yFromProgress = (progressValue) => {
    const safe = Math.max(0, Math.min(100, Number(progressValue || 0)))
    return padding.top + plotHeight - (safe / 100) * plotHeight
  }

  const groupMap = new Map()
  records.forEach((item) => {
    const list = groupMap.get(item.phaseLabel) || []
    list.push(item)
    groupMap.set(item.phaseLabel, list)
  })

  const series = Array.from(groupMap.entries()).map(([phase, list], idx) => {
    const points = list.map((item) => ({
      ...item,
      x: xFromTs(item.date.getTime()),
      y: yFromProgress(item.progressValue),
    }))
    const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
    return {
      phase,
      color: seriesColors[idx % seriesColors.length],
      points,
      path,
    }
  })

  const total = records.length
  const maxLabels = 10
  const step = total <= maxLabels ? 1 : Math.ceil(total / maxLabels)
  const xTicks = []
  for (let i = 0; i < total; i += step) {
    const item = records[i]
    xTicks.push({
      x: xFromTs(item.date.getTime()),
      label: formatDateTime(item.date).slice(5, 10),
    })
  }
  const last = records[total - 1]
  const lastLabel = formatDateTime(last.date).slice(5, 10)
  if (!xTicks.length || xTicks[xTicks.length - 1].label !== lastLabel) {
    const lastX = xFromTs(last.date.getTime())
    const prev = xTicks[xTicks.length - 1]
    if (!prev || lastX - prev.x > 28) {
      xTicks.push({ x: lastX, label: lastLabel })
    }
  }

  const gridLines = [0, 25, 50, 75, 100].map((value) => ({
    value,
    y: yFromProgress(value),
  }))

  return {
    hasData: true,
    series,
    xTicks,
    gridLines,
  }
})
</script>

<template>
  <el-card shadow="never" class="panel-card project-trend-card" style="margin-bottom: 10px;">
    <template #header>
      <div class="card-header-block">
        <div>
          <h3>项目阶段进度趋势图</h3>
        </div>
      </div>
    </template>

    <div class="project-trend-card-content">
      <el-empty v-if="!selectedProject" description="请先在左侧选中项目" />
      <el-empty v-else-if="!chartData.hasData" description="当前项目暂无进度记录，无法绘制趋势线" />

      <div v-else class="trend-chart-wrap">
        <div class="trend-legend">
          <span v-for="item in chartData.series" :key="item.phase" class="trend-legend-item">
            <i :style="`background:${item.color}`"></i>
            {{ item.phase }}
          </span>
        </div>

        <div ref="svgBoxRef" class="trend-svg-box">
          <svg
            :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
            class="trend-svg"
            preserveAspectRatio="xMidYMid meet"
          >
            <g>
              <line
                v-for="line in chartData.gridLines"
                :key="`grid-${line.value}`"
                :x1="padding.left"
                :y1="line.y"
                :x2="chartWidth - padding.right"
                :y2="line.y"
                stroke="#e8eef6"
                stroke-width="1"
              />
              <text
                v-for="line in chartData.gridLines"
                :key="`label-${line.value}`"
                :x="padding.left - 10"
                :y="line.y + 4"
                text-anchor="end"
                font-size="12"
                fill="#7b8a9d"
              >{{ line.value }}</text>
            </g>

            <g>
              <path
                v-for="item in chartData.series"
                :key="`line-${item.phase}`"
                :d="item.path"
                fill="none"
                :stroke="item.color"
                stroke-width="2.5"
              />
              <g v-for="item in chartData.series" :key="`dots-${item.phase}`">
                <circle
                  v-for="point in item.points"
                  :key="`dot-${item.phase}-${point.id}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="6"
                  :fill="item.color"
                  stroke="#fff"
                  stroke-width="2"
                  style="cursor: pointer;"
                  @mouseenter="showTooltip($event, item.phase, [`时间：${formatDateTime(point.recordTime || point.createdAt)}`, `进度：${point.progressValue}%`, `摘要：${point.summary || '--'}`, `下一步：${point.nextAction || '--'}`])"
                  @mousemove="moveTooltip"
                  @mouseleave="hideTooltip"
                />
              </g>
            </g>

            <g>
              <line
                :x1="padding.left"
                :y1="chartHeight - padding.bottom"
                :x2="chartWidth - padding.right"
                :y2="chartHeight - padding.bottom"
                stroke="#c5d2e5"
                stroke-width="1"
              />
              <text
                v-for="(tick, idx) in chartData.xTicks"
                :key="`tick-${idx}`"
                :x="tick.x"
                :y="chartHeight - 14"
                text-anchor="middle"
                font-size="12"
                fill="#6b7c93"
              >{{ tick.label }}</text>
            </g>
          </svg>

          <div
            v-if="tooltip.visible"
            :style="{
              position: 'fixed',
              left: `${tooltip.x}px`,
              top: `${tooltip.y}px`,
              zIndex: 9999,
              background: '#0f172a',
              color: '#f8fafc',
              padding: '10px 12px',
              borderRadius: '10px',
              minWidth: '220px',
              boxShadow: '0 10px 30px rgba(15,23,42,.35)',
              pointerEvents: 'none',
              fontSize: '12px',
              lineHeight: '1.55',
            }"
          >
            <div style="font-weight: 700; margin-bottom: 4px;">{{ tooltip.title }}</div>
            <div v-for="(line, idx) in tooltip.lines" :key="idx" style="opacity: .95;">{{ line }}</div>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.trend-chart-wrap {
  width: 100%;
}

.trend-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
  min-height: 20px;
}

.trend-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #5f6f84;
}

.trend-legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.trend-svg-box {
  position: relative;
  width: 100%;
  height: 240px;
}

.trend-svg {
  width: 100% !important;
  height: 240px !important;
  display: block !important;
}

@media (min-width: 1920px) {
  .trend-svg-box,
  .trend-svg {
    height: 260px !important;
  }
}
</style>
