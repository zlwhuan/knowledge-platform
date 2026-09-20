import { createApp, h, reactive } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import LoginView from './components/LoginView.vue'
import './style.css'

const loginForm = reactive({ username: 'admin', password: 'Admin@123' })

const app = createApp({
  render() {
    return h(LoginView, {
      loginForm,
      loginError: '',
      onSubmit: () => {},
    })
  },
})
app.use(ElementPlus)
app.mount('#app')
