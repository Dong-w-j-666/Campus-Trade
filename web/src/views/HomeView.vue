<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import GoodsCard from '../components/GoodsCard.vue'

const categories = ref([])
const goods = ref([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const totalPages = ref(0)

const filters = reactive({
  keyword: '',
  categoryId: null,
  campus: '',
  sort: 'latest',
  page: 0
})

const appliedKeyword = ref('')

const visibleCategories = computed(() => [
  { id: null, name: '全部好物' },
  ...categories.value.filter((category) => category.parentId !== null || !categories.value.some((child) => child.parentId === category.id))
])

async function loadGoods() {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({
    page: String(filters.page),
    size: '12',
    sort: filters.sort
  })
  if (appliedKeyword.value) params.set('keyword', appliedKeyword.value)
  if (filters.categoryId) params.set('categoryId', String(filters.categoryId))
  if (filters.campus) params.set('campus', filters.campus)

  try {
    const result = await api.get(`/goods?${params}`)
    goods.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

function search() {
  appliedKeyword.value = filters.keyword.trim()
  filters.page = 0
  loadGoods()
}

function chooseCategory(id) {
  filters.categoryId = id
  filters.page = 0
  loadGoods()
}

function changeFilter() {
  filters.page = 0
  loadGoods()
}

function goPage(page) {
  filters.page = page
  loadGoods()
  document.querySelector('.market-section')?.scrollIntoView({ behavior: 'smooth' })
}

function resetFilters() {
  filters.keyword = ''
  appliedKeyword.value = ''
  filters.categoryId = null
  filters.campus = ''
  filters.sort = 'latest'
  filters.page = 0
  loadGoods()
}

onMounted(async () => {
  try {
    categories.value = await api.get('/categories')
  } catch (requestError) {
    error.value = requestError.message
  }
  loadGoods()
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-grid">
        <div class="hero-copy">
          <div class="eyebrow">
            <span></span>
            只属于校园的循环生活
          </div>
          <h1>
            旧物不旧，
            <em>故事继续。</em>
          </h1>
          <p class="hero-intro">
            把闲置交给同校同学，也在这里遇见下一本教材、下一台相机和下一段校园故事。
          </p>

          <form class="hero-search" @submit.prevent="search">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-4-4" />
            </svg>
            <input
              v-model="filters.keyword"
              type="search"
              placeholder="搜课本、耳机、自行车……"
              aria-label="搜索商品"
            />
            <button type="submit">找一找</button>
          </form>

          <div class="hero-facts">
            <div>
              <strong>{{ total || '20+' }}</strong>
              <span>校园好物</span>
            </div>
            <div>
              <strong>同校</strong>
              <span>面对面交易</span>
            </div>
            <div>
              <strong>0</strong>
              <span>成交服务费</span>
            </div>
          </div>
        </div>

        <div class="hero-art" aria-hidden="true">
          <div class="hero-orbit orbit-one"></div>
          <div class="hero-orbit orbit-two"></div>
          <div class="hero-sticker">在校园里<br />重新流动</div>
          <div class="featured-stack">
            <div class="stack-card stack-back">
              <span>BUY · SELL · REPEAT</span>
            </div>
            <div class="stack-card stack-front">
              <div class="stack-visual">
                <span class="sun"></span>
                <span class="hill hill-one"></span>
                <span class="hill hill-two"></span>
              </div>
              <div class="stack-caption">
                <span>今日推荐</span>
                <strong>让喜欢的东西<br />遇见需要它的人</strong>
              </div>
            </div>
            <div class="float-note">
              <span class="note-icon">↗</span>
              <div>
                <strong>刚刚成交</strong>
                <small>高数教材 · 主校区</small>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="hero-ticker" aria-hidden="true">
        <span>CAMPUS ONLY</span>
        <i></i>
        <span>MEET IN PERSON</span>
        <i></i>
        <span>GIVE IT A SECOND LIFE</span>
        <i></i>
        <span>CAMPUS ONLY</span>
      </div>
    </section>

    <section class="market-section">
      <div class="section-heading">
        <div>
          <span class="section-index">01 / MARKET</span>
          <h2>正在流转的好物</h2>
        </div>
        <p>看看今天校园里有什么新发现</p>
      </div>

      <div class="category-strip" aria-label="商品分类">
        <button
          v-for="category in visibleCategories"
          :key="category.id ?? 'all'"
          type="button"
          :class="{ active: filters.categoryId === category.id }"
          @click="chooseCategory(category.id)"
        >
          {{ category.name }}
        </button>
      </div>

      <div class="market-toolbar">
        <span class="result-count">
          <b>{{ total }}</b> 件好物
          <template v-if="appliedKeyword">· “{{ appliedKeyword }}”</template>
        </span>
        <div class="toolbar-controls">
          <label>
            <span>校区</span>
            <select v-model="filters.campus" @change="changeFilter">
              <option value="">全部校区</option>
              <option value="主校区">主校区</option>
              <option value="东校区">东校区</option>
              <option value="西校区">西校区</option>
            </select>
          </label>
          <label>
            <span>排序</span>
            <select v-model="filters.sort" @change="changeFilter">
              <option value="latest">最新发布</option>
              <option value="popular">最多浏览</option>
              <option value="price-asc">价格从低到高</option>
              <option value="price-desc">价格从高到低</option>
            </select>
          </label>
        </div>
      </div>

      <div v-if="loading" class="goods-grid">
        <div v-for="index in 8" :key="index" class="goods-skeleton">
          <div class="skeleton-visual shimmer"></div>
          <div class="skeleton-line wide shimmer"></div>
          <div class="skeleton-line shimmer"></div>
        </div>
      </div>

      <div v-else-if="error" class="state-panel error-panel">
        <span class="state-symbol">!</span>
        <h3>市场暂时没有响应</h3>
        <p>{{ error }}</p>
        <button class="button button-dark" type="button" @click="loadGoods">重新加载</button>
      </div>

      <div v-else-if="goods.length === 0" class="state-panel">
        <span class="state-symbol">?</span>
        <h3>没有找到匹配的好物</h3>
        <p>换一个关键词或分类试试，也可以成为第一个发布的人。</p>
        <button class="button button-outline" type="button" @click="resetFilters">清除筛选</button>
      </div>

      <div v-else class="goods-grid">
        <GoodsCard v-for="goodsItem in goods" :key="goodsItem.id" :goods="goodsItem" />
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button
          type="button"
          :disabled="filters.page === 0"
          aria-label="上一页"
          @click="goPage(filters.page - 1)"
        >
          ←
        </button>
        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          :class="{ active: filters.page === page - 1 }"
          @click="goPage(page - 1)"
        >
          {{ page }}
        </button>
        <button
          type="button"
          :disabled="filters.page >= totalPages - 1"
          aria-label="下一页"
          @click="goPage(filters.page + 1)"
        >
          →
        </button>
      </div>
    </section>

    <section class="trust-section">
      <div class="trust-heading">
        <span>BUILT FOR CAMPUS</span>
        <h2>离得近，聊得来，也更放心。</h2>
      </div>
      <div class="trust-grid">
        <article>
          <span class="trust-number">01</span>
          <h3>同校身份</h3>
          <p>学号注册与校园信息展示，让每次交易都有真实身份作为基础。</p>
        </article>
        <article>
          <span class="trust-number">02</span>
          <h3>先锁单再见面</h3>
          <p>提交订单后自动锁定商品，减少一件物品被多人同时预订的尴尬。</p>
        </article>
        <article>
          <span class="trust-number">03</span>
          <h3>沙箱交易</h3>
          <p>支付、退款和结算全流程可演示，当前不接入真实资金，适合课程与原型验证。</p>
        </article>
      </div>
    </section>
  </div>
</template>
