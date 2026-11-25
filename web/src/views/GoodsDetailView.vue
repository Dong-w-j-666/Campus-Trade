<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { dateTime, goodsStatus, money } from '../format'
import { session } from '../session'

const route = useRoute()
const router = useRouter()
const goods = ref(null)
const loading = ref(true)
const error = ref('')
const activeImage = ref(0)
const brokenImages = ref(new Set())
const buying = ref(false)
const buyError = ref('')
const remark = ref('')
const reputation = ref(null)

const isOwner = computed(
  () => session.user && goods.value && session.user.id === goods.value.seller.id
)
const canBuy = computed(
  () => goods.value?.status === 'ON_SALE' && session.user && !isOwner.value
)

const currentImage = computed(() => {
  const images = goods.value?.images || []
  return images[activeImage.value] || ''
})

async function loadGoods() {
  loading.value = true
  error.value = ''
  reputation.value = null
  try {
    goods.value = await api.get(`/goods/${route.params.id}`)
    activeImage.value = 0
    try {
      reputation.value = await api.get(`/evaluations/users/${goods.value.seller.id}`)
    } catch {
      reputation.value = null
    }
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

function scoreStars(score) {
  const rounded = Math.max(0, Math.min(5, Math.round(Number(score || 0))))
  return `${'★'.repeat(rounded)}${'☆'.repeat(5 - rounded)}`
}

function markBroken(url) {
  brokenImages.value = new Set([...brokenImages.value, url])
}

async function createOrder() {
  if (!session.user) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  buying.value = true
  buyError.value = ''
  try {
    const order = await api.post('/orders', {
      goodsId: goods.value.id,
      remark: remark.value
    })
    router.push({ name: 'orders', query: { created: order.id, role: 'buyer' } })
  } catch (requestError) {
    buyError.value = requestError.message
  } finally {
    buying.value = false
  }
}

onMounted(loadGoods)
watch(() => route.params.id, loadGoods)
</script>

<template>
  <div class="detail-page page-container">
    <div v-if="loading" class="detail-loading">
      <div class="detail-gallery-skeleton shimmer"></div>
      <div class="detail-copy-skeleton">
        <div class="skeleton-line short shimmer"></div>
        <div class="skeleton-line title shimmer"></div>
        <div class="skeleton-line shimmer"></div>
        <div class="skeleton-line shimmer"></div>
      </div>
    </div>

    <div v-else-if="error" class="state-panel error-panel page-state">
      <span class="state-symbol">!</span>
      <h3>没能打开这件商品</h3>
      <p>{{ error }}</p>
      <button class="button button-dark" type="button" @click="loadGoods">重新加载</button>
    </div>

    <template v-else-if="goods">
      <nav class="breadcrumbs">
        <RouterLink to="/">逛市场</RouterLink>
        <span>/</span>
        <span>{{ goods.categoryName }}</span>
        <span>/</span>
        <b>{{ goods.title }}</b>
      </nav>

      <section class="detail-layout">
        <div class="detail-gallery">
          <div class="main-photo" :class="{ unavailable: !currentImage || brokenImages.has(currentImage) }">
            <img
              v-if="currentImage && !brokenImages.has(currentImage)"
              :src="currentImage"
              :alt="goods.title"
              @error="markBroken(currentImage)"
            />
            <div v-else class="detail-placeholder">
              <span>{{ goods.categoryName }}</span>
              <i></i>
              <b>暂无实拍图</b>
            </div>
            <span class="photo-count" v-if="goods.images.length">
              {{ activeImage + 1 }} / {{ goods.images.length }}
            </span>
          </div>

          <div v-if="goods.images.length > 1" class="thumbnail-row">
            <button
              v-for="(image, index) in goods.images"
              :key="image"
              type="button"
              :class="{ active: activeImage === index }"
              @click="activeImage = index"
            >
              <img
                v-if="!brokenImages.has(image)"
                :src="image"
                :alt="`商品图 ${index + 1}`"
                @error="markBroken(image)"
              />
              <span v-else>图 {{ index + 1 }}</span>
            </button>
          </div>
        </div>

        <aside class="detail-info">
          <div class="detail-status-row">
            <span class="condition-badge inline">{{ goods.conditionLevel }}</span>
            <span class="detail-status" :class="`status-${goods.status.toLowerCase()}`">
              {{ goodsStatus[goods.status] || goods.status }}
            </span>
            <span>{{ goods.viewCount }} 次浏览</span>
          </div>

          <h1>{{ goods.title }}</h1>
          <div class="detail-price">{{ money(goods.price) }}</div>

          <div class="detail-facts">
            <div>
              <span>交易地点</span>
              <strong>{{ goods.campus }}</strong>
            </div>
            <div>
              <span>发布时间</span>
              <strong>{{ dateTime(goods.createdAt) }}</strong>
            </div>
            <div>
              <span>商品分类</span>
              <strong>{{ goods.categoryName }}</strong>
            </div>
          </div>

          <div class="seller-card">
            <span class="seller-avatar">{{ goods.seller.name.slice(-1) }}</span>
            <div class="seller-copy">
              <span>发布同学</span>
              <strong>{{ goods.seller.name }}</strong>
              <small>
                {{ goods.seller.campus }} · 信用分 {{ goods.seller.creditScore }}
                <template v-if="reputation"> · {{ reputation.creditLevel }}</template>
              </small>
            </div>
            <span class="verified-badge">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m6 12 4 4 8-8" />
              </svg>
              校园身份
            </span>
          </div>

          <div class="buy-box">
            <label v-if="canBuy" class="field compact-field">
              <span>给卖家留言（可选）</span>
              <input v-model="remark" maxlength="255" placeholder="例如：今晚图书馆门口可以吗？" />
            </label>
            <p v-if="buyError" class="inline-error">{{ buyError }}</p>
            <button
              v-if="!session.user"
              class="button button-dark button-wide button-tall"
              type="button"
              @click="router.push({ name: 'login', query: { redirect: route.fullPath } })"
            >
              登录后购买
            </button>
            <button
              v-else-if="isOwner"
              class="button button-muted button-wide button-tall"
              type="button"
              disabled
            >
              这是你发布的商品
            </button>
            <button
              v-else
              class="button button-dark button-wide button-tall"
              type="button"
              :disabled="!canBuy || buying"
              @click="createOrder"
            >
              {{ buying ? '正在锁定商品…' : canBuy ? '提交订单并锁定' : goodsStatus[goods.status] }}
            </button>
            <p class="buy-note">
              提交后商品将为你保留。本版本使用沙箱支付，不产生真实扣款。
            </p>
          </div>
        </aside>
      </section>

      <section class="description-section">
        <div class="description-heading">
          <span class="section-index">ABOUT THIS ITEM</span>
          <h2>商品说明</h2>
        </div>
        <div class="description-copy">
          <p v-if="goods.description">{{ goods.description }}</p>
          <p v-else class="muted-copy">卖家暂未填写详细说明，可以在提交订单时留言询问。</p>
        </div>
      </section>

      <section v-if="reputation" class="reputation-section">
        <header class="reputation-heading">
          <div>
            <span class="section-index">SELLER REPUTATION</span>
            <h2>卖家口碑</h2>
          </div>
          <div class="reputation-score">
            <strong>{{ reputation.averageScore || '—' }}</strong>
            <span>{{ scoreStars(reputation.averageScore) }}</span>
            <small>{{ reputation.evaluationCount }} 条交易评价</small>
          </div>
        </header>

        <div v-if="reputation.recentEvaluations.length" class="review-list">
          <article
            v-for="review in reputation.recentEvaluations.slice(0, 4)"
            :key="review.id"
            class="review-card"
          >
            <div class="review-meta">
              <strong>{{ review.evaluatorName }}</strong>
              <span>{{ scoreStars(review.score) }}</span>
              <time>{{ dateTime(review.createdAt) }}</time>
            </div>
            <p v-if="review.content">{{ review.content }}</p>
            <div v-if="review.tags.length" class="review-tags">
              <span v-for="tag in review.tags" :key="tag">{{ tag }}</span>
            </div>
          </article>
        </div>
        <p v-else class="reputation-empty">还没有交易评价，先完成一单再来看看吧。</p>
      </section>
    </template>
  </div>
</template>
