<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register, session } from '../session'

const route = useRoute()
const router = useRouter()
const mode = ref(route.query.mode === 'register' ? 'register' : 'login')
const submitting = ref(false)
const message = ref('')
const fieldErrors = ref({})

const loginForm = reactive({
  account: '',
  password: ''
})

const registerForm = reactive({
  studentNo: '',
  realName: '',
  phone: '',
  password: '',
  campus: '主校区'
})

function switchMode(nextMode) {
  mode.value = nextMode
  message.value = ''
  fieldErrors.value = {}
}

function redirectAfterAuth() {
  const redirect = route.query.redirect
  router.push(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
}

function useDemo(account) {
  loginForm.account = account
  loginForm.password = 'Campus@123'
  message.value = '演示账号已填入，点击登录即可'
}

async function submitLogin() {
  submitting.value = true
  message.value = ''
  fieldErrors.value = {}
  try {
    await login(loginForm)
    redirectAfterAuth()
  } catch (error) {
    message.value = error.message
    fieldErrors.value = error.fields || {}
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  submitting.value = true
  message.value = ''
  fieldErrors.value = {}
  try {
    await register(registerForm)
    redirectAfterAuth()
  } catch (error) {
    message.value = error.message
    fieldErrors.value = error.fields || {}
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-poster">
      <div class="poster-grid"></div>
      <div class="poster-top">
        <span>CAMPUS CIRCULATION</span>
        <span>VOL. 01</span>
      </div>
      <div class="poster-copy">
        <span class="poster-kicker">LESS NEW. MORE STORIES.</span>
        <h1>让闲置<br /><em>继续发光。</em></h1>
        <p>一个更近、更真实、更有温度的校园交易空间。</p>
      </div>
      <div class="poster-object" aria-hidden="true">
        <div class="poster-disc"></div>
        <span class="object-label label-one">BOOKS</span>
        <span class="object-label label-two">TECH</span>
        <span class="object-label label-three">DAILY</span>
      </div>
      <div class="poster-bottom">
        <span>同校见面</span>
        <span>身份可见</span>
        <span>轻松流转</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-panel-inner">
        <div v-if="session.user" class="already-signed-in">
          <span class="mini-avatar large">{{ session.user.realName.slice(-1) }}</span>
          <h2>你已登录</h2>
          <p>{{ session.user.realName }} · {{ session.user.studentNo }}</p>
          <RouterLink class="button button-dark" to="/">继续逛市场</RouterLink>
        </div>

        <template v-else>
          <div class="auth-tabs">
            <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">
              登录
            </button>
            <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">
              注册
            </button>
          </div>

          <div class="auth-heading">
            <span class="section-index">WELCOME TO CAMPUS</span>
            <h2>{{ mode === 'login' ? '回来继续逛逛' : '加入校园集' }}</h2>
            <p>
              {{
                mode === 'login'
                  ? '使用学号或手机号登录你的校园账户'
                  : '用真实校园身份，开始你的第一笔交易'
              }}
            </p>
          </div>

          <div v-if="message" class="form-message" role="alert">{{ message }}</div>

          <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
            <label class="field">
              <span>学号 / 手机号</span>
              <input
                v-model.trim="loginForm.account"
                autocomplete="username"
                placeholder="例如：20240004"
              />
              <small v-if="fieldErrors.account">{{ fieldErrors.account }}</small>
            </label>
            <label class="field">
              <span>密码</span>
              <input
                v-model="loginForm.password"
                type="password"
                autocomplete="current-password"
                placeholder="输入登录密码"
              />
              <small v-if="fieldErrors.password">{{ fieldErrors.password }}</small>
            </label>
            <button class="button button-dark button-wide" type="submit" :disabled="submitting">
              {{ submitting ? '正在登录…' : '进入校园集' }}
            </button>

            <div class="demo-login">
              <div>
                <strong>课程演示账号</strong>
                <span>密码均为 Campus@123</span>
              </div>
              <div class="demo-buttons">
                <button type="button" @click="useDemo('20240004')">买家 · 陈浩</button>
                <button type="button" @click="useDemo('20240002')">卖家 · 李晓明</button>
              </div>
            </div>
          </form>

          <form v-else class="auth-form register-form" @submit.prevent="submitRegister">
            <div class="form-row two-columns">
              <label class="field">
                <span>姓名</span>
                <input v-model.trim="registerForm.realName" autocomplete="name" placeholder="你的真实姓名" />
                <small v-if="fieldErrors.realName">{{ fieldErrors.realName }}</small>
              </label>
              <label class="field">
                <span>8 位学号</span>
                <input v-model.trim="registerForm.studentNo" inputmode="numeric" placeholder="2024xxxx" />
                <small v-if="fieldErrors.studentNo">{{ fieldErrors.studentNo }}</small>
              </label>
            </div>
            <label class="field">
              <span>手机号</span>
              <input v-model.trim="registerForm.phone" inputmode="tel" autocomplete="tel" placeholder="用于登录和联系" />
              <small v-if="fieldErrors.phone">{{ fieldErrors.phone }}</small>
            </label>
            <div class="form-row two-columns">
              <label class="field">
                <span>校区</span>
                <select v-model="registerForm.campus">
                  <option>主校区</option>
                  <option>东校区</option>
                  <option>西校区</option>
                </select>
              </label>
              <label class="field">
                <span>设置密码</span>
                <input
                  v-model="registerForm.password"
                  type="password"
                  autocomplete="new-password"
                  placeholder="至少 8 位，含字母和数字"
                />
                <small v-if="fieldErrors.password">{{ fieldErrors.password }}</small>
              </label>
            </div>
            <button class="button button-dark button-wide" type="submit" :disabled="submitting">
              {{ submitting ? '正在创建账户…' : '创建校园账户' }}
            </button>
            <p class="terms-note">注册即表示你同意使用真实校园信息，并遵守校园交易规范。</p>
          </form>
        </template>
      </div>
    </section>
  </div>
</template>
