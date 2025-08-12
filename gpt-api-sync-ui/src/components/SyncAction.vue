<template>
  <Card class="w-full">
    <CardHeader>
      <CardTitle>手动同步</CardTitle>
      <CardDescription>
        触发一次从 gpt-load 到 new-api 的数据同步。
      </CardDescription>
    </CardHeader>
    <CardContent>
      <Button @click="showDialog = true" class="w-full">
        <Zap class="w-4 h-4 mr-2" />
        触发同步
      </Button>
    </CardContent>
    <CardFooter v-if="message || error" class="flex flex-col items-start gap-2">
       <div v-if="message" class="w-full p-4 bg-green-100 border border-green-400 text-green-700 rounded">
        <h4 class="font-bold">同步成功</h4>
        <pre class="whitespace-pre-wrap break-all">{{ message }}</pre>
      </div>
      <div v-if="error" class="w-full p-4 bg-red-100 border border-red-400 text-red-700 rounded">
        <h4 class="font-bold">同步失败</h4>
        <p>{{ error }}</p>
      </div>    </CardFooter>

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
      </AlertDialogContent>    </AlertDialog>
  </Card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Zap } from 'lucide-vue-next';
import { Button } from '@/components/ui/button';
import Card from '@/components/ui/card/Card.vue';
import CardContent from '@/components/ui/card/CardContent.vue';
import CardDescription from '@/components/ui/card/CardDescription.vue';
import CardFooter from '@/components/ui/card/CardFooter.vue';
import CardHeader from '@/components/ui/card/CardHeader.vue';
import CardTitle from '@/components/ui/card/CardTitle.vue';
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