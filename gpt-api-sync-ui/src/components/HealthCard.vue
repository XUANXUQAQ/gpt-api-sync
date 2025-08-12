<template>
  <Card class="w-full max-w-md">
    <CardHeader>
      <CardTitle>服务健康状态</CardTitle>
      <CardDescription>检查核心服务的连通性</CardDescription>
    </CardHeader>
    <CardContent>
      <div v-if="healthData" class="space-y-2">
        <p><strong>GPT-Load:</strong> <span :class="healthData.gptLoad === 'ok' ? 'text-green-500' : 'text-red-500'">{{ healthData.gptLoad }}</span></p>
        <p><strong>状态:</strong> <span :class="healthData.status === 'healthy' ? 'text-green-500' : 'text-red-500'">{{ healthData.status }}</span></p>
        <p><strong>时间戳:</strong> {{ new Date(healthData.timestamp).toLocaleString() }}</p>
      </div>
      <p v-if="error" class="text-red-500">{{ error }}</p>
    </CardContent>
  </Card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getHealth } from '@/lib/api';

const healthData = ref<any>(null);
const error = ref<string | null>(null);

const fetchHealth = async () => {
  try {
    healthData.value = await getHealth();
  } catch (e) {
    error.value = '获取健康信息失败';
    console.error(e);
  }
};

onMounted(fetchHealth);
</script>