import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    proxy: {
      '/api/research': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/tasks': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')

          if (!normalizedId.includes('node_modules')) {
            return
          }

          if (normalizedId.includes('element-plus') || normalizedId.includes('@element-plus')) {
            return 'element-plus'
          }

          if (normalizedId.includes('vue-router')) {
            return 'vue-router'
          }

          if (normalizedId.includes('/vue/') || normalizedId.includes('@vue')) {
            return 'vue'
          }

          if (normalizedId.includes('axios')) {
            return 'axios'
          }

          return 'vendor'
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
