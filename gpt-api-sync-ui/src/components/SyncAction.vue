<template>
  <div>
    <Button @click="showDialog = true">触发同步</Button>
    <AlertDialog :open="showDialog" @update:open="showDialog = $event">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>确认同步</AlertDialogTitle>
          <AlertDialogDescription>
            此操作将从 gpt-load 获取分组，并将其智能同步（创建或更新）为 new-api 中的渠道。您确定要继续吗？
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>取消</AlertDialogCancel>
          <AlertDialogAction @click="handleSync">确认</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
    <div v-if="message" class="mt-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded w-full max-w-md">
      <h4 class="font-bold">同步成功</h4>
      <pre>{{ message }}</pre>
    </div>
    <div v-if="error" class="mt-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded w-full max-w-md">
       <h4 class="font-bold">同步失败</h4>
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Button } from '@/components/ui/button';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { syncChannels } from '@/lib/api';

const showDialog = ref(false);
const message = ref<string | null>(null);
const error = ref<string | null>(null);

const handleSync = async () => {
  try {
    message.value = null;
    error.value = null;
    const responseData = await syncChannels();
    message.value = JSON.stringify(responseData, null, 2);
  } catch (e: any) {
    error.value = e.message || '同步失败';
    console.error(e);
  } finally {
    showDialog.value = false;
  }
};
</script>