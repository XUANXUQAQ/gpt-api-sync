<template>
  <WelcomeGuide v-if="isFirstTime" @configured="handleConfigured" />
  <div v-else class="container mx-auto p-4 md:p-8">
    <header class="mb-8 flex justify-between items-center">
      <div class="text-center flex-grow">
        <h1 class="text-4xl font-bold tracking-tight">GPT-API 同步服务</h1>
        <p class="text-muted-foreground mt-2">一个用于同步 gpt-load 和 new-api 配置的仪表板</p>
      </div>
      <Button variant="outline" size="icon" @click="showSettings = true">
        <Settings class="h-5 w-5" />
      </Button>
    </header>
    <main class="space-y-8 max-w-4xl mx-auto">
      <div class="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
        <HealthCard class="lg:col-span-1" />
        <SyncAction class="lg:col-span-2" @sync-completed="handleSyncCompleted" />
      </div>
      <ApiInfoCards ref="apiInfoCardsRef" />
    </main>
    <SettingsDialog v-model:open="showSettings" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { isFirstStartup } from '@/lib/api';
import HealthCard from '@/components/HealthCard.vue';
import SyncAction from '@/components/SyncAction.vue';
import ApiInfoCards from '@/components/ApiInfoCards.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import WelcomeGuide from '@/components/WelcomeGuide.vue';
import { Button } from '@/components/ui/button';
import { Settings } from 'lucide-vue-next';

const showSettings = ref(false);
const isFirstTime = ref<boolean | null>(null);
const apiInfoCardsRef = ref<{ fetchApiData: () => void } | null>(null);

const checkFirstStartup = async () => {
  const hasConfigured = localStorage.getItem('hasConfigured');
  if (hasConfigured === 'true') {
    isFirstTime.value = false;
    return;
  }

  try {
    const first = await isFirstStartup();
    isFirstTime.value = first === 'true';
  } catch (error) {
    console.error('Failed to check first startup:', error);
    // In case of error, assume it's not the first time to avoid blocking the app
    isFirstTime.value = false;
  }
};

const handleConfigured = () => {
  isFirstTime.value = false;
  // Optionally, you can reload the page to ensure all components get the new config
  window.location.reload();
};

const handleSyncCompleted = () => {
  if (apiInfoCardsRef.value) {
    apiInfoCardsRef.value.fetchApiData();
  }
};

onMounted(checkFirstStartup);
</script>