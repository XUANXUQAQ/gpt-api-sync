<template>
  <div id="app">
    <header>
      <h1>GPT-API 同步服务</h1>
    </header>
    <main>
      <ServiceInfo />
      <ApiInfo />
      <ConfigManager />
      <Actions />
    </main>
  </div>
</template>

<script setup lang="ts">
import ServiceInfo from './components/ServiceInfo.vue';
import ApiInfo from './components/ApiInfo.vue';
import ConfigManager from './components/ConfigManager.vue';
import Actions from './components/Actions.vue';
</script>

<style scoped>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;}header {  margin-bottom: 2rem;}main {  max-width: 800px;  margin: 0 auto;  display: grid;
  gap: 1rem;
}
</style>