import { createApp, h, reactive } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './components/LoginView.vue'
import './style.css'

const routes = [
  { path: '/', name: 'Home', component: { render: () => null }, meta: { view: 'home' } },
  { path: '/library', component: { render: () => null }, meta: { view: 'library' } },
]

const router = createRouter({ history: createWebHistory(), routes })
const loginForm = reactive({ username: 'admin', password: 'Admin@123' })

const app = createApp({
  render() {
    return h(LoginView, { loginForm, loginError: '', onSubmit: () => {} })
  },
})
app.use(ElementPlus)
app.use(router)
app.mount('#app')
