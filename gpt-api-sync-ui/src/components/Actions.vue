<template>
  <div class="actions">
    <h2>操作</h2>
    <button @click="syncChannels">触发同步</button>
    <p v-if="message">{{ message }}</p>
    <p v-if="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { syncChannels as syncChannelsApi } from '../lib/api';

const message = ref<string | null>(null);
const error = ref<string | null>(null);

const syncChannels = async () => {
  try {
    message.value = null;
    error.value = null;
    const responseData = await syncChannelsApi();
    message.value = `同步成功: ${JSON.stringify(responseData)}`;
  } catch (e) {
    error.value = '同步失败';
    console.error(e);
  }
};
</script>

<style scoped>
.actions {
  border: 1px solid #ccc;
  padding: 1rem;
  margin-bottom: 1rem;
}
</style>