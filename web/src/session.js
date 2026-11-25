import { readonly, reactive } from 'vue'
import { api } from './api'

const state = reactive({
  user: null,
  ready: false
})

let restorePromise = null

export async function restoreSession(force = false) {
  if (state.ready && !force) {
    return state.user
  }
  if (restorePromise) {
    return restorePromise
  }

  restorePromise = api
    .get('/auth/me')
    .then((user) => {
      state.user = user || null
      return state.user
    })
    .catch((error) => {
      if (error.status !== 401) {
        console.warn('无法恢复登录状态', error)
      }
      state.user = null
      return null
    })
    .finally(() => {
      state.ready = true
      restorePromise = null
    })

  return restorePromise
}

export async function login(payload) {
  const user = await api.post('/auth/login', payload)
  state.user = user
  state.ready = true
  return user
}

export async function register(payload) {
  const user = await api.post('/auth/register', payload)
  state.user = user
  state.ready = true
  return user
}

export async function logout() {
  await api.post('/auth/logout')
  state.user = null
}

export const session = readonly(state)
