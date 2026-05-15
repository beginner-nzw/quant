import axios from 'axios'
import type { ApiResult } from '../types/task'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from './auth'
import { buildRequestHeaders } from './requestHeaders'

const request = axios.create({
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const user = getCurrentUser()
    config.headers = config.headers || {}
    Object.assign(config.headers, buildRequestHeaders(user))
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && result.success === false) {
      if (result.code === 'RATE_LIMITED' || result.code === 'HOT_TARGET_LIMITED') {
        ElMessage.warning(result.message || '请求过于频繁，请稍后重试')
      }
    }
    return result
  },
  (error) => {
    ElMessage.error(error?.message || '请求异常')
    return Promise.reject(error)
  }
)

export async function get<T>(url: string, params?: Record<string, any>) {
  return request.get<any, ApiResult<T>>(url, { params })
}

export async function post<T>(url: string, data?: Record<string, any>) {
  return request.post<any, ApiResult<T>>(url, data)
}

export default request
