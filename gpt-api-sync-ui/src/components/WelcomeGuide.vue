<template>
    <div class="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center">
        <div class="w-full max-w-md p-8 space-y-6 bg-card text-card-foreground rounded-lg shadow-lg">
            <div class="text-center">
                <h1 class="text-3xl font-bold">欢迎使用</h1>
                <p class="text-muted-foreground mt-2">首次使用，请先配置 GPT-Load 和 New API 的信息。</p>
            </div>
            <div class="space-y-4">
                <div>
                    <h3 class="text-lg font-medium">GPT-Load</h3>
                    <div class="space-y-2 mt-2">
                        <Label for="gpt-baseurl">Base URL</Label>
                        <Input id="gpt-baseurl" v-model="config.gptLoad.baseUrl" placeholder="http://localhost:8000" />
                        <Label for="gpt-authkey">Auth Key</Label>
                        <Input id="gpt-authkey" type="password" v-model="config.gptLoad.authKey" />
                    </div>
                </div>
                <div>
                    <h3 class="text-lg font-medium">New API</h3>
                    <div class="space-y-2 mt-2">
                        <Label for="newapi-baseurl">Base URL</Label>
                        <Input id="newapi-baseurl" v-model="config.newApi.baseUrl"
                            placeholder="http://localhost:3000" />
                        <Label for="newapi-token">Access Token</Label>
                        <Input id="newapi-token" type="password" v-model="config.newApi.accessToken" />
                        <Label for="newapi-userid">User ID</Label>
                        <Input id="newapi-userid" v-model="config.newApi.userId" />
                    </div>
                </div>
            </div>
            <Button @click="handleSave" class="w-full" :disabled="isSaving">
                <LoaderCircle v-if="isSaving" class="w-4 h-4 mr-2 animate-spin" />
                保存并开始
            </Button>
            <p v-if="error" class="text-sm text-destructive text-center">{{ error }}</p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { LoaderCircle } from 'lucide-vue-next';
import { getConfig, updateConfig } from '@/lib/api';

const emit = defineEmits(['configured']);

const config = ref({
    gptLoad: {
        baseUrl: '',
        authKey: '',
    },
    newApi: {
        baseUrl: '',
        accessToken: '',
        userId: '',
    },
});
const isSaving = ref(false);
const error = ref<string | null>(null);

const handleSave = async () => {
    isSaving.value = true;
    error.value = null;
    try {
        // First, get the full config to not overwrite other settings
        const fullConfig = await getConfig();
        // Merge the settings from the welcome guide
        fullConfig.gptLoad = { ...fullConfig.gptLoad, ...config.value.gptLoad };
        fullConfig.newApi = { ...fullConfig.newApi, ...config.value.newApi };

        await updateConfig(fullConfig);
        emit('configured');
    } catch (e: any) {
        error.value = e.message || '保存失败，请检查网络连接或后端服务。';
    } finally {
        isSaving.value = false;
    }
};
</script>