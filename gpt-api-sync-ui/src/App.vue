<template>
  <div class="container mx-auto p-4 md:p-8">
    <header class="mb-8 text-center">
      <h1 class="text-4xl font-bold tracking-tight">GPT-API 同步服务</h1>
      <p class="text-muted-foreground mt-2">一个用于同步 gpt-load 和 new-api 配置的仪表板</p>
    </header>
    <main class="space-y-8">
      <div class="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
        <HealthCard class="lg:col-span-1" />
        <div class="lg:col-span-2 flex items-center justify-center">
           <SyncAction />
        </div>
      </div>
      <ApiInfoCards />
    </main>
  </div>
</template>

<script setup lang="ts">
import HealthCard from '@/components/HealthCard.vue';
import SyncAction from '@/components/SyncAction.vue';
import ApiInfoCards from '@/components/ApiInfoCards.vue';
</script>