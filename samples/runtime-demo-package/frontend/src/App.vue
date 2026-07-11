<template>
  <main class="app-shell">
    <h1>Runtime Demo Frontend</h1>
    <p>{{ message }}</p>
    <button @click="loadHello">Ping Backend</button>
    <pre>{{ backendMessage }}</pre>
  </main>
</template>

<script setup>
import { ref } from 'vue'

const message = 'This demo package is meant for the Jingxuan runtime flow.'
const backendMessage = ref('Backend response will appear here.')

async function loadHello() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const response = await fetch(`${baseUrl}/api/hello`)
  const data = await response.json()
  backendMessage.value = JSON.stringify(data, null, 2)
}
</script>

<style scoped>
.app-shell {
  max-width: 720px;
  margin: 40px auto;
  padding: 24px;
  font-family: Arial, sans-serif;
}

button {
  margin-top: 12px;
  padding: 10px 14px;
}

pre {
  margin-top: 16px;
  padding: 16px;
  background: #f3f4f6;
  border-radius: 8px;
}
</style>
