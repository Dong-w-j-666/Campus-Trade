export class ApiError extends Error {
  constructor(message, status, fields = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  let body = options.body

  if (body && !(body instanceof FormData) && typeof body !== 'string') {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(body)
  }

  const response = await fetch(`/api${path}`, {
    credentials: 'include',
    ...options,
    headers,
    body
  })

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  let payload = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = text
    }
  }

  if (!response.ok) {
    throw new ApiError(
      payload?.message || '请求失败，请稍后重试',
      response.status,
      payload?.fields || {}
    )
  }
  return payload
}

export const api = {
  get(path) {
    return request(path)
  },
  post(path, body) {
    return request(path, { method: 'POST', body })
  },
  upload(path, file) {
    const form = new FormData()
    form.append('file', file)
    return request(path, { method: 'POST', body: form })
  }
}
