import { createApp, h, reactive } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const loginForm = reactive({ username: 'admin', password: 'Admin@123' })

function App() {
  return h('div', { style: 'padding:40px;max-width:420px' }, [
    h('h3', 'element-only login probe'),
    h(
      'div',
      { class: 'el-card', style: 'padding:16px' },
      [
        h('div', { style: 'margin-bottom:12px' }, [
          h('label', '用户名'),
          h('input', {
            class: 'el-input__inner',
            style: 'width:100%;height:32px;margin-top:4px',
            value: loginForm.username,
            autocomplete: 'username',
            onInput: (e) => { loginForm.username = e.target.value },
          }),
        ]),
        h('div', { style: 'margin-bottom:12px' }, [
          h('label', '密码'),
          h('input', {
            class: 'el-input__inner',
            type: 'password',
            style: 'width:100%;height:32px;margin-top:4px',
            value: loginForm.password,
            autocomplete: 'current-password',
            onInput: (e) => { loginForm.password = e.target.value },
          }),
        ]),
        h('button', { class: 'el-button el-button--primary', type: 'button' }, '进入系统'),
      ],
    ),
  ])
}

const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
