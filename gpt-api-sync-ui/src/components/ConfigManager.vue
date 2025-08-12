<template>
  <div class="config-manager">
    <h2>配置管理</h2>
    <div class="config-actions">
      <button @click="fetchConfig">获取配置</button>
      <button @click="reloadConfig">重载配置</button>
    </div>
    <div v-if="config" class="config-display">
      <h3>当前配置</h3>
      <textarea v-model="editableConfig" rows="20"></textarea>
      <button @click="updateConfig">更新配置</button>
    </div>
    <p v-if="message">{{ message }}</p>
    <p v-if="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getConfig, reloadConfig as reloadConfigApi, updateConfig as updateConfigApi } from '../lib/api';

const config = ref<any>(null);
const editableConfig = ref('');
const message = ref<string | null>(null);
const error = ref<string | null>(null);

const fetchConfig = async () => {
  try {
    message.value = null;
    error.value = null;
    const data = await getConfig();
    config.value = data;
    editableConfig.value = JSON.stringify(data, null, 2);
  } catch (e) {
    error.value = '获取配置失败';
    console.error(e);
  }
};

const reloadConfig = async () => {
  try {
    message.value = null;
    error.value = null;
    await reloadConfigApi();
    message.value = '配置重载成功';
    fetchConfig(); // 重载后重新获取配置
  } catch (e) {
    error.value = '重载配置失败';
    console.error(e);
  }
};

const updateConfig = async () => {
  try {
    message.value = null;
    error.value = null;
    await updateConfigApi(JSON.parse(editableConfig.value));
    message.value = '配置更新成功';
    fetchConfig(); // 更新后重新获取配置
  } catch (e) {
    error.value = '更新配置失败';
    console.error(e);
  }
};
</script>

<style scoped>
.config-manager {
  border: 1px solid #ccc;
  padding: 1rem;
  margin-bottom: 1rem;
}

.config-actions {
  margin-bottom: 1rem;
}

.config-actions button {
  margin-right: 1rem;
}

.config-display textarea {
  width: 100%;
  box-sizing: border-box;
  font-family: monospace;
}
</style>