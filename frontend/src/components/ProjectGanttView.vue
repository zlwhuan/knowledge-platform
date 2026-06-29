<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  projects: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['select-project'])

const chartRef = ref(null)
let chart = null

const stageColors = {
  '商机立项': '#909399',
  '合同执行': '#E6A23C',
  '实施交付': '#409EFF',
  '验收结算': '#67C23A',
  '售后维保': '#F56C6C',
}

const sortedProjects = computed(() => {
  return [...props.projects]
    .sort((a, b) => {
      if (a.startDate && b.startDate) return new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
      if (a.startDate) return -1
      if (b.startDate) return 1
      return 0
    })
})

function renderChart() {
  if (!chartRef.value || !sortedProjects.value.length) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)

  const categories = sortedProjects.value.map(p => p.name || '未命名')
  const barData = sortedProjects.value.map((p, idx) => {
    const start = p.startDate ? new Date(p.startDate).getTime() : new Date().getTime() - 30 * 86400000
    const end = p.plannedEndDate ? new Date(p.plannedEndDate).getTime() : new Date().getTime() + 30 * 86400000
    return {
      name: p.name,
      value: [idx, start, end, end - start],
      itemStyle: { color: stageColors[p.stage] || '#909399' },
      project: p,
    }
  })

  const allStarts = barData.map(d => d.value[1])
  const allEnds = barData.map(d => d.value[2])
  const minTime = Math.min(...allStarts) - 7 * 86400000
  const maxTime = Math.max(...allEnds) + 14 * 86400000
  const today = new Date().getTime()

  chart.setOption({
    tooltip: {
      formatter(params) {
        const p = params.data?.project
        if (!p) return ''
        const latestRecord = (p.progressRecords || [])[0]
        const lines = [
          `<strong>${p.name}</strong>`,
          `客户: ${p.customerName || '--'}`,
          `阶段: ${p.stage || '--'} / 状态: ${p.status || '--'}`,
          `进度: ${p.progress || 0}% | 风险: ${p.riskLevel || '低'}`,
          `周期: ${p.startDate || '未设置'} → ${p.plannedEndDate || '未设置'}`,
          `项目经理: ${p.projectManager || '--'}`,
        ]
        if (latestRecord) lines.push(`最新进展: ${latestRecord.summary || '--'}`)
        return lines.join('<br/>')
      }
    },
    grid: { left: 180, right: 40, top: 40, bottom: 60 },
    xAxis: {
      type: 'time',
      min: minTime,
      max: maxTime,
      axisLabel: { formatter: (val) => { const d = new Date(val); return `${d.getMonth() + 1}/${d.getDate()}` } },
      splitLine: { show: true, lineStyle: { color: '#f0f0f0' } },
    },
    yAxis: {
      type: 'category',
      data: categories,
      inverse: true,
      axisLabel: { width: 160, overflow: 'truncate', fontSize: 12 },
    },
    series: [
      {
        type: 'custom',
        name: '项目周期',
        renderItem(params, api) {
          const categoryIdx = api.value(0)
          const start = api.coord([api.value(1), categoryIdx])
          const end = api.coord([api.value(2), categoryIdx])
          const barHeight = api.size([0, 1])[1] * 0.6
          const rect = echarts.graphic.clipRectByRect(
            { x: start[0], y: start[1] - barHeight / 2, width: Math.max(end[0] - start[0], 4), height: barHeight },
            { x: params.coordSys.x, y: params.coordSys.y, width: params.coordSys.width, height: params.coordSys.height }
          )
          if (!rect) return null
          return {
            type: 'group',
            children: [
              { type: 'rect', shape: rect, style: api.style() },
              {
                type: 'text',
                style: {
                  x: rect.x + 6,
                  y: rect.y + rect.height / 2,
                  text: `${params.data?.project?.progress || 0}%`,
                  fill: '#fff',
                  fontSize: 11,
                  textVerticalAlign: 'middle',
                }
              }
            ]
          }
        },
        encode: { x: [1, 2], y: 0 },
        data: barData,
      },
      {
        type: 'line',
        name: '今天',
        data: [[today, 0], [today, categories.length - 1]],
        lineStyle: { color: '#F56C6C', width: 2, type: 'dashed' },
        symbol: 'none',
        z: 10,
      }
    ],
  })

  chart.on('click', (params) => {
    if (params.data?.project) emit('select-project', params.data.project)
  })
}

watch(() => [props.projects, props.loading], () => { nextTick(renderChart) }, { deep: true })

onMounted(() => {
  nextTick(renderChart)
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
  window.removeEventListener('resize', () => chart?.resize())
})
</script>

<template>
  <section class="page-section project-gantt-page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="card-header-block">
          <div>
            <h3>项目甘特图</h3>
            <p>可视化项目时间线，点击条形选中项目查看详情</p>
          </div>
        </div>
      </template>
      <div v-if="loading" style="text-align:center;padding:40px;">
        <p>加载中...</p>
      </div>
      <div v-else-if="!sortedProjects.length" style="text-align:center;padding:40px;">
        <el-empty description="暂无项目数据" />
      </div>
      <div v-else>
        <div style="display:flex;gap:16px;margin-bottom:12px;flex-wrap:wrap;">
          <div v-for="(color, stage) in stageColors" :key="stage" style="display:flex;align-items:center;gap:4px;">
            <span :style="{ background: color, width: '12px', height: '12px', borderRadius: '2px', display: 'inline-block' }"></span>
            <small>{{ stage }}</small>
          </div>
          <div style="display:flex;align-items:center;gap:4px;">
            <span style="border-top: 2px dashed #F56C6C; width: 20px; display: inline-block;"></span>
            <small>今天</small>
          </div>
        </div>
        <div ref="chartRef" :style="{ height: Math.max(400, sortedProjects.length * 42 + 100) + 'px', width: '100%' }"></div>
      </div>
    </el-card>
  </section>
</template>
