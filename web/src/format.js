export function money(value) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(Number(value || 0))
}

export function compactDate(value) {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.max(1, Math.floor(diff / 60000))
    return `${minutes} 分钟前`
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 3600000)} 小时前`
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric'
  }).format(date)
}

export function dateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export const orderStatus = {
  UNPAID: { label: '待付款', tone: 'amber' },
  PAID: { label: '待确认', tone: 'green' },
  FINISHED: { label: '已完成', tone: 'ink' },
  CANCELLED: { label: '已取消', tone: 'muted' }
}

export const goodsStatus = {
  ON_SALE: '在售',
  LOCKED: '已预订',
  SOLD: '已售出',
  OFF_SHELF: '已下架',
  PENDING: '待审核'
}
