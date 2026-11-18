import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import cesium from 'vite-plugin-cesium'

export default defineConfig({
  plugins: [vue(), cesium()],
  // cesium 1.121 的 Source/Cesium.js 与自带 @cesium/engine 存在导出不匹配，
  // 预打包必失败 → 改用其自带单文件构建（Build/Cesium/index.js），排除预打包
  optimizeDeps: {
    exclude: ['cesium']
  },
  resolve: {
    alias: [
      {
        // cesium 的 exports 禁用了 ./Build/*.js 深路径，且 Source/Cesium.js 存在导出缺陷，
        // 用正则别名把裸导入 'cesium' 精确指到它自带的单文件构建（自包含、离线可用）
        find: /^cesium$/,
        replacement: fileURLToPath(new URL('./node_modules/cesium/Build/Cesium/index.js', import.meta.url))
      },
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) }
    ]
  },
  server: {
    // 监听所有网卡:localhost / 127.0.0.1 / 局域网 IP 都能访问
    host: '0.0.0.0',
    // 端口与后端代理由 config.bat 传入；默认值 5174 / 8091
    port: Number(process.env.VITE_PORT || 5174),
    proxy: {
      '/api': { target: process.env.VITE_API_TARGET || 'http://127.0.0.1:8091', changeOrigin: true }
    }
  }
})
