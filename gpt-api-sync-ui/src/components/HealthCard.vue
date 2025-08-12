<template>
  <Card class="w-full max-w-md">
    <CardHeader>
      <CardTitle>服务健康状态</CardTitle>
      <CardDescription>检查核心服务的连通性</CardDescription>
    </CardHeader>
    <CardContent>
      <div v-if="serviceStatuses" class="space-y-2">
        <div v-for="(status, service) in serviceStatuses" :key="service">
          <p>
            <strong class="capitalize">{{ String(service).replace(/[-_]/g, ' ') }}:</strong>
            <span :class="status === 'ok' ? 'text-green-500' : 'text-red-500'">{{ status }}</span>
          </p>
        </div>
      </div>
      <p v-if="error" class="text-red-500">{{ error }}</p>
    </CardContent>
  </Card>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getServiceStatus } from '@/lib/api';

const statusData = ref<Record<string, any> | null>(null);
const error = ref<string | null>(null);

const serviceStatuses = computed(() => {
  if (!statusData.value || !statusData.value.connections) return null;
  // We only want to display the connection statuses, not other data like config.
  return statusData.value.connections;
});

const fetchStatus = async () => {
  try {
    statusData.value = await getServiceStatus();
  } catch (e) {
    error.value = '获取服务状态失败';
    console.error(e);
  }
};

onMounted(fetchStatus);
</script>