<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import GoodsCard from '../components/GoodsCard.vue'
import { goodsStatus } from '../format'

const goods = ref([])
const loading = ref(true)
const error = ref('')
const action = ref('all')
const busyId = ref(null)

const filters = [
  { id: 'all', label: '全部' },
  { id: 'ON_SALE', label: '在售' },
  { id: 'LOCKED', label: '已预订' },
  { id: 'SOLD', label: '已售出' },
  { id: 'OFF_SHELF', label: '已下架' }
]

const visibleGoods = computed(() => {
  if (action.value === 'all') return goods.value
  return goods.value.filter((item) => item.status === action.value)
})

const stats = computed(() => ({
  onSale: goods.value.filter((item) => item.status === 'ON_SALE').length,
  sold: goods.value.filter((item) => item.status === 'SOLD').length,
  views: goods.value.reduce((sum, item) => sum + item.viewCount, 0)
}))

async function loadGoods() {
  loading.value = true
  error.value = ''
  try {
    goods.value = await api.get('/goods/mine')
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

async function takeOffShelf(item) {
  if (!window.confirm(`确认下架“${item.title}”吗？`)) return
  busyId.value = item.id
  try {
    await api.post(`/goods/${item.id}/off-shelf`)
    item.status = 'OFF_SHELF'
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    busyId.value = null
  }
}

onMounted(loadGoods)
</script>

<template>
  <div class="my-goods-page page-container">
    <header class="page-heading">
      <div>
        <span class="section-index">YOUR SHELF</span>
        <h1>我的发布</h1>
        <p>管理正在流转的闲置，也回顾它们遇见的新主人。</p>
      </div>
      <RouterLink class="button button-dark" to="/publish">
        <span class="button-plus">+</span>
        发布新闲置
      </RouterLink>
    </header>

    <div class="seller-stats">
      <div>
        <span>正在出售</span>
        <strong>{{ stats.onSale }}</strong>
        <small>件好物等待新主人</small>
      </div>
      <div>
        <span>已经售出</span>
        <strong>{{ stats.sold }}</strong>
        <small>次校园循环已完成</small>
      </div>
      <div>
        <span>累计浏览</span>
        <strong>{{ stats.views }}</strong>
        <small>次来自同学的关注</small>
      </div>
    </div>

    <div class="my-goods-toolbar">
      <div class="order-filters">
        <button
          v-for="filter in filters"
          :key="filter.id"
          type="button"
          :class="{ active: action === filter.id }"
          @click="action = filter.id"
        >
          {{ filter.label }}
        </button>
      </div>
      <span>{{ visibleGoods.length }} 件</span>
    </div>

    <div v-if="error" class="form-message wide" role="alert">{{ error }}</div>

    <div v-if="loading" class="goods-grid">
      <div v-for="index in 4" :key="index" class="goods-skeleton">
        <div class="skeleton-visual shimmer"></div>
        <div class="skeleton-line wide shimmer"></div>
        <div class="skeleton-line shimmer"></div>
      </div>
    </div>

    <div v-else-if="visibleGoods.length === 0" class="state-panel">
      <span class="state-symbol">+</span>
      <h3>这里暂时还是空的</h3>
      <p>书架上、抽屉里，也许正有一件闲置可以开始新的旅程。</p>
      <RouterLink class="button button-dark" to="/publish">发布第一件闲置</RouterLink>
    </div>

    <div v-else class="my-goods-grid">
      <article v-for="item in visibleGoods" :key="item.id" class="my-goods-item">
        <div class="my-goods-card">
          <GoodsCard :goods="item" />
          <span class="my-status-tag">{{ goodsStatus[item.status] || item.status }}</span>
        </div>
        <button
          v-if="item.status === 'ON_SALE'"
          class="text-button off-shelf-button"
          type="button"
          :disabled="busyId === item.id"
          @click="takeOffShelf(item)"
        >
          {{ busyId === item.id ? '正在下架…' : '下架商品' }}
        </button>
      </article>
    </div>
  </div>
</template>
