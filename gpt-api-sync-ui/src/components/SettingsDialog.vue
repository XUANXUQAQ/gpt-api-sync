<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)">
    <DialogContent class="sm:max-w-[800px] h-[80vh] flex flex-col">
      <DialogHeader>
        <DialogTitle>应用配置</DialogTitle>
        <DialogDescription>
          在这里查看和修改应用的后端配置。修改后请保存。
        </DialogDescription>
      </DialogHeader>

      <div v-if="loading" class="flex-grow flex items-center justify-center">
        <p>加载中...</p>
      </div>
      <div v-if="error" class="p-4 bg-red-100 border border-red-400 text-red-700 rounded">
        <h4 class="font-bold">加载配置失败</h4>
        <p>{{ error }}</p>
      </div>

      <Tabs v-if="localConfig" class="flex-grow flex flex-col" v-model="activeTab">
        <TabsList class="grid w-full grid-cols-4">
          <TabsTrigger value="general">通用</TabsTrigger>
          <TabsTrigger value="gpt-load">GPT-Load</TabsTrigger>
          <TabsTrigger value="new-api">New API</TabsTrigger>
          <TabsTrigger value="models">模型重定向</TabsTrigger>
        </TabsList>

        <div class="flex-grow overflow-y-auto p-1">
          <TabsContent value="general" class="space-y-4 p-2">
            <div class="space-y-2">
              <Label for="server-port">服务端口</Label>
              <Input id="server-port" type="number" v-model.number="localConfig.server.port" />
            </div>
            <div class="space-y-2">
              <Label for="log-level">日志级别</Label>
              <Select v-model="localConfig.log.level">
                <SelectTrigger>
                  <SelectValue placeholder="选择日志级别" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INFO">INFO</SelectItem>
                  <SelectItem value="DEBUG">DEBUG</SelectItem>
                  <SelectItem value="WARN">WARN</SelectItem>
                  <SelectItem value="ERROR">ERROR</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label for="sync-conn-timeout">连接超时 (ms)</Label>
              <Input id="sync-conn-timeout" type="number" v-model.number="localConfig.sync.connectionTimeout" />
            </div>
            <div class="space-y-2">
              <Label for="sync-read-timeout">读取超时 (ms)</Label>
              <Input id="sync-read-timeout" type="number" v-model.number="localConfig.sync.readTimeout" />
            </div>
          </TabsContent>

          <TabsContent value="gpt-load" class="space-y-4 p-2">
            <div class="space-y-2">
              <Label for="gpt-baseurl">Base URL</Label>
              <Input id="gpt-baseurl" v-model="localConfig.gptLoad.baseUrl" />
            </div>
            <div class="space-y-2">
              <Label for="gpt-authkey">Auth Key</Label>
              <Input id="gpt-authkey" type="password" v-model="localConfig.gptLoad.authKey" />
            </div>
          </TabsContent>

          <TabsContent value="new-api" class="space-y-4 p-2">
            <div class="space-y-2">
              <Label for="newapi-baseurl">Base URL</Label>
              <Input id="newapi-baseurl" v-model="localConfig.newApi.baseUrl" />
            </div>
            <div class="space-y-2">
              <Label for="newapi-token">Access Token</Label>
              <Input id="newapi-token" type="password" v-model="localConfig.newApi.accessToken" />
            </div>
            <div class="space-y-2">
              <Label for="newapi-userid">User ID</Label>
              <Input id="newapi-userid" v-model="localConfig.newApi.userId" />
            </div>
          </TabsContent>

          <TabsContent value="models" class="space-y-4 p-2">
            <div class="space-y-2">
              <Label>模型重定向说明</Label>
              <p class="text-sm text-muted-foreground">
                当从 GPT-Load 获取的模型标准模型名列表中不存在时，系统将采用模糊匹配算法，自动寻找并重定向到最相似的模型名称。您可以在下方手动管理标准模型列表。
                <br />
                例如：有的API返回可能模型名为gemini-pro-2.5，如果您在标准模型名中添加gemini-2.5-pro，则在进行同步时将会在New-API中自动添加一条 "gemini-2.5-pro":
                "gemini-pro-2.5"的规则
              </p>
            </div>
            <Label>标准模型列表</Label>
            <div class="flex flex-wrap gap-2 p-2 border rounded-md">
              <Badge v-for="(model, index) in localConfig.modelRedirect.standardModels" :key="index"
                variant="secondary">
                {{ model }}
                <button @click="removeModel(index)" class="ml-2 text-destructive hover:text-red-400">&times;</button>
              </Badge>
              <p v-if="!localConfig.modelRedirect.standardModels.length" class="text-sm text-muted-foreground">暂无模型</p>
            </div>
            <div class="flex gap-2">
              <Input v-model="newModel" placeholder="添加新模型..." @keyup.enter="addModel" />
              <Button @click="addModel">添加</Button>
            </div>
          </TabsContent>
        </div>
      </Tabs>

      <DialogFooter class="flex-shrink-0">
        <div class="flex justify-between w-full">
          <Button type="button" variant="destructive" @click="handleReload" :disabled="isActionLoading">
            {{ isActionLoading && actionType === 'reload' ? '重新加载中...' : '重新加载配置' }}
          </Button>
          <div class="flex gap-2">
            <Button type="button" variant="secondary" @click="emit('update:open', false)">
              关闭
            </Button>
            <Button type="button" @click="handleSave" :disabled="isActionLoading">
              {{ isActionLoading && actionType === 'save' ? '保存中...' : '保存更改' }}
            </Button>
          </div>
        </div>
      </DialogFooter>
      <div v-if="actionMessage" class="text-sm text-green-600 mt-2 text-center">{{ actionMessage }}</div>
      <div v-if="actionError" class="text-sm text-red-600 mt-2 text-center">{{ actionError }}</div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import { getConfig, updateConfig, reloadConfig } from '@/lib/api';
