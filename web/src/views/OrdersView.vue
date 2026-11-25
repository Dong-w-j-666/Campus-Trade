<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { dateTime, money, orderStatus } from '../format'
import { session } from '../session'

const route = useRoute()
const router = useRouter()
const role = ref(route.query.role === 'seller' ? 'seller' : 'buyer')
const action = ref('all')
const orders = ref([])
const loading = ref(true)
const error = ref('')
const busyId = ref(null)

const filters = [
  { id: 'all', label: '全部' },
  { id: 'UNPAID', label: '待付款' },
  { id: 'PAID', label: '待确认' },
  { id: 'FINISHED', label: '已完成' },
  { id: 'CANCELLED', label: '已取消' }
]

const filteredOrders = computed(() => {
  if (action.value === 'all') return orders.value
  return orders.value.filter((order) => order.status === action.value)
})

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    orders.value = await api.get(`/orders?role=${role.value}`)
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

function switchRole(nextRole) {
  role.value = nextRole
  router.replace({ name: 'orders', query: { role: nextRole } })
}

async function perform(order, actionName) {
  const prompts = {
    pay: '确认使用沙箱支付这笔订单吗？',
    cancel: '确认取消这个订单吗？商品会重新上架。',
    refund: '确认申请全额退款吗？商品会重新上架。',
    complete: '确认已经拿到商品并完成交易吗？'
  }
  if (!window.confirm(prompts[actionName])) return

  busyId.value = order.id
  error.value = ''
  try {
    const updated = await api.post(`/orders/${order.id}/${actionName}`)
    const index = orders.value.findIndex((item) => item.id === order.id)
    if (index >= 0) orders.value[index] = updated
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    busyId.value = null
  }
}

onMounted(loadOrders)
watch(role, loadOrders)
</script>

<template>
  <div class="orders-page page-container">
    <header class="page-heading">
      <div>
        <span class="section-index">YOUR TRADES</span>
        <h1>我的订单</h1>
        <p>每一次交接，都让物品和故事继续流动。</p>
      </div>
      <RouterLink class="button button-outline" to="/">继续逛市场</RouterLink>
    </header>

    <div v-if="route.query.created" class="success-banner">
      <span>✓</span>
      <div>
        <strong>订单已创建，商品已为你锁定</strong>
        <p>请在下方完成沙箱支付，再与卖家约定见面时间。</p>
      </div>
    </div>

    <div class="orders-switcher">
      <button type="button" :class="{ active: role === 'buyer' }" @click="switchRole('buyer')">
        我买到的
      </button>
      <button type="button" :class="{ active: role === 'seller' }" @click="switchRole('seller')">
        我卖出的
      </button>
    </div>

    <div class="order-filters">
      <button
        v-for="filter in filters"
        :key="filter.id"
        type="button"
        :class="{ active: action === filter.id }"
        @click="action = filter.id"
      >
        {{ filter.label }}
        <span v-if="filter.id === 'all'">{{ orders.length }}</span>
      </button>
    </div>

    <div v-if="error" class="form-message wide" role="alert">{{ error }}</div>

    <div v-if="loading" class="order-list">
      <div v-for="index in 3" :key="index" class="order-skeleton">
        <div class="order-image shimmer"></div>
        <div>
          <div class="skeleton-line short shimmer"></div>
          <div class="skeleton-line wide shimmer"></div>
          <div class="skeleton-line shimmer"></div>
        </div>
      </div>
    </div>

    <div v-else-if="filteredOrders.length === 0" class="state-panel orders-empty">
      <span class="state-symbol">0</span>
      <h3>{{ role === 'buyer' ? '还没有这类订单' : '还没有卖出记录' }}</h3>
      <p>{{ role === 'buyer' ? '市场里正有好物等你发现。' : '发布一件闲置，等待新的故事发生。' }}</p>
      <RouterLink
        class="button button-dark"
        :to="role === 'buyer' ? '/' : '/publish'"
      >
        {{ role === 'buyer' ? '去逛市场' : '发布闲置' }}
      </RouterLink>
    </div>

    <div v-else class="order-list">
      <article v-for="order in filteredOrders" :key="order.id" class="order-card">
        <header class="order-header">
          <div>
            <span>订单号 {{ order.orderNo }}</span>
            <span>{{ dateTime(order.createdAt) }}</span>
          </div>
          <span
            class="order-status"
            :class="`tone-${orderStatus[order.status]?.tone || 'muted'}`"
          >
            {{ orderStatus[order.status]?.label || order.status }}
          </span>
        </header>

        <div class="order-main">
          <RouterLink :to="`/goods/${order.goodsId}`" class="order-image">
            <img v-if="order.goodsImage" :src="order.goodsImage" :alt="order.goodsTitle" />
            <span v-else>好物</span>
          </RouterLink>
          <div class="order-product-copy">
            <RouterLink :to="`/goods/${order.goodsId}`">
              <h2>{{ order.goodsTitle }}</h2>
            </RouterLink>
            <p>
              {{ role === 'buyer' ? `卖家：${order.sellerName}` : `买家：${order.buyerName}` }}
              · {{ order.tradeCampus }}
            </p>
            <span v-if="order.remark" class="order-remark">“{{ order.remark }}”</span>
          </div>
          <div class="order-price">
            <span>订单金额</span>
            <strong>{{ money(order.amount) }}</strong>
          </div>
        </div>

        <footer class="order-footer">
          <div class="order-progress">
            <span :class="{ done: true }">创建订单</span>
            <i :class="{ done: ['PAID', 'FINISHED'].includes(order.status) }"></i>
            <span :class="{ done: ['PAID', 'FINISHED'].includes(order.status) }">完成付款</span>
            <i :class="{ done: order.status === 'FINISHED' }"></i>
            <span :class="{ done: order.status === 'FINISHED' }">交易完成</span>
          </div>

          <div v-if="role === 'buyer'" class="order-actions">
            <template v-if="order.status === 'UNPAID'">
              <button
                class="button button-ghost button-small"
                type="button"
                :disabled="busyId === order.id"
                @click="perform(order, 'cancel')"
              >
                取消订单
              </button>
              <button
                class="button button-dark button-small"
                type="button"
                :disabled="busyId === order.id"
                @click="perform(order, 'pay')"
              >
                {{ busyId === order.id ? '处理中…' : '沙箱支付' }}
              </button>
            </template>
            <template v-else-if="order.status === 'PAID'">
              <button
                class="button button-ghost button-small"
                type="button"
                :disabled="busyId === order.id"
                @click="perform(order, 'refund')"
              >
                申请退款
              </button>
              <button
                class="button button-dark button-small"
                type="button"
                :disabled="busyId === order.id"
                @click="perform(order, 'complete')"
              >
                {{ busyId === order.id ? '处理中…' : '确认收货' }}
              </button>
            </template>
            <span v-else-if="order.status === 'FINISHED'" class="settled-note">
              已结算给卖家 {{ money(order.amount * 0.98) }}
            </span>
          </div>
          <div v-else class="order-actions">
            <span v-if="order.status === 'UNPAID'" class="settled-note">等待买家付款</span>
            <span v-else-if="order.status === 'PAID'" class="settled-note">等待与买家当面交接</span>
            <span v-else-if="order.status === 'FINISHED'" class="settled-note">
              已结算 {{ money(order.amount * 0.98) }}
            </span>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>
