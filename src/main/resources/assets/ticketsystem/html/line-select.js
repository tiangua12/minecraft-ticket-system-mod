/**
 * 线路选择功能
 * 用于index.html中的线路选择标签页
 */

// API帮助函数（复用main.js中的api对象或自己定义）
async function fetchJSON(input, init) {
    const res = await fetch(input, init);
    if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status} ${res.statusText}${txt ? ' - ' + txt.slice(0, 120) : ''}`);
    }
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const txt = await res.text().catch(() => '');
        throw new Error(`Non-JSON response: ${txt.slice(0, 120)}`);
    }
    return res.json();
}

const lineSelectAPI = {
    async listLines() { return fetchJSON('/api/lines'); },
    async deleteLine(id) { return fetchJSON('/api/lines/' + encodeURIComponent(id), { method: 'DELETE' }); },
    async createLine(lineData) { return fetchJSON('/api/lines', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(lineData) }); }
};

// DOM元素
function el(id) { return document.getElementById(id); }

// 全局变量
let allLines = [];
let filteredLines = [];

// 初始化线路选择功能
function initLineSelect() {
    const lineSelectTab = el('tab-line-select');
    if (!lineSelectTab) return; // 当前未显示线路选择标签页

    // 绑定事件
    const refreshBtn = el('refresh-lines');
    const createBtn = el('create-new-line');
    const searchInput = el('line-search');

    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadLines);
    }

    if (createBtn) {
        createBtn.addEventListener('click', createNewLine);
    }

    if (searchInput) {
        searchInput.addEventListener('input', filterLines);
    }

    // 如果当前显示的是线路选择标签页，则加载线路
    if (lineSelectTab.classList.contains('show')) {
        loadLines();
    }
}

// 加载线路列表
async function loadLines() {
    try {
        const linesListEl = el('lines-list');
        if (linesListEl) {
            linesListEl.innerHTML = '<div class="card" style="text-align:center; padding:20px; color:#888;">加载中...</div>';
        }

        allLines = await lineSelectAPI.listLines();
        filteredLines = [...allLines];

        renderLinesList();

        // 显示成功消息
        showLineSelectToast(`已加载 ${allLines.length} 条线路`, 'success');
    } catch (error) {
        console.error('加载线路失败:', error);
        showLineSelectToast(`加载线路失败: ${error.message}`, 'error');

        const linesListEl = el('lines-list');
        if (linesListEl) {
            linesListEl.innerHTML = '<div class="card" style="text-align:center; padding:20px; color:#ff6b6b;">加载失败，请检查网络连接</div>';
        }
    }
}

// 过滤线路
function filterLines() {
    const searchInput = el('line-search');
    if (!searchInput) return;

    const searchTerm = searchInput.value.trim().toLowerCase();

    if (!searchTerm) {
        filteredLines = [...allLines];
    } else {
        filteredLines = allLines.filter(line => {
            return (
                (line.id && line.id.toLowerCase().includes(searchTerm)) ||
                (line.name && line.name.toLowerCase().includes(searchTerm)) ||
                (line.color && line.color.toLowerCase().includes(searchTerm))
            );
        });
    }

    renderLinesList();
}

// 渲染线路列表
function renderLinesList() {
    const linesListEl = el('lines-list');
    if (!linesListEl) return;

    if (filteredLines.length === 0) {
        linesListEl.innerHTML = `
            <div class="card" style="text-align:center; padding:40px; color:#888;">
                ${allLines.length === 0 ? '暂无线路数据' : '无匹配的线路'}
                <br><br>
                <button onclick="createNewLine()" class="btn primary" style="margin-top:12px;">创建第一条线路</button>
            </div>
        `;
        return;
    }

    let html = '';
    filteredLines.forEach(line => {
        const stationCount = line.stations ? line.stations.length : 0;
        const colorStyle = line.color ? `style="background-color: ${line.color};"` : '';

        html += `
            <div class="card line-card" data-line-id="${line.id}" style="cursor: pointer; margin-bottom: 12px;">
                <div style="display: flex; align-items: center; justify-content: space-between;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span class="color-badge" ${colorStyle}></span>
                            <strong>${line.name || line.id}</strong>
                            <span style="font-size: 12px; color: #888; background: #f0f0f0; padding: 2px 6px; border-radius: 4px;">${line.id}</span>
                        </div>
                        <div style="margin-top: 8px; font-size: 14px; color: #666;">
                            ${stationCount} 个车站
                            ${line.color ? `<span style="margin-left: 12px;">颜色: ${line.color}</span>` : ''}
                        </div>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn" onclick="event.stopPropagation(); editLine('${line.id}')">编辑</button>
                        <button class="btn danger" onclick="event.stopPropagation(); deleteLine('${line.id}')">删除</button>
                    </div>
                </div>
            </div>
        `;
    });

    linesListEl.innerHTML = html;

    // 添加点击事件（点击卡片进入编辑页面）
    document.querySelectorAll('.line-card').forEach(card => {
        card.addEventListener('click', function(e) {
            // 如果点击的是按钮，不触发卡片点击
            if (e.target.tagName === 'BUTTON' || e.target.closest('button')) {
                return;
            }
            const lineId = this.dataset.lineId;
            editLine(lineId);
        });
    });
}

// 编辑线路 - 跳转到线路编辑页面
function editLine(lineId) {
    window.location.href = `line-edit.html?id=${encodeURIComponent(lineId)}`;
}

// 创建新线路
async function createNewLine() {
    try {
        // 生成线路ID（简单实现）
        const newId = 'L' + Date.now().toString().slice(-6);
        const lineData = {
            id: newId,
            name: '新线路',
            color: '#3366CC',
            stations: []
        };

        const createdLine = await lineSelectAPI.createLine(lineData);

        showLineSelectToast('新线路创建成功', 'success');

        // 跳转到编辑页面
        setTimeout(() => {
            editLine(newId);
        }, 500);

    } catch (error) {
        console.error('创建线路失败:', error);
        showLineSelectToast(`创建线路失败: ${error.message}`, 'error');
    }
}

// 删除线路
async function deleteLine(lineId) {
    const line = allLines.find(l => l.id === lineId);
    if (!line) return;

    if (!confirm(`确定要删除线路 "${line.name || lineId}" 吗？此操作不可恢复。`)) {
        return;
    }

    try {
        await lineSelectAPI.deleteLine(lineId);
        showLineSelectToast(`线路 "${line.name || lineId}" 已删除`, 'success');

        // 重新加载列表
        loadLines();
    } catch (error) {
        console.error('删除线路失败:', error);
        showLineSelectToast(`删除线路失败: ${error.message}`, 'error');
    }
}

// 显示提示消息
function showLineSelectToast(message, type = 'info') {
    // 尝试使用main.js的showToast函数，如果不存在则创建简单的提示
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
        return;
    }

    // 简单的toast实现
    let toast = el('line-select-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'line-select-toast';
        toast.style.cssText = `
            position: fixed;
            bottom: 20px;
            left: 50%;
            transform: translateX(-50%);
            padding: 12px 24px;
            background: #333;
            color: white;
            border-radius: 6px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 1000;
            opacity: 0;
            transition: opacity 0.3s;
        `;
        document.body.appendChild(toast);
    }

    toast.textContent = message;

    // 根据类型设置颜色
    if (type === 'success') {
        toast.style.background = '#28a745';
    } else if (type === 'error') {
        toast.style.background = '#dc3545';
    } else {
        toast.style.background = '#333';
    }

    toast.style.opacity = '1';

    // 3秒后隐藏
    setTimeout(() => {
        toast.style.opacity = '0';
    }, 3000);
}

// 添加CSS样式
function addLineSelectStyles() {
    const styleId = 'line-select-styles';
    if (document.getElementById(styleId)) return;

    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = `
        .color-badge {
            display: inline-block;
            width: 16px;
            height: 16px;
            border-radius: 4px;
            border: 1px solid rgba(0,0,0,0.1);
        }
        .line-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            transition: all 0.2s;
        }
        .btn.danger {
            background: #dc3545;
            color: white;
        }
        .btn.danger:hover {
            background: #c82333;
        }
    `;
    document.head.appendChild(style);
}

// 当线路选择标签页显示时初始化
document.addEventListener('DOMContentLoaded', function() {
    // 添加样式
    addLineSelectStyles();

    // 监听标签页切换
    const tabButtons = document.querySelectorAll('.tab[data-tab]');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabName = this.dataset.tab;
            if (tabName === 'line-select') {
                // 延迟一点初始化，确保DOM已更新
                setTimeout(initLineSelect, 50);
            }
        });
    });

    // 检查当前是否已经是线路选择标签页
    const lineSelectTab = el('tab-line-select');
    if (lineSelectTab && lineSelectTab.classList.contains('show')) {
        setTimeout(initLineSelect, 100);
    }
});

// 导出函数到全局作用域
window.editLine = editLine;
window.createNewLine = createNewLine;
window.deleteLine = deleteLine;