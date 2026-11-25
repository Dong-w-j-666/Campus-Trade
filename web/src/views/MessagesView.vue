<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { compactDate, dateTime, orderStatus } from '../format'
import { session } from '../session'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.query.tab === 'system' ? 'system' : 'trade')
const conversations = ref([])
const notifications = ref([])
const messages = ref([])
const loadingConversations = ref(true)
const loadingThread = ref(false)
const error = ref('')
const threadError = ref('')
const draft = ref('')
const sending = ref(false)
const messageScroll = ref(null)
let refreshTimer = null

const selectedOrderId = computed(() => {
  const value = Number(route.query.order)
  return Number.isInteger(value) && value > 0 ? value : null
})

const selectedConversation = computed(() =>
  conversations.value.find((conversation) => conversation.orderId === selectedOrderId.value)
)

const totalUnread = computed(() =>
  conversations.value.reduce((total, conversation) => total + conversation.unreadCount, 0)
)

function initials(name) {
  return name?.slice(-2) || '同学'
}

function scrollToBottom() {
  nextTick(() => {
    if (messageScroll.value) {
      messageScroll.value.scrollTop = messageScroll.value.scrollHeight
    }
  })
}

async function loadConversations(silent = false) {
  if (!silent) loadingConversations.value = true
  error.value = ''
  try {
    conversations.value = await api.get('/messages/conversations')
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    if (!silent) loadingConversations.value = false
  }
}

async function loadThread(orderId, silent = false) {
  if (!orderId) {
    messages.value = []
    return
  }
  if (!silent) loadingThread.value = true
  threadError.value = ''
  try {
    const result = await api.get(`/messages/orders/${orderId}`)
    const hasUnreadIncoming = result.some((message) => !message.mine && !message.read)
    messages.value = result.map((message) => ({
      ...message,
      read: message.mine ? message.read : true
    }))
    if (hasUnreadIncoming) {
      await api.post(`/messages/orders/${orderId}/read`)
    }
    if (!silent) scrollToBottom()
  } catch (requestError) {
    threadError.value = requestError.message
  } finally {
    if (!silent) loadingThread.value = false
  }
}

async function loadNotifications(silent = false) {
  if (!silent) loadingThread.value = true
  threadError.value = ''
  try {
    const result = await api.get('/messages/notifications')
    notifications.value = result
    if (result.some((notification) => !notification.read)) {
      await api.post('/messages/notifications/read')
    }
  } catch (requestError) {
    threadError.value = requestError.message
  } finally {
    if (!silent) loadingThread.value = false
  }
}

async function selectConversation(conversation) {
  activeTab.value = 'trade'
  if (selectedOrderId.value === conversation.orderId) {
    await loadThread(conversation.orderId)
    return
  }
  await router.replace({
    name: 'messages',
    query: { order: conversation.orderId }
  })
}

async function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'system') {
    if (route.query.tab !== 'system') {
      await router.replace({ name: 'messages', query: { tab: 'system' } })
    } else {
      await loadNotifications()
    }
  } else {
    await router.replace({ name: 'messages', query: {} })
    if (!selectedOrderId.value && conversations.value[0]) {
      await selectConversation(conversations.value[0])
    }
  }
}

async function sendMessage() {
  const content = draft.value.trim()
  if (!content || !selectedOrderId.value || sending.value) return
  sending.value = true
  threadError.value = ''
  try {
    const message = await api.post(`/messages/orders/${selectedOrderId.value}`, { content })
    messages.value = [...messages.value, message]
    draft.value = ''
    await loadConversations(true)
    scrollToBottom()
  } catch (requestError) {
    threadError.value = requestError.message
  } finally {
    sending.value = false
  }
}

async function refresh() {
  if (!session.user) return
  await loadConversations(true)
  if (activeTab.value === 'system') {
    await loadNotifications(true)
  } else if (selectedOrderId.value) {
    await loadThread(selectedOrderId.value, true)
  }
}

async function initialize() {
  await loadConversations()
  if (activeTab.value === 'system') {
    await loadNotifications()
    return
  }
  const orderId = selectedOrderId.value || conversations.value[0]?.orderId
  if (orderId) {
    if (selectedOrderId.value !== orderId) {
      await router.replace({ name: 'messages', query: { order: orderId } })
    } else {
      await loadThread(orderId)
    }
  }
}

watch(
  () => route.query.order,
  async (value) => {
    if (activeTab.value === 'trade' && value) {
      await loadThread(Number(value))
    }
  }
)

watch(
  () => route.query.tab,
  async (value) => {
    if (value === 'system') {
      activeTab.value = 'system'
      await loadNotifications()
    }
  }
)

