<template>
  <div class="api-info">
    <h2>API 信息</h2>
    <div class="api-section">
      <h3>GPT-Load 信息</h3>
      <pre v-if="gptLoadInfo">{{ gptLoadInfo }}</pre>
      <p v-if="gptLoadError">{{ gptLoadError }}</p>
    </div>
    <div class="api-section">
      <h3>New-API 信息</h3>
      <pre v-if="newApiInfo">{{ newApiInfo }}</pre>
      <p v-if="newApiError">{{ newApiError }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getGptLoadInfo, getNewApiInfo } from '../lib/api';

const gptLoadInfo = ref<any>(null);
const newApiInfo = ref<any>(null);
const gptLoadError = ref<string | null>(null);
const newApiError = ref<string | null>(null);

const fetchApiData = async () => {
  try {
    gptLoadInfo.value = await getGptLoadInfo();
  } catch (e) {
    gptLoadError.value = '获取 GPT-Load 信息失败';
    console.error(e);
  }
  try {
    newApiInfo.value = await getNewApiInfo();
  } catch (e) {
    newApiError.value = '获取 New-API 信息失败';
    console.error(e);
  }
};

onMounted(fetchApiData);
</script>

<style scoped>
.api-info {
  border: 1px solid #ccc;
  padding: 1rem;
  margin-bottom: 1rem;
}
.api-section {
  margin-bottom: 1rem;
}
pre {
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>