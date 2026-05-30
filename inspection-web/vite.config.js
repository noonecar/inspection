import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,               // 端口配置
    host: '0.0.0.0',         // 允许外网访问（cpolar）
    allowedHosts: [               // 通配符匹配任意 cpolar 域名
      '.r36.cpolar.top',
      '.cpolar.top',
      '.cpolar.cn'
    ],
    proxy: {
      '/api': {               // 拦截所有以 /api 开头的请求
        target: 'http://localhost:8080',  // SpringBoot 后端地址
        changeOrigin: true,   // 解决跨域
      },
      '/uploads': {            // 代理上传文件访问
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})