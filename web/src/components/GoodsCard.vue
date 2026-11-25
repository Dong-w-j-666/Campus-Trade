<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { compactDate, money } from '../format'

defineProps({
  goods: {
    type: Object,
    required: true
  }
})

const imageBroken = ref(false)
</script>

<template>
  <RouterLink class="goods-card" :to="`/goods/${goods.id}`">
    <div class="goods-visual">
      <img
        v-if="goods.imageUrl && !imageBroken"
        :src="goods.imageUrl"
        :alt="goods.title"
        loading="lazy"
        @error="imageBroken = true"
      />
      <div v-else class="goods-placeholder">
        <span>{{ goods.categoryName?.slice(0, 2) || '闲置' }}</span>
        <i></i>
        <b></b>
      </div>
      <span class="condition-badge">{{ goods.conditionLevel }}</span>
    </div>

    <div class="goods-card-body">
      <div class="goods-meta-row">
        <span>{{ goods.categoryName }}</span>
        <span>{{ compactDate(goods.createdAt) }}</span>
      </div>
      <h3>{{ goods.title }}</h3>
      <div class="goods-bottom">
        <strong>{{ money(goods.price) }}</strong>
        <span class="campus-dot">{{ goods.campus }}</span>
      </div>
      <div class="goods-seller-line">
        <span class="mini-avatar">{{ goods.sellerName?.slice(-1) }}</span>
        <span>{{ goods.sellerName }}</span>
        <span class="view-count">{{ goods.viewCount }} 次浏览</span>
      </div>
    </div>
  </RouterLink>
</template>
