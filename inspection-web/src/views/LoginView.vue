<template>
  <div class="login">
    <div class="card" :class="{ shake: hasError }">
      <div class="title">无线电监督检查系统</div>
      <div class="subtitle">安全合规 · 数据可信 · 智能监管</div>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            autocomplete="username"
            class="input"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            autocomplete="current-password"
            class="input"
            :class="{ 'input-error': hasError }"
          />
        </el-form-item>
        <div class="options">
          <el-checkbox v-model="remember">记住密码</el-checkbox>
          <span v-if="errorMsg" class="error-msg">{{ errorMsg }}</span>
        </div>
        <el-button type="primary" class="btn" @click="handleLogin" :loading="loading">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const remember = ref(false)
const hasError = ref(false)
const errorMsg = ref('')
const form = reactive({
  username: '',
  password: ''
})

onMounted(() => {
  const savedUsername = localStorage.getItem('rememberedUsername')
  const savedPassword = localStorage.getItem('rememberedPassword')
  if (savedUsername) {
    form.username = savedUsername
  }
  if (savedPassword) {
    form.password = savedPassword
  }
  if (savedUsername || savedPassword) {
    remember.value = true
  }
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  errorMsg.value = ''
  hasError.value = false
  try {
    const res = await login(form)
    if (res.success) {
      auth.setAuth(res.data.token, {
        username: res.data.username,
        realName: res.data.realName,
        role: res.data.role
      })
      if (remember.value) {
        localStorage.setItem('rememberedUsername', form.username)
        localStorage.setItem('rememberedPassword', form.password)
      } else {
        localStorage.removeItem('rememberedUsername')
        localStorage.removeItem('rememberedPassword')
      }
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      errorMsg.value = '密码错误，请重试'
      hasError.value = true
    }
  } catch (e) {
    errorMsg.value = '密码错误，请重试'
    hasError.value = true
  } finally {
    loading.value = false
    if (hasError.value) {
      setTimeout(() => {
        hasError.value = false
      }, 600)
    }
  }
}
</script>

<style scoped>
.login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top, rgba(59, 130, 246, 0.35), transparent 55%),
    linear-gradient(135deg, #0b1220, #0f172a 45%, #111827);
}

.card {
  width: 380px;
  background: rgba(15, 23, 42, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.25);
  padding: 30px;
  border-radius: 18px;
  box-shadow: 0 24px 50px rgba(2, 6, 23, 0.6);
  backdrop-filter: blur(12px);
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  text-align: center;
  color: #e2e8f0;
}

.subtitle {
  font-size: 12px;
  text-align: center;
  color: #94a3b8;
  margin-bottom: 20px;
}

.input :deep(.el-input__wrapper) {
  background: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input :deep(.el-input__wrapper.is-focus) {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
}

.input :deep(.el-input__inner) {
  color: #e2e8f0;
}

.input-error :deep(.el-input__wrapper) {
  border-color: #f87171;
}

.options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  color: #cbd5f5;
  font-size: 12px;
}

.btn {
  width: 100%;
  height: 42px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.35);
}

.error-msg {
  color: #fca5a5;
}

.shake {
  animation: shake 0.35s ease-in-out 0s 1;
}

@keyframes shake {
  0% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-6px);
  }
  50% {
    transform: translateX(6px);
  }
  75% {
    transform: translateX(-4px);
  }
  100% {
    transform: translateX(0);
  }
}
</style>