onMounted(async () => {
  await initialize()
  refreshTimer = window.setInterval(refresh, 4000)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<template>
  <div class="messages-page page-container">
    <header class="page-heading messages-heading">
      <div>
        <span class="section-index">INBOX / CAMPUS TRADE</span>
        <h1>消息中心</h1>
        <p>和交易对方约定时间地点，重要消息会留在这里。</p>
      </div>
      <RouterLink class="button button-outline" to="/orders">返回订单</RouterLink>
    </header>

    <div class="message-tabs" role="tablist" aria-label="消息类型">
      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'trade'"
        :class="{ active: activeTab === 'trade' }"
        @click="switchTab('trade')"
      >
        交易会话
        <span v-if="totalUnread" class="message-tab-count">{{ totalUnread }}</span>
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'system'"
        :class="{ active: activeTab === 'system' }"
        @click="switchTab('system')"
      >
        系统通知
      </button>
    </div>

    <div v-if="error" class="form-message wide" role="alert">{{ error }}</div>

    <div class="message-layout" :class="{ 'system-only': activeTab === 'system' }">
      <aside v-if="activeTab === 'trade'" class="conversation-panel">
        <div class="conversation-panel-heading">
          <span>交易对话</span>
          <small>{{ conversations.length }} 个订单</small>
        </div>

        <div v-if="loadingConversations" class="conversation-loading">
          <div v-for="index in 4" :key="index" class="conversation-skeleton shimmer"></div>
        </div>
        <div v-else-if="conversations.length === 0" class="message-empty compact">
          <span class="state-symbol">0</span>
          <p>有订单后，就可以在这里和对方沟通。</p>
          <RouterLink class="button button-dark button-small" to="/">去逛市场</RouterLink>
        </div>
        <div v-else class="conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.orderId"
            type="button"
            class="conversation-item"
            :class="{ active: conversation.orderId === selectedOrderId }"
            @click="selectConversation(conversation)"
          >
            <span class="conversation-avatar">
              <img
                v-if="conversation.goodsImage"
                :src="conversation.goodsImage"
                :alt="conversation.goodsTitle"
              />
              <template v-else>{{ conversation.goodsTitle.slice(0, 1) }}</template>
            </span>
            <span class="conversation-copy">
              <strong>{{ conversation.counterpartName }}</strong>
              <small>{{ conversation.goodsTitle }}</small>
              <span>{{ conversation.lastMessage || '还没有对话，打个招呼吧' }}</span>
            </span>
            <span class="conversation-meta">
              <small>{{ compactDate(conversation.lastMessageAt) }}</small>
              <b v-if="conversation.unreadCount">{{ conversation.unreadCount }}</b>
            </span>
          </button>
        </div>
      </aside>

      <section v-if="activeTab === 'trade'" class="thread-panel">
        <template v-if="selectedConversation">
          <header class="thread-header">
            <div class="thread-person">
              <span class="mini-avatar large">{{ initials(selectedConversation.counterpartName) }}</span>
              <div>
                <span class="section-index">ORDER CONVERSATION</span>
                <h2>{{ selectedConversation.counterpartName }}</h2>
                <p>
                  {{ selectedConversation.goodsTitle }}
                  <span>·</span>
                  {{ orderStatus[selectedConversation.orderStatus]?.label || selectedConversation.orderStatus }}
                </p>
              </div>
            </div>
            <RouterLink
              class="button button-ghost button-small"
              :to="{ name: 'orders' }"
            >
              查看订单
            </RouterLink>
          </header>

          <div ref="messageScroll" class="message-scroll">
            <div v-if="loadingThread" class="message-loading">
              <div class="message-loading-bubble shimmer"></div>
              <div class="message-loading-bubble short shimmer"></div>
            </div>
            <div v-else-if="messages.length === 0" class="message-empty">
              <span class="message-empty-mark">✦</span>
              <h3>从一句问候开始</h3>
              <p>可以先确认商品状态、交易时间和见面地点。</p>
            </div>
            <template v-else>
              <div v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.mine }">
                <div class="message-meta">
                  <strong>{{ message.mine ? '我' : message.senderName }}</strong>
                  <time>{{ dateTime(message.createdAt) }}</time>
                </div>
                <div class="message-bubble">{{ message.content }}</div>
              </div>
            </template>
            <p v-if="threadError" class="inline-error message-inline-error">{{ threadError }}</p>
          </div>

          <footer class="message-composer">
            <textarea
              v-model="draft"
              maxlength="500"
              rows="2"
              placeholder="输入消息，例如：明天中午 12 点主楼门口方便吗？"
              @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <div class="message-composer-foot">
              <small>手机号会自动打码 · {{ draft.length }} / 500</small>
              <button
                class="button button-dark button-small"
                type="button"
                :disabled="sending || !draft.trim()"
                @click="sendMessage"
              >
                {{ sending ? '发送中…' : '发送消息' }}
              </button>
            </div>
          </footer>
        </template>
        <div v-else class="message-empty thread-empty">
          <span class="message-empty-mark">↗</span>
          <h3>选择一个交易会话</h3>
          <p>左侧会显示你参与过的订单对话。</p>
        </div>
      </section>

      <section v-else class="thread-panel system-panel">
        <header class="thread-header">
          <div>
            <span class="section-index">NOTIFICATIONS</span>
            <h2>系统通知</h2>
            <p>认证、审核和平台动态会显示在这里。</p>
          </div>
        </header>
        <div class="notification-list">
          <div v-if="loadingThread" class="conversation-loading">
            <div v-for="index in 3" :key="index" class="conversation-skeleton shimmer"></div>
          </div>
          <div v-else-if="notifications.length === 0" class="message-empty">
            <span class="message-empty-mark">○</span>
            <h3>暂时没有新通知</h3>
            <p>新的平台消息会出现在这里。</p>
          </div>
          <article v-for="notification in notifications" :key="notification.id" class="notification-card">
            <span class="notification-symbol">i</span>
            <div>
              <strong>{{ notification.content }}</strong>
              <time>{{ dateTime(notification.createdAt) }}</time>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>
