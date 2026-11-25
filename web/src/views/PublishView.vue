<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { money } from '../format'
import { session } from '../session'

const router = useRouter()
const categories = ref([])
const uploading = ref(false)
const submitting = ref(false)
const message = ref('')
const fieldErrors = ref({})
const localImages = ref([])

const form = reactive({
  title: '',
  description: '',
  price: '',
  conditionLevel: '九成新',
  categoryId: '',
  campus: session.user?.campus || '主校区'
})

const conditions = ['全新', '九五新', '九成新', '八成新', '七成新', '有使用痕迹']

async function onFilesSelected(event) {
  const selected = [...event.target.files]
  event.target.value = ''
  const remaining = 6 - localImages.value.length
  const files = selected.slice(0, remaining)

  for (const file of files) {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      message.value = '仅支持 JPG、PNG 或 WebP 图片'
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      message.value = '单张图片不能超过 5MB'
      continue
    }
    localImages.value.push({
      file,
      preview: URL.createObjectURL(file),
      url: '',
      state: 'waiting'
    })
  }
}

function removeImage(index) {
  const [removed] = localImages.value.splice(index, 1)
  if (removed?.preview) URL.revokeObjectURL(removed.preview)
}

async function uploadPendingImages() {
  const pending = localImages.value.filter((image) => !image.url)
  if (!pending.length) return
  uploading.value = true
  for (const image of pending) {
    image.state = 'uploading'
    try {
      const result = await api.upload('/uploads/images', image.file)
      image.url = result.url
      image.state = 'done'
    } catch (error) {
      image.state = 'failed'
      throw error
    }
  }
}

async function submit() {
  submitting.value = true
  message.value = ''
  fieldErrors.value = {}
  try {
    await uploadPendingImages()
    const result = await api.post('/goods', {
      title: form.title,
      description: form.description,
      price: Number(form.price),
      conditionLevel: form.conditionLevel,
      categoryId: Number(form.categoryId),
      campus: form.campus,
      imageUrls: localImages.value.map((image) => image.url).filter(Boolean)
    })
    await router.push(`/goods/${result.id}`)
  } catch (error) {
    message.value = error.message
    fieldErrors.value = error.fields || {}
    if (error.status === 401) {
      router.push({ name: 'login', query: { redirect: '/publish' } })
    }
  } finally {
    uploading.value = false
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    categories.value = await api.get('/categories')
    form.categoryId = categories.value[0]?.id || ''
  } catch (error) {
    message.value = error.message
  }
})

onBeforeUnmount(() => {
  localImages.value.forEach((image) => URL.revokeObjectURL(image.preview))
})
</script>

