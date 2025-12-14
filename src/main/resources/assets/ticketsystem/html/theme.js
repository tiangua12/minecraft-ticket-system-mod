/**
 * 主题切换公共模块
 * 提供深色/浅色模式切换功能
 */

class ThemeManager {
    constructor() {
        this.themeToggle = null;
        this.themeLabel = null;
        this.themeIcon = null;
        this.initialized = false;
    }

    /**
     * 初始化主题管理器
     * @param {string} toggleId - 主题切换按钮ID (默认: 'themeToggle')
     * @param {string} labelId - 主题标签ID (默认: 'themeLabel')
     */
    init(toggleId = 'themeToggle', labelId = 'themeLabel') {
        if (this.initialized) return;

        this.themeToggle = document.getElementById(toggleId);
        if (!this.themeToggle) {
            console.warn(`主题切换按钮未找到: #${toggleId}`);
            return;
        }

        this.themeLabel = document.getElementById(labelId);
        this.themeIcon = this.themeToggle.querySelector('.theme-icon');

        // 初始化主题状态
        this.applySavedTheme();

        // 绑定点击事件
        this.themeToggle.addEventListener('click', () => this.toggleTheme());

        this.initialized = true;
        console.log('主题管理器初始化完成');
    }

    /**
     * 应用保存的主题
     */
    applySavedTheme() {
        const savedTheme = localStorage.getItem('theme') || 'dark';
        const isLight = savedTheme === 'light';

        if (isLight) {
            document.body.classList.add('light');
            this.updateButtonText('浅色模式', '☀️');
        } else {
            document.body.classList.remove('light');
            this.updateButtonText('深色模式', '🌙');
        }
    }

    /**
     * 切换主题
     */
    toggleTheme() {
        const isLight = document.body.classList.contains('light');

        if (isLight) {
            document.body.classList.remove('light');
            localStorage.setItem('theme', 'dark');
            this.updateButtonText('深色模式', '🌙');
        } else {
            document.body.classList.add('light');
            localStorage.setItem('theme', 'light');
            this.updateButtonText('浅色模式', '☀️');
        }

        // 触发自定义事件，供其他组件监听
        document.dispatchEvent(new CustomEvent('themechange', {
            detail: { theme: isLight ? 'dark' : 'light' }
        }));
    }

    /**
     * 更新按钮文本和图标
     * @param {string} text - 按钮文本
     * @param {string} icon - 图标字符
     */
    updateButtonText(text, icon) {
        if (this.themeLabel) {
            this.themeLabel.textContent = text;
        }
        if (this.themeIcon) {
            this.themeIcon.textContent = icon;
        }
    }

    /**
     * 获取当前主题
     * @returns {string} 'light' 或 'dark'
     */
    getCurrentTheme() {
        return document.body.classList.contains('light') ? 'light' : 'dark';
    }

    /**
     * 设置主题
     * @param {string} theme - 'light' 或 'dark'
     */
    setTheme(theme) {
        if (theme === 'light') {
            document.body.classList.add('light');
            localStorage.setItem('theme', 'light');
            this.updateButtonText('浅色模式', '☀️');
        } else {
            document.body.classList.remove('light');
            localStorage.setItem('theme', 'dark');
            this.updateButtonText('深色模式', '🌙');
        }

        document.dispatchEvent(new CustomEvent('themechange', {
            detail: { theme }
        }));
    }

    /**
     * 添加主题变化监听器
     * @param {Function} callback - 回调函数
     */
    onThemeChange(callback) {
        document.addEventListener('themechange', (event) => {
            callback(event.detail.theme);
        });
    }
}

// 创建全局实例
window.ThemeManager = new ThemeManager();

// 自动初始化（如果DOM已加载）
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.ThemeManager.init();
    });
} else {
    window.ThemeManager.init();
}