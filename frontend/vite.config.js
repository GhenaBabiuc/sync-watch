import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            '/api/storage': {
                target: 'http://localhost:8081',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api\/storage/, '/api')
            },

            '/api/sync': {
                target: 'http://localhost:8082',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api\/sync/, '/api')
            },

            '/ws': {
                target: 'ws://localhost:8082',
                ws: true,
                changeOrigin: true
            }
        }
    }
})
