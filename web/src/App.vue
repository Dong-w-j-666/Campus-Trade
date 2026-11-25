<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { api } from './api'
import { logout, restoreSession, session } from './session'

const router = useRouter()
const route = useRoute()
const mobileOpen = ref(false)
const unreadCount = ref(0)
let unreadTimer = null

const initials = computed(() => session.user?.realName?.slice(-2) || '同学')
const isMarket = computed(() => route.name === 'home')
const unreadLabel = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))

onMounted(() => {
  restoreSession()
  unreadTimer = window.setInterval(loadUnread, 5000)
})

onUnmounted(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
})

watch(
  () => session.user?.id,
  async (userId) => {
    if (!userId) {
      unreadCount.value = 0
      return
    }
    await loadUnread()
  },
  { immediate: true }
)

async function loadUnread() {
  if (!session.user) {
    unreadCount.value = 0
    return
  }
  try {
    const result = await api.get('/messages/unread-count')
    unreadCount.value = Number(result.count || 0)
  } catch {
    // Keep the last known badge value when the network is temporarily unavailable.
  }
}

async function signOut() {
  await logout()
  mobileOpen.value = false
  router.push('/')
}

function closeMobile() {
  mobileOpen.value = false
}
</script>

<template>
  <div class="site-shell">
    <header class="site-header" :class="{ 'on-market': isMarket }">
      <div class="header-inner">
        <RouterLink class="brand" to="/" @click="closeMobile">
          <span class="brand-mark" aria-hidden="true">
            <span></span>
            <span></span>
          </span>
          <span class="brand-copy">
            <strong>校园集</strong>
            <small>CAMPUS MARKET</small>
          </span>
        </RouterLink>

        <button
          class="mobile-menu-button"
          type="button"
          :aria-expanded="mobileOpen"
          aria-label="打开导航"
          @click="mobileOpen = !mobileOpen"
        >
          <span></span>
          <span></span>
        </button>

        <nav class="main-nav" :class="{ open: mobileOpen }">
          <RouterLink to="/" @click="closeMobile">逛市场</RouterLink>
          <RouterLink to="/publish" @click="closeMobile">发布闲置</RouterLink>
          <RouterLink to="/orders" @click="closeMobile">我的订单</RouterLink>
          <RouterLink to="/messages" @click="closeMobile">
            消息
            <span v-if="unreadCount" class="nav-unread">{{ unreadLabel }}</span>
          </RouterLink>
          <RouterLink to="/my-goods" @click="closeMobile">我的发布</RouterLink>

          <div class="mobile-account">
            <template v-if="session.user">
              <div class="account-brief">
                <span class="avatar">{{ initials }}</span>
                <span>
                  <strong>{{ session.user.realName }}</strong>
                  <small>{{ session.user.campus }} · 信用 {{ session.user.creditScore }}</small>
                </span>
              </div>
              <button class="text-button" type="button" @click="signOut">退出登录</button>
            </template>
            <RouterLink v-else class="button button-dark button-small" to="/login" @click="closeMobile">
              登录 / 注册
            </RouterLink>
          </div>
        </nav>

        <div class="header-actions">
          <RouterLink class="publish-link" to="/publish">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            发布
          </RouterLink>

          <div v-if="session.user" class="user-chip">
            <span class="avatar">{{ initials }}</span>
            <span class="user-chip-copy">
              <strong>{{ session.user.realName }}</strong>
              <small>{{ session.user.campus }}</small>
            </span>
            <button class="logout-dot" type="button" title="退出登录" @click="signOut">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 5H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4M16 17l5-5-5-5M21 12H9" />
              </svg>
            </button>
          </div>
          <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
        </div>
      </div>
    </header>

    <main class="page-main">
      <RouterView />
    </main>

    <footer class="site-footer">
      <div>
        <span class="footer-logo">校园集</span>
        <p>让每一件闲置，都在校园里找到下一位主人。</p>
      </div>
      <div class="footer-meta">
        <span>Campus Trade · 2026</span>
        <span>课程原型持续迭代中</span>
      </div>
    </footer>
  </div>
</template>
