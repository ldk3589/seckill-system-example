<template>
  <div class="wrap">
    <h1>高并发秒杀系统 Demo（Vue）</h1>

    <section class="card">
      <h2>1) 登录 / 注册</h2>
      <div class="row">
        <input v-model="form.username" placeholder="username" />
        <input v-model="form.password" placeholder="password" type="password" />
        <button @click="register">注册</button>
        <button @click="login">登录</button>
        <button @click="logout" class="danger">退出</button>
      </div>
      <div class="hint">当前 token：{{ token ? '已设置' : '无' }}</div>
    </section>

    <section class="card">
      <h2>2) 商品与库存</h2>
      <div class="row">
        <input v-model.number="productId" type="number" min="1" />
        <button @click="warmup">预热库存到 Redis</button>
        <button @click="loadProduct">查询商品</button>
      </div>

      <div v-if="product" class="box">
        <div><b>商品：</b>{{ product.name }}</div>
        <div><b>价格：</b>{{ product.price }}</div>
        <div><b>库存（DB缓存视图）：</b>{{ product.stock }}</div>
      </div>
    </section>

    <section class="card">
      <h2>3) 秒杀下单</h2>
      <div class="row">
        <button @click="seckill" class="primary">立即秒杀</button>
        <button @click="queryStatus">查询订单状态</button>
        <button @click="togglePolling">{{ polling ? '停止轮询' : '轮询状态' }}</button>
      </div>

      <div class="box">
        <div><b>orderNo：</b>{{ orderNo || '-' }}</div>
        <div><b>status：</b>{{ status || '-' }}</div>
      </div>
    </section>

    <section class="card">
      <h2>日志</h2>
      <pre class="log">{{ logs.join('\n') }}</pre>
    </section>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import api from './api'

const form = ref({ username: 'u1', password: '123456' })
const token = ref(localStorage.getItem('token') || '')
const productId = ref(1)
const product = ref(null)

const orderNo = ref('')
const status = ref('')
const logs = ref([])
const polling = ref(false)
let timer = null

function log(msg) {
  const t = new Date().toLocaleTimeString()
  logs.value.unshift(`[${t}] ${msg}`)
}

function setToken(t) {
  token.value = t || ''
  if (t) localStorage.setItem('token', t)
  else localStorage.removeItem('token')
}

async function register() {
  try {
    const res = await api.post('/api/auth/register', form.value)
    log(`注册：${JSON.stringify(res.data)}`)
  } catch (e) {
    log(`注册失败：${errMsg(e)}`)
  }
}

async function login() {
  try {
    const res = await api.post('/api/auth/login', form.value)
    const t = res.data?.data?.token
    setToken(t)
    log(`登录成功，token 已保存`)
  } catch (e) {
    log(`登录失败：${errMsg(e)}`)
  }
}

function logout() {
  setToken('')
  log('已退出')
}

async function warmup() {
  try {
    const res = await api.post(`/api/product/${productId.value}/warmup`)
    log(`预热：${JSON.stringify(res.data)}`)
  } catch (e) {
    log(`预热失败：${errMsg(e)}`)
  }
}

async function loadProduct() {
  try {
    const res = await api.get(`/api/product/${productId.value}`)
    product.value = res.data?.data
    log(`商品：${JSON.stringify(res.data)}`)
  } catch (e) {
    log(`查询商品失败：${errMsg(e)}`)
  }
}

async function seckill() {
  try {
    const res = await api.post('/api/seckill/do', { productId: productId.value })
    orderNo.value = res.data?.data?.orderNo || ''
    status.value = ''
    log(`秒杀：${JSON.stringify(res.data)}`)
  } catch (e) {
    const data = e?.response?.data
    if (data?.code === 409) {
      log('你已经秒杀过该商品：请查询订单状态，或换账号再抢')
      // 如果你希望自动进入“查状态”，可以取消注释：
      // await queryStatus()
      return
    }
    log(`秒杀失败：${data ? JSON.stringify(data) : (e?.message || String(e))}`)
  }
}

async function queryStatus() {
  if (!orderNo.value) {
    log('请先秒杀拿到 orderNo')
    return
  }
  try {
    const res = await api.get(`/api/order/status/${orderNo.value}`)
    status.value = res.data?.data?.status || ''
    log(`状态：${JSON.stringify(res.data)}`)
  } catch (e) {
    log(`查状态失败：${errMsg(e)}`)
  }
}

function togglePolling() {
  polling.value = !polling.value
  if (polling.value) {
    log('开始轮询订单状态（每 500ms）')
    timer = setInterval(queryStatus, 500)
  } else {
    log('停止轮询')
    clearInterval(timer)
    timer = null
  }
}

function errMsg(e) {
  const data = e?.response?.data
  return data ? JSON.stringify(data) : (e?.message || String(e))
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.wrap { max-width: 980px; margin: 24px auto; font-family: ui-sans-serif, system-ui; padding: 0 16px; }
.card { border: 1px solid #ddd; border-radius: 12px; padding: 16px; margin: 12px 0; }
.row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
input { padding: 8px 10px; border: 1px solid #ccc; border-radius: 8px; min-width: 180px; }
button { padding: 8px 12px; border: 1px solid #ccc; border-radius: 8px; background: #fff; cursor: pointer; }
button.primary { border-color: #222; font-weight: 700; }
button.danger { border-color: #a00; color: #a00; }
.box { margin-top: 10px; padding: 10px; background: #fafafa; border-radius: 10px; border: 1px dashed #ddd; }
.hint { margin-top: 8px; color: #666; font-size: 12px; }
.log { background: #0b1020; color: #d7e3ff; padding: 12px; border-radius: 10px; overflow: auto; max-height: 260px; }
</style>