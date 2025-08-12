<template>
  <div class="container mx-auto p-4 md:p-8">
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
        <SyncAction class="lg:col-span-2" />
      </div>
      <ApiInfoCards />
    </main>
    <SettingsDialog v-model:open="showSettings" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import HealthCard from '@/components/HealthCard.vue';
import SyncAction from '@/components/SyncAction.vue';
import ApiInfoCards from '@/components/ApiInfoCards.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import { Button } from '@/components/ui/button';
import { Settings } from 'lucide-vue-next';

const showSettings = ref(false);
</script>