<script setup>
import companyLogo from '../img/logo.png'

defineProps({
  loginForm: { type: Object, required: true },
  loginError: { type: String, default: '' },
})

const emit = defineEmits(['submit'])
</script>

<template>
  <div class="login-page">
    <div class="login-hero">
      <section class="login-copy">
        <div class="login-logo-wrap">
          <img :src="companyLogo" alt="公司Logo" class="login-company-logo" />
        </div>
        <span class="hero-kicker">Project Operating Cockpit</span>
        <h1>项目经营协同平台</h1>
        <p>统一管理知识库、客户、项目进度与回款数据，支持老板驾驶舱、角色分工协作和全流程留痕闭环。</p>
        <div class="hero-points">
          <span>知识库沉淀与复用</span>
          <span>客户关系与跟进预警</span>
          <span>项目全阶段推进闭环</span>
          <span>经营指标与回款可视化</span>
        </div>
      </section>

      <el-card class="login-card" style="width:min(420px,92vw)">
        <template #header>
          <div class="card-header-block">
            <div>
              <h3>登录系统</h3>
              <p>请输入账号信息进入工作台</p>
            </div>
          </div>
        </template>
        <el-alert v-if="loginError" :title="loginError" type="error" show-icon class="login-alert" />
        <el-form label-position="top" @submit.prevent="emit('submit')">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" class="login-button" @click="emit('submit')">进入系统</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.login-logo-wrap {
  margin-bottom: 10px;
}

.login-company-logo {
  height: 52px;
  width: auto;
  object-fit: contain;
  filter: drop-shadow(0 6px 16px rgba(20, 63, 125, 0.18));
}
</style>