import { cloneDeep } from 'lodash-es';

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
}>();

const localConfig = ref<any>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const activeTab = ref('general');

const isActionLoading = ref(false);
const actionType = ref<'save' | 'reload' | null>(null);
const actionMessage = ref<string | null>(null);
const actionError = ref<string | null>(null);

const newModel = ref('');

const fetchConfig = async () => {
  if (!props.open) return;
  loading.value = true;
  error.value = null;
  localConfig.value = null;
  try {
    const config = await getConfig();
    localConfig.value = cloneDeep(config);
  } catch (e) {
    error.value = '获取配置信息失败。';
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  if (!localConfig.value) return;
  isActionLoading.value = true;
  actionType.value = 'save';
  actionMessage.value = null;
  actionError.value = null;
  try {
    await updateConfig(localConfig.value);
    actionMessage.value = '配置已成功保存！';
  } catch (e) {
    actionError.value = '保存配置失败。';
    console.error(e);
  } finally {
    isActionLoading.value = false;
    actionType.value = null;
    setTimeout(() => {
      actionMessage.value = null;
      actionError.value = null;
    }, 3000);
  }
};

const handleReload = async () => {
  isActionLoading.value = true;
  actionType.value = 'reload';
  actionMessage.value = null;
  actionError.value = null;
  try {
    await reloadConfig();
    await fetchConfig(); // Refetch config after reload
    actionMessage.value = '配置已成功从磁盘重新加载！';
  } catch (e) {
    actionError.value = '重新加载配置失败。';
    console.error(e);
  } finally {
    isActionLoading.value = false;
    actionType.value = null;
    setTimeout(() => {
      actionMessage.value = null;
      actionError.value = null;
    }, 3000);
  }
};

const addModel = () => {
  if (newModel.value && !localConfig.value.modelRedirect.standardModels.includes(newModel.value)) {
    localConfig.value.modelRedirect.standardModels.push(newModel.value);
    newModel.value = '';
  }
};

const removeModel = (index: number) => {
  localConfig.value.modelRedirect.standardModels.splice(index, 1);
};

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    fetchConfig();
    // Reset tab to general on open
    activeTab.value = 'general';
  }
}, { immediate: true });
</script>