<template>
  <div class="publish-page page-container">
    <header class="page-heading publish-heading">
      <div>
        <span class="section-index">PASS IT ON</span>
        <h1>发布一件闲置</h1>
        <p>真实、清楚、有温度地介绍它，更容易遇见合适的新主人。</p>
      </div>
      <div class="step-indicator">
        <span class="active">01 填写</span>
        <i></i>
        <span>02 预览</span>
        <i></i>
        <span>03 发布</span>
      </div>
    </header>

    <div v-if="message" class="form-message wide" role="alert">{{ message }}</div>

    <form class="publish-layout" @submit.prevent="submit">
      <div class="publish-form">
        <section class="form-section">
          <div class="form-section-title">
            <span>01</span>
            <div>
              <h2>商品照片</h2>
              <p>最多 6 张，第一张将作为封面</p>
            </div>
          </div>

          <div class="image-uploader">
            <label v-if="localImages.length < 6" class="upload-tile">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                @change="onFilesSelected"
              />
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 16V4m0 0L7 9m5-5 5 5M5 14v5h14v-5" />
              </svg>
              <strong>添加图片</strong>
              <span>JPG / PNG / WebP · 5MB</span>
            </label>
            <div v-for="(image, index) in localImages" :key="image.preview" class="uploaded-tile">
              <img :src="image.preview" :alt="`待发布图片 ${index + 1}`" />
              <span v-if="index === 0" class="cover-label">封面</span>
              <span v-if="image.state === 'uploading'" class="upload-state">上传中</span>
              <span v-if="image.state === 'failed'" class="upload-state failed">失败</span>
              <button type="button" aria-label="移除图片" @click="removeImage(index)">×</button>
            </div>
          </div>
        </section>

        <section class="form-section">
          <div class="form-section-title">
            <span>02</span>
            <div>
              <h2>基本信息</h2>
              <p>像和同学聊天一样，把情况说清楚</p>
            </div>
          </div>

          <label class="field">
            <span>商品标题</span>
            <input
              v-model.trim="form.title"
              maxlength="60"
              placeholder="例如：高等数学第七版 上册，九成新"
            />
            <div class="field-foot">
              <small v-if="fieldErrors.title">{{ fieldErrors.title }}</small>
              <em>{{ form.title.length }} / 60</em>
            </div>
          </label>

          <label class="field">
            <span>详细描述</span>
            <textarea
              v-model="form.description"
              maxlength="5000"
              rows="6"
              placeholder="使用情况、瑕疵、配件、期望交易方式等，都可以写在这里。"
            ></textarea>
            <div class="field-foot">
              <small v-if="fieldErrors.description">{{ fieldErrors.description }}</small>
              <em>{{ form.description.length }} / 5000</em>
            </div>
          </label>
        </section>

        <section class="form-section">
          <div class="form-section-title">
            <span>03</span>
            <div>
              <h2>价格与交易</h2>
              <p>合理的定价，会让它更快遇见新主人</p>
            </div>
          </div>

          <div class="form-row two-columns">
            <label class="field price-field">
              <span>价格</span>
              <div class="price-input">
                <b>¥</b>
                <input v-model="form.price" type="number" min="0.01" step="0.01" placeholder="0.00" />
              </div>
              <small v-if="fieldErrors.price">{{ fieldErrors.price }}</small>
            </label>
            <label class="field">
              <span>商品成色</span>
              <select v-model="form.conditionLevel">
                <option v-for="condition in conditions" :key="condition">{{ condition }}</option>
              </select>
            </label>
          </div>

          <div class="form-row two-columns">
            <label class="field">
              <span>商品分类</span>
              <select v-model="form.categoryId">
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
              <small v-if="fieldErrors.categoryId">{{ fieldErrors.categoryId }}</small>
            </label>
            <label class="field">
              <span>交易校区</span>
              <select v-model="form.campus">
                <option>主校区</option>
                <option>东校区</option>
                <option>西校区</option>
              </select>
            </label>
          </div>
        </section>

        <button class="button button-dark button-wide publish-submit" type="submit" :disabled="submitting">
          {{ submitting ? (uploading ? '正在上传图片…' : '正在发布…') : '确认发布' }}
        </button>
      </div>

      <aside class="live-preview">
        <div class="preview-label">
          <span>LIVE PREVIEW</span>
          <i></i>
          发布效果
        </div>
        <div class="preview-card">
          <div class="preview-image">
            <img v-if="localImages[0]" :src="localImages[0].preview" alt="商品封面预览" />
            <div v-else class="preview-placeholder">
              <span>YOUR ITEM</span>
              <b>等待一张好照片</b>
            </div>
            <span class="condition-badge">{{ form.conditionLevel }}</span>
          </div>
          <div class="preview-body">
            <span class="preview-category">
              {{ categories.find((category) => category.id === Number(form.categoryId))?.name || '未选择分类' }}
            </span>
            <h3>{{ form.title || '你的商品标题会显示在这里' }}</h3>
            <div class="goods-bottom">
              <strong>{{ form.price ? money(form.price) : '¥ 0' }}</strong>
              <span class="campus-dot">{{ form.campus }}</span>
            </div>
            <div class="goods-seller-line">
              <span class="mini-avatar">{{ session.user?.realName.slice(-1) }}</span>
              <span>{{ session.user?.realName }}</span>
            </div>
          </div>
        </div>
        <div class="publish-tips">
          <strong>发布小提示</strong>
          <ul>
            <li>封面尽量使用自然光下的清晰实拍图</li>
            <li>如实说明划痕、缺件等使用痕迹</li>
            <li>避免在公开描述中填写楼栋门牌号</li>
          </ul>
        </div>
      </aside>
    </form>
  </div>
</template>
