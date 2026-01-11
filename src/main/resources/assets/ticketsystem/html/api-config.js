/**
 * 统一的API配置管理器
 * 提供API地址的存储、加载和fetch函数重写功能
 */
const ApiConfigManager = {
    // 存储键名
    STORAGE_KEYS: {
        API_BASE: 'ticketsystem_api_base',
        API_SOURCE: 'ticketsystem_api_source'
    },

    // 默认API地址
    DEFAULT_API_BASE: 'http://127.0.0.1:23333/api',

    // 当前使用的API基础地址
    currentApiBase: null,
    currentSource: null,

    // 初始化：确定要使用的API地址
    async init() {
        try {
            // 1. 尝试从游戏配置获取
            const gameConfig = await this.fetchGameConfig();
            const gameApiBase = gameConfig?.game_api_base || '';

            // 2. 从浏览器本地存储获取
            const storedApiBase = localStorage.getItem(this.STORAGE_KEYS.API_BASE);
            const storedSource = localStorage.getItem(this.STORAGE_KEYS.API_SOURCE);

            // 3. 应用优先级：游戏配置 > 浏览器存储 > 默认地址
            if (gameApiBase && gameApiBase.trim() !== '') {
                this.currentApiBase = gameApiBase.trim();
                this.currentSource = 'game_config';
                console.log('使用游戏内配置的API地址:', this.currentApiBase);
            } else if (storedApiBase && storedApiBase.trim() !== '') {
                this.currentApiBase = storedApiBase.trim();
                this.currentSource = storedSource || 'browser_storage';
                console.log('使用浏览器存储的API地址:', this.currentApiBase);
            } else {
                this.currentApiBase = this.DEFAULT_API_BASE;
                this.currentSource = 'default';
                console.log('使用默认API地址:', this.currentApiBase);
            }

            // 更新UI显示
            this.updateUi();

            // 设置全局fetch函数使用当前API地址
            this.overrideFetch();

            return this.currentApiBase;
        } catch (error) {
            console.error('API配置初始化失败:', error);
            this.currentApiBase = this.DEFAULT_API_BASE;
            this.currentSource = 'default';
            this.updateUi();
            this.overrideFetch();
            return this.currentApiBase;
        }
    },

    // 从服务器获取游戏配置
    async fetchGameConfig() {
        try {
            // 使用默认地址获取配置
            const response = await fetch('/api/config');
            if (response.ok) {
                return await response.json();
            }
            return null;
        } catch (error) {
            console.warn('获取游戏配置失败:', error);
            return null;
        }
    },

    // 更新UI显示
    updateUi() {
        const apiBaseInput = document.getElementById('apiBase');
        const apiBaseInput2 = document.getElementById('apiBaseInput'); // console.html 中的输入框
        const currentApiUrl = document.getElementById('currentApiUrl');
        const apiStatus = document.getElementById('apiStatus');
        const apiSourceInfo = document.getElementById('apiSourceInfo');

        if (apiBaseInput) {
            apiBaseInput.value = this.currentApiBase;
        }
        if (apiBaseInput2) {
            apiBaseInput2.value = this.currentApiBase;
        }

        if (currentApiUrl) {
            currentApiUrl.textContent = this.currentApiBase;
        }

        if (apiStatus) {
            let statusText = '默认';
            let statusColor = 'var(--accent)';

            switch (this.currentSource) {
                case 'game_config':
                    statusText = '游戏配置';
                    statusColor = 'var(--success)';
                    break;
                case 'browser_storage':
                    statusText = '浏览器存储';
                    statusColor = 'var(--warning)';
                    break;
                case 'default':
                    statusText = '默认';
                    statusColor = 'var(--accent)';
                    break;
            }

            apiStatus.textContent = statusText;
            apiStatus.style.backgroundColor = statusColor;
        }

        if (apiSourceInfo) {
            let sourceText = '来源：';
            switch (this.currentSource) {
                case 'game_config':
                    sourceText += '游戏内配置';
                    break;
                case 'browser_storage':
                    sourceText += '浏览器本地存储';
                    break;
                case 'default':
                    sourceText += '默认地址';
                    break;
            }
            apiSourceInfo.textContent = sourceText;
        }
    },

    // 保存到浏览器本地存储
    saveToBrowser(apiBase) {
        const trimmed = apiBase.trim();
        if (!trimmed) {
            alert('API地址不能为空');
            return false;
        }

        try {
            // 简单验证URL格式
            new URL(trimmed);
        } catch (error) {
            alert('请输入有效的URL格式（如 http://127.0.0.1:23333/api）');
            return false;
        }

        localStorage.setItem(this.STORAGE_KEYS.API_BASE, trimmed);
        localStorage.setItem(this.STORAGE_KEYS.API_SOURCE, 'browser_storage');

        // 重新初始化
        this.init();

        alert('API地址已保存到浏览器本地存储');
        return true;
    },

    // 清除浏览器缓存，使用默认地址
    clearBrowserCache() {
        localStorage.removeItem(this.STORAGE_KEYS.API_BASE);
        localStorage.removeItem(this.STORAGE_KEYS.API_SOURCE);

        // 重新初始化
        this.init();

        alert('已清除浏览器缓存，使用默认API地址');
    },

    // 从游戏配置加载
    async loadFromGameConfig() {
        try {
            const gameConfig = await this.fetchGameConfig();
            const gameApiBase = gameConfig?.game_api_base || '';

            if (gameApiBase && gameApiBase.trim() !== '') {
                this.currentApiBase = gameApiBase.trim();
                this.currentSource = 'game_config';
                this.updateUi();
                alert('已加载游戏内配置的API地址');
            } else {
                alert('游戏内未配置API地址，请先在游戏配置中设置');
            }
        } catch (error) {
            console.error('加载游戏配置失败:', error);
            alert('加载游戏配置失败: ' + error.message);
        }
    },

    // 重写fetch函数，自动使用当前API地址
    overrideFetch() {
        const originalFetch = window.fetch;
        const apiBase = this.currentApiBase;

        window.fetch = async function(resource, options) {
            let url = resource;

            // 如果是相对路径，且不是以 /api 开头（静态资源），则添加API基础地址
            if (typeof resource === 'string' && resource.startsWith('/api')) {
                // 如果当前API地址不是默认地址，且请求的是API路径，则替换基础地址
                if (apiBase !== ApiConfigManager.DEFAULT_API_BASE) {
                    // 移除默认的基础部分，添加当前API基础地址
                    const apiPath = resource;
                    url = apiBase.replace(/\/api$/, '') + apiPath;
                }
            }

            return originalFetch.call(window, url, options);
        };
    }
};

// 自动初始化（如果DOM已加载）
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        ApiConfigManager.init();
    });
} else {
    ApiConfigManager.init();
}