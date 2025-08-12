<template>
  <div class="service-info">
    <h2>服务信息</h2>
    <div v-if="serviceInfo">
      <p><strong>服务名称:</strong> {{ serviceInfo.service }}</p>
      <p><strong>版本:</strong> {{ serviceInfo.version }}</p>
      <p><strong>状态:</strong> {{ serviceInfo.status }}</p>
    </div>
    <div v-if="serviceStatus">
      <p><strong>服务状态详情:</strong> {{ serviceStatus }}</p>
    </div>
    <p v-if="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getServiceInfo, getServiceStatus } from '../lib/api';

const serviceInfo = ref<any>(null);
const serviceStatus = ref<any>(null);
const error = ref<string | null>(null);

const fetchServiceData = async () => {
  try {
    serviceInfo.value = await getServiceInfo();
    serviceStatus.value = await getServiceStatus();
  } catch (e) {
    error.value = '获取服务数据失败';
    console.error(e);
  }
};

onMounted(fetchServiceData);
</script>

<style scoped>
.service-info {
  border: 1px solid #ccc;
  padding: 1rem;
  margin-bottom: 1rem;
}
</style>