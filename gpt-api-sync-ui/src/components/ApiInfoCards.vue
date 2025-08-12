<template>
  <Accordion type="single" class="w-full space-y-4" collapsible>
    <AccordionItem value="gpt-load">
      <AccordionTrigger class="w-full">
        <div class="flex justify-between items-center w-full p-4">
          <div class="text-left">
            <CardTitle>GPT-Load 分组信息</CardTitle>
            <CardDescription>共 {{ gptLoadInfo?.length || 0 }} 个分组</CardDescription>
          </div>
        </div>
      </AccordionTrigger>
      <AccordionContent>
        <CardContent class="p-0">
          <Table v-if="gptLoadInfo && gptLoadInfo.length">
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>模型</TableHead>
                <TableHead>渠道类型</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="group in gptLoadInfo" :key="group.name">
                <TableCell>{{ group.name }}</TableCell>
                <TableCell>{{ (Array.isArray(group.models) && group.models.length > 0) ? group.models.join(', ') : group.test_model || 'N/A' }}</TableCell>
                <TableCell>{{ group.channel_type }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <p v-else-if="gptLoadError" class="text-red-500 p-4">{{ gptLoadError }}</p>
          <p v-else class="p-4">暂无数据</p>
        </CardContent>
      </AccordionContent>
    </AccordionItem>

    <AccordionItem value="new-api">
      <AccordionTrigger class="w-full">
        <div class="flex justify-between items-center w-full p-4">
          <div class="text-left">
            <CardTitle>New-API 渠道信息</CardTitle>
            <CardDescription>共 {{ newApiInfo?.length || 0 }} 个渠道</CardDescription>
          </div>
        </div>
      </AccordionTrigger>
      <AccordionContent>
        <CardContent class="p-0">
          <Table v-if="newApiInfo && newApiInfo.length">
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>分组</TableHead>
                <TableHead>模型</TableHead>
                <TableHead>状态</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="channel in newApiInfo" :key="channel.id">
                <TableCell>{{ channel.name }}</TableCell>
                <TableCell>{{ channel.group }}</TableCell>
                <TableCell class="flex flex-wrap gap-1 py-2">
                  <Badge v-for="model in channel.models.split(',')" :key="model" variant="outline">
                    {{ model }}
                  </Badge>
                </TableCell>
                <TableCell>
                  <span :class="channel.status === 1 ? 'text-green-500' : 'text-gray-500'">
                    {{ channel.status === 1 ? '已启用' : '已禁用' }}
                  </span>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <p v-else-if="newApiError" class="text-red-500 p-4">{{ newApiError }}</p>
          <p v-else class="p-4">暂无数据</p>
        </CardContent>
      </AccordionContent>
    </AccordionItem>
  </Accordion>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import CardContent from '@/components/ui/card/CardContent.vue';
import CardDescription from '@/components/ui/card/CardDescription.vue';
import CardHeader from '@/components/ui/card/CardHeader.vue';
import CardTitle from '@/components/ui/card/CardTitle.vue';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { getGptLoadInfo, getNewApiInfo } from '@/lib/api';

const gptLoadInfo = ref<any[] | null>(null);
const newApiInfo = ref<any[] | null>(null);
const gptLoadError = ref<string | null>(null);
const newApiError = ref<string | null>(null);

const fetchApiData = async () => {
  try {
    gptLoadInfo.value = await getGptLoadInfo();
  } catch (e) {
    gptLoadError.value = '获取 GPT-Load 信息失败';
    console.error(e);
  }
  try {
    newApiInfo.value = await getNewApiInfo();
  } catch (e) {
    newApiError.value = '获取 New-API 信息失败';
    console.error(e);
  }
};

onMounted(fetchApiData);
</script>