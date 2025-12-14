/**
 * FTC Ticketing System – Web Console UI Script
 * Purpose: Bind UI events, render lists, wire to app.js helpers
 */
(function(){
  const $ = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

  // API base 可从本地设置覆盖
  let API_BASE = localStorage.getItem('ftc_api_base') || '/api';
  const apiUrl = (p) => `${API_BASE}${p}`;
  const api = {
    async getConfig(){ return fetch(apiUrl('/config')).then(r=>r.json()); },
    async listStations(){ return fetch(apiUrl('/stations')).then(r=>r.json()); },
    async addStation(s){ return fetch(apiUrl('/stations'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(s)}).then(r=>r.json()); },
    async delStation(code){ return fetch(apiUrl('/stations/'+encodeURIComponent(code)),{method:'DELETE'}).then(r=>r.json()); },
    async listLines(){ return fetch(apiUrl('/lines')).then(r=>r.json()); },
    async addLine(l){ return fetch(apiUrl('/lines'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(l)}).then(r=>r.json()); },
    async updateLine(id,l){ return fetch(apiUrl('/lines/'+encodeURIComponent(id)),{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(l)}).then(r=>r.json()); },
    async listFares(){ return fetch(apiUrl('/fares')).then(r=>r.json()); },
    async addFare(f){ return fetch(apiUrl('/fares'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(f)}).then(r=>r.json()); },
    async delFare(f){ return fetch(apiUrl('/fares'),{method:'DELETE',headers:{'Content-Type':'application/json'},body:JSON.stringify(f)}).then(r=>r.json()); },
    async recalculateFares(){ return fetch(apiUrl('/fares/recalculate'),{method:'POST'}).then(r=>r.json()); },
    async export(){
      // 优先使用后端导出端点，若不存在则拼装
      try{ return await fetch(apiUrl('/export')).then(r=>r.json()); }
      catch(e){
        const [config, stations, lines, fares] = await Promise.all([
          api.getConfig().catch(()=>({})),
          api.listStations().catch(()=>[]),
          api.listLines().catch(()=>[]),
          api.listFares().catch(()=>[]),
        ]);
        return {config, stations, lines, fares};
      }
    },
    async import(payload){
      try{ return await fetch(apiUrl('/import'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}).then(r=>r.json()); }
      catch(e){
        // 无导入端点时，逐项写入
        if(payload.stations){ for(const s of payload.stations){ await api.addStation(s).catch(()=>{}); } }
        if(payload.lines){ for(const l of payload.lines){ await api.addLine(l).catch(()=>{}); } }
        if(payload.fares){ for(const f of payload.fares){ await api.addFare(f).catch(()=>{}); } }
        return { ok:true };
      }
    },
    async reset(){
      try{ return await fetch(apiUrl('/reset'),{method:'POST'}).then(r=>r.json()); }
      catch(e){
        const stations = await api.listStations().catch(()=>[]);
        for(const s of stations){ await api.delStation(s.code).catch(()=>{}); }
        const lines = await api.listLines().catch(()=>[]);
        for(const l of lines){ await fetch(apiUrl('/lines/'+encodeURIComponent(l.id)),{method:'DELETE'}).catch(()=>{}); }
        const fares = await api.listFares().catch(()=>[]);
        for(const f of fares){ await api.delFare({from:f.from,to:f.to}).catch(()=>{}); }
        return { ok:true };
      }
    }
  };

  // Toast（操作结果提示）
  const toastEl = document.createElement('div');
  toastEl.className = 'toast';
  document.body.appendChild(toastEl);
  function showToast(msg, ms=1800){
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    setTimeout(()=>toastEl.classList.remove('show'), ms);
  }

  // 标签切换
  $$('.tab').forEach(btn=>{
    btn.addEventListener('click',()=>{
      $$('.tab').forEach(b=>b.classList.remove('active'));
      btn.classList.add('active');
      const target = btn.getAttribute('data-tab');
      $$('.view').forEach(v=>v.classList.remove('active'));
      $('#view-'+target).classList.add('active');
    });
  });

  // 仪表盘
  async function renderDashboard(){
    try{
      const [stations, lines] = await Promise.all([
        api.listStations(), api.listLines()
      ]);
      $('#stationCount').textContent = stations.length;
      $('#lineCount').textContent = lines.length;
    }catch(e){
      // 离线回退
      $('#stationCount').textContent = '0';
      $('#lineCount').textContent = '0';
    }
  }

  // 线路编辑器
  function drawLineRow(container, line, fares, stationsList){
    const row = document.createElement('div');
    row.className = 'line-row';
    const svg = document.createElementNS('http://www.w3.org/2000/svg','svg');
    svg.classList.add('line-svg');
    row.appendChild(svg);
    const label = document.createElement('div');
    label.className = 'line-label';
    label.textContent = `${line.en_name||line.id}`;
    row.appendChild(label);
    container.appendChild(row);

    const W = svg.clientWidth || 800;
    const H = svg.clientHeight || 80;
    const padding = 30;
    const color = line.color || 'orange';
    const stations = (line.stations||[]).slice();

    // 绘制主线
    const y = H/2;
    const main = document.createElementNS('http://www.w3.org/2000/svg','line');
    main.setAttribute('x1', String(padding));
    main.setAttribute('x2', String(W-padding));
    main.setAttribute('y1', String(y));
    main.setAttribute('y2', String(y));
    main.setAttribute('stroke', color);
    main.setAttribute('stroke-width', '4');
    svg.appendChild(main);

    const count = Math.max(stations.length, 5);
    const xs = [];
    for(let i=0;i<stations.length;i++){
      xs.push(padding + ((W-2*padding) * (i/(stations.length-1||1))));
    }
    // 站点渲染
    stations.forEach((code, i)=>{
      const cx = xs[i];
      const c = document.createElementNS('http://www.w3.org/2000/svg','circle');
      c.classList.add('station');
      c.setAttribute('cx', String(cx));
      c.setAttribute('cy', String(y));
      c.setAttribute('r', '6');
      c.setAttribute('fill', color);
      svg.appendChild(c);
      const stObj = Array.isArray(stationsList) ? stationsList.find(ss=>ss.code===code) : null;
      const enText = document.createElementNS('http://www.w3.org/2000/svg','text');
      enText.classList.add('label');
      enText.setAttribute('x', String(cx-16));
      enText.setAttribute('y', String(y-28));
      if(stObj?.en_name){ enText.textContent = stObj.en_name; svg.appendChild(enText); }
      const nameText = document.createElementNS('http://www.w3.org/2000/svg','text');
      nameText.classList.add('label');
      nameText.setAttribute('x', String(cx-16));
      nameText.setAttribute('y', String(y-14));
      nameText.textContent = stObj?.name || code;
      svg.appendChild(nameText);
      const codeText = document.createElementNS('http://www.w3.org/2000/svg','text');
      codeText.classList.add('label');
      codeText.setAttribute('x', String(cx-16));
      codeText.setAttribute('y', String(y));
      codeText.textContent = code;
      svg.appendChild(codeText);
    });

    // 区间票价标签显示（取双向任一已设票价）
    for(let i=0;i<stations.length-1;i++){
      const x1 = xs[i], x2 = xs[i+1];
      const mid = (x1 + x2) / 2;
      const fwd = Array.isArray(fares) ? fares.find(f=>f.from===stations[i] && f.to===stations[i+1]) : null;
      const rev = Array.isArray(fares) ? fares.find(f=>f.from===stations[i+1] && f.to===stations[i]) : null;
      const fare = fwd || rev;
      if(fare && fare.cost != null){
        const t = document.createElementNS('http://www.w3.org/2000/svg','text');
        t.classList.add('fare-label');
        t.setAttribute('x', String(mid - 12));
        t.setAttribute('y', String(y - 18));
        t.textContent = `¤${fare.cost}`;
        svg.appendChild(t);
      }
    }

    // 区间点击设置票价（并刷新显示）
    for(let i=0;i<stations.length-1;i++){
      const x1 = xs[i], x2 = xs[i+1];
      const r = document.createElementNS('http://www.w3.org/2000/svg','rect');
      r.classList.add('segment');
      r.setAttribute('x', String(Math.min(x1,x2)));
      r.setAttribute('y', String(y-10));
      r.setAttribute('width', String(Math.abs(x2-x1)));
      r.setAttribute('height', '20');
      r.setAttribute('fill', 'transparent');
      r.addEventListener('click', async ()=>{
        const current = Array.isArray(fares) ? (fares.find(f=>f.from===stations[i] && f.to===stations[i+1])?.cost || '') : '';
        const cost = Number(prompt(`设置区间票价 (${stations[i]} → ${stations[i+1]})`, String(current))) || 0;
        if(cost>=0){
          await api.addFare({from:stations[i], to:stations[i+1], cost}).catch(()=>{});
          // 刷新票价并重绘当前行
          const freshFares = await api.listFares().catch(()=>fares);
          container.innerHTML = '';
          drawLineRow(container, line, freshFares, stationsList);
          showToast('票价已更新');
        }
      });
      svg.appendChild(r);
    }

    // 在主线上点击新增站（输入编号与名称）
    svg.addEventListener('click', async (ev)=>{
      // 若点击的是区间命中层，则不触发新增站逻辑
      if(ev.target && ev.target.classList && ev.target.classList.contains('segment')) return;
      const idx = stations.length>0 ? stations.length : 0;
      const prefix = (stations[0]||'01-01').split('-')[0] || '01';
      const suggested = `${prefix}-${String(idx+1).padStart(2,'0')}`;
      const code = prompt('新站编号（如 01-02）', suggested);
      if(!code) return;
      const name = prompt('新站中文名', `站点${idx+1}`);
      if(!name) return;
      const enName = prompt('Station English Name', `Station${idx+1}`);
      if(!enName) return;
      stations.push(code.trim());
      // 持久化：新增站、更新线路
      await api.addStation({code: code.trim(), name: name.trim(), en_name: enName.trim()}).catch(()=>{});
      await api.updateLine(line.id, {...line, stations}).catch(()=>{});
      // 重新渲染
      container.innerHTML = '';
      drawLineRow(container, {...line, stations}, fares, stationsList);
    });

    // 为该线路添加按编号移除站点的按钮
    const removeBtn = document.createElement('button');
    removeBtn.textContent = '移除此线路的站点…';
    removeBtn.className = 'danger';
    removeBtn.addEventListener('click', async ()=>{
      const code = prompt('输入要移除的站点编号');
      if(!code) return;
      const stations2 = (line.stations||[]).filter(s=>s!==code.trim());
      await api.updateLine(line.id, {...line, stations: stations2}).catch(()=>{});
      container.innerHTML = '';
      drawLineRow(container, {...line, stations: stations2}, fares, stationsList);
      showToast('已从该线路移除此站点');
    });
    row.appendChild(removeBtn);
  }

  async function renderLineEditor(){
    const container = $('#lineEditor');
    container.innerHTML = '';
    try{
      const [lines, stations, fares] = await Promise.all([api.listLines(), api.listStations(), api.listFares()]);
      for(const line of lines){ drawLineRow(container, line, fares, stations); }
    }catch(e){
      container.textContent = '无法加载线路数据（API不可用）';
    }
  }

  // 新建线路
  $('#createLineBtn').addEventListener('click', async ()=>{
    const id = ($('#newLineId').value||'').trim();
    const color = $('#newLineColor').value||'orange';
    if(!id) return alert('请输入线路ID');
    const line = { id, en_name: `Line${id}`, cn_name: `线路${id}`, color, stations: [] };
    await api.addLine(line).catch(()=>{});
    $('#newLineId').value = '';
    await renderLineEditor();
  });

  // 系统设置 - 使用统一的ApiConfigManager
  $('#saveApiBaseBtn').addEventListener('click', ()=>{
    const v = ($('#apiBaseInput').value||'').trim();
    if(!v) {
      alert('请输入API地址');
      return;
    }

    if (window.ApiConfigManager && typeof window.ApiConfigManager.saveApiBase === 'function') {
      const success = window.ApiConfigManager.saveApiBase(v, 'browser_storage');
      if (success) {
        alert('API地址已保存到浏览器');
      } else {
        alert('保存失败，请检查API地址格式');
      }
    } else {
      // 回退到旧逻辑
      const API_BASE = v.endsWith('/api') ? v : (v+'/api');
      localStorage.setItem('ftc_api_base', API_BASE);
      alert('API地址已保存到浏览器（使用旧逻辑）');
    }
  });

  $('#exportBtn').addEventListener('click', async ()=>{
    const data = await api.export().catch(()=>({}));
    const blob = new Blob([JSON.stringify(data,null,2)],{type:'application/json'});
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'ftc_export.json';
    a.click();
    URL.revokeObjectURL(a.href);
  });

  $('#importFile').addEventListener('change', async (e)=>{
    const file = e.target.files[0];
    if(!file) return;
    const text = await file.text();
    const json = JSON.parse(text);
    await api.reset().catch(()=>{});
    await api.import(json).catch(()=>{});
    alert('导入完成');
    await renderDashboard();
    await renderLineEditor();
  });

  $('#resetDataBtn').addEventListener('click', async ()=>{
    if(!confirm('确认清空全部数据？')) return;
    await api.reset().catch(()=>{});
    await renderDashboard();
    await renderLineEditor();
  });

  // 重新计算票价
  $('#recalculateFaresBtn').addEventListener('click', async ()=>{
    if(!confirm('确认重新计算所有票价？这将基于当前线路和站点结构重新生成票价表。')) return;
    try {
      const result = await api.recalculateFares();
      showToast('票价重新计算完成');
      // 刷新线路编辑器以显示新的票价
      await renderLineEditor();
    } catch (e) {
      showToast('重新计算票价失败：' + (e.message || '未知错误'));
    }
  });

  // 子标签页切换功能
  document.querySelectorAll('.sub-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const subtabId = tab.getAttribute('data-subtab');

      // 更新活动标签
      document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      // 显示对应的子视图
      document.querySelectorAll('.sub-view').forEach(view => view.classList.remove('active'));
      const targetView = document.getElementById(`subview-${subtabId}`);
      if (targetView) {
        targetView.classList.add('active');
      }

      // 根据子标签页加载对应数据
      if (subtabId === 'ticket-calculator' || subtabId === 'ticket-export') {
        loadStationsForCalculator();
      } else if (subtabId === 'fare-chart') {
        loadFareChart();
      }
    });
  });

  // 加载车站数据到计算器下拉框
  async function loadStationsForCalculator() {
    try {
      const stations = await api.listStations();
      const startSelect = document.getElementById('startStation');
      const endSelect = document.getElementById('endStation');

      if (!startSelect || !endSelect) return;

      // 清空现有选项（保留第一个提示选项）
      while (startSelect.options.length > 1) startSelect.remove(1);
      while (endSelect.options.length > 1) endSelect.remove(1);

      stations.forEach(station => {
        const option = document.createElement('option');
        option.value = station.code;
        option.textContent = `${station.code} - ${station.name}`;
        startSelect.appendChild(option.cloneNode(true));
        endSelect.appendChild(option);
      });
    } catch (error) {
      console.error('加载车站数据失败:', error);
    }
  }

  // 辅助函数：生成车站分组键（同原版）
  const nameKey = s => String(`${(s?.name||'')}|${(s?.en_name||'')}`).trim().toLowerCase();

  // 解析车站编码，如 "01-01" -> {prefix: "01", num: 1}
  const parseCode = (code) => {
    const [p, n] = String(code || '').split('-');
    return { prefix: p, num: Number(n) || 0 };
  };

  // 获取两站之间的单段票价（支持双向查询）
  const segFare = (from, to, isRegular, fares) => {
    const f = fares.find(x => x.from === from && x.to === to) || fares.find(x => x.from === to && x.to === from) || {};
    return Number(isRegular ? (f.cost_regular ?? f.cost) : (f.cost_express ?? f.cost)) || 0;
  };

  // 计算同一条线路上两站之间的累计票价
  const sumLineFare = (a, b, isRegular, fares) => {
    const pa = parseCode(a), pb = parseCode(b);
    if (!pa.prefix || !pb.prefix || pa.prefix !== pb.prefix) {
      return null; // 不同线不累计
    }
    const lo = Math.min(pa.num, pb.num), hi = Math.max(pa.num, pb.num);
    let total = 0;
    for (let i = lo; i < hi; i++) {
      const from = `${pa.prefix}-${String(i).padStart(2, '0')}`;
      const to   = `${pa.prefix}-${String(i + 1).padStart(2, '0')}`;
      total += segFare(from, to, isRegular, fares);
    }
    return total;
  };

  // 计算两站之间的最优票价（简化版：优先同线累计，否则直接票价）
  const bestFareBetweenCodes = (a, b, isRegular, fares) => {
    const vLine = sumLineFare(a, b, isRegular, fares);
    const f = fares.find(x => (x.from === a && x.to === b) || (x.from === b && x.to === a)) || {};
    const vSeg = Number(isRegular ? (f.cost_regular ?? f.cost) : (f.cost_express ?? f.cost)) || 0;
    return (vLine != null && vLine > 0) ? vLine : (vSeg > 0 ? vSeg : null);
  };

  // 加载票价图数据
  async function loadFareChart() {
    try {
      // 获取所有数据
      const [fares, stations, lines] = await Promise.all([
        api.listFares(),
        api.listStations(),
        api.listLines().catch(() => []) // 线路数据可能不存在
      ]);

      // 生成票价表HTML
      renderFareTable('regular', fares, stations, lines);
      // 特急票价表暂时与普通票价相同（当前系统只有普通票价）
      renderFareTable('express', fares, stations, lines);

      showToast('票价数据加载完成');
    } catch (error) {
      console.error('加载票价数据失败:', error);
      showToast('加载票价数据失败: ' + (error.message || '未知错误'));
    }
  }

  // 渲染票价表（仿原版分组样式）
  function renderFareTable(type, fares, stations, lines) {
    const containerId = `${type}-fare-table-container`;
    const container = document.getElementById(containerId);
    if (!container) return;

    if (fares.length === 0) {
      container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--muted);">暂无票价数据</div>';
      return;
    }

    const isRegular = type === 'regular';

    // 1. 车站排序与分组（同原版）
    const sorted = [...stations].sort((a, b) => (a.code || '').localeCompare(b.code || ''));
    const groupMap = {};
    for (const s of sorted) {
      const k = nameKey(s);
      if (!k) continue;
      if (!groupMap[k]) groupMap[k] = { name: s.name || '', en_name: s.en_name || '', codes: [] };
      groupMap[k].codes.push(s.code);
    }
    const displayGroups = Object.values(groupMap).sort((a, b) => (a.codes[0] || '').localeCompare(b.codes[0] || ''));

    // 2. 预计算每个站在哪些线路上
    const linesForStation = {};
    (lines || []).forEach(li => {
      (li.stations || []).forEach(code => {
        if (!linesForStation[code]) linesForStation[code] = [];
        linesForStation[code].push(li);
      });
    });

    // 3. 计算两个车站之间的线路颜色（简化版）
    const colorsForRoute = (a, b) => {
      const la = linesForStation[a] || [];
      const lb = linesForStation[b] || [];
      // 同一条线路：取交集的第一种颜色
      const same = la.find(x => lb.some(y => y.id === x.id));
      if (same) return [same.color || '#93a2b7'];
      // 不同线路：返回两条线路的颜色（最多2种）
      const colors = [];
      if (la.length > 0) colors.push(la[0].color || '#93a2b7');
      if (lb.length > 0 && lb[0].id !== la[0]?.id) colors.push(lb[0].color || '#93a2b7');
      return colors.slice(0, 2);
    };

    // 4. 计算两个分组之间的线路颜色集合
    const colorsForGroups = (ga, gb) => {
      const set = new Set();
      for (const a of ga.codes) {
        for (const b of gb.codes) {
          for (const c of colorsForRoute(a, b)) set.add(c);
        }
      }
      return Array.from(set).slice(0, 3);
    };

    // 5. 计算最大票价用于颜色归一化（可选）
    let maxFare = 1;
    for (const ga of displayGroups) {
      for (const gb of displayGroups) {
        if (ga === gb) continue;
        let best = null;
        for (const a of ga.codes) {
          for (const b of gb.codes) {
            const v = bestFareBetweenCodes(a, b, isRegular, fares);
            if (v != null) best = (best == null ? v : Math.max(best, v));
          }
        }
        if (best != null) maxFare = Math.max(maxFare, best);
      }
    }

    // 6. 构建表格
    const table = document.createElement('table');
    table.className = 'fare-grid';

    // 表头
    const thead = document.createElement('thead');
    const trh = document.createElement('tr');
    trh.appendChild(document.createElement('th')); // 左上角空白
    for (const g of displayGroups) {
      const th = document.createElement('th');
      th.innerHTML = `<div class="label-cn">${g.name || g.codes[0]}</div><div class="label-en">${g.en_name || ''}</div><div class="label-code">${(g.codes || []).join(' | ')}</div>`;
      trh.appendChild(th);
    }
    thead.appendChild(trh);
    table.appendChild(thead);

    // 表格主体
    const tbody = document.createElement('tbody');
    for (const ga of displayGroups) {
      const tr = document.createElement('tr');
      const th = document.createElement('th');
      th.innerHTML = `<div class="label-cn">${ga.name || ga.codes[0]}</div><div class="label-en">${ga.en_name || ''}</div><div class="label-code">${(ga.codes || []).join(' | ')}</div>`;
      tr.appendChild(th);

      for (const gb of displayGroups) {
        const td = document.createElement('td');
        if (ga === gb) {
          td.textContent = '-';
          td.style.color = '#666';
          tr.appendChild(td);
          continue;
        }

        // 计算两组间最优票价
        let best = null;
        for (const a of ga.codes) {
          for (const b of gb.codes) {
            const v = bestFareBetweenCodes(a, b, isRegular, fares);
            if (v != null) best = (best == null ? v : Math.min(best, v));
          }
        }

        // 单元格内容
        const cell = document.createElement('div');
        cell.className = 'fare-cell';
        cell.textContent = (best != null ? best : '');
        cell.style.fontSize = '12px';
        td.appendChild(cell);

        // 线路颜色点
        const cols = colorsForGroups(ga, gb);
        if (cols && cols.length) {
          const dots = document.createElement('div');
          dots.className = 'line-dots';
          cols.forEach(col => {
            const d = document.createElement('span');
            d.className = 'dot';
            d.style.backgroundColor = col;
            dots.appendChild(d);
          });
          td.appendChild(dots);
        }

        tr.appendChild(td);
      }
      tbody.appendChild(tr);
    }
    table.appendChild(tbody);

    // 清空容器并添加表格
    container.innerHTML = '';
    container.appendChild(table);
  }

  // 车票计算功能
  document.getElementById('calculateTicketBtn')?.addEventListener('click', async () => {
    const startCode = document.getElementById('startStation').value;
    const endCode = document.getElementById('endStation').value;

    if (!startCode || !endCode) {
      alert('请选择起点站和终点站');
      return;
    }

    if (startCode === endCode) {
      alert('起点站和终点站不能相同');
      return;
    }

    try {
      // 这里调用API计算票价
      // 注意：实际API端点可能需要调整
      const response = await fetch(apiUrl('/fares/calculate'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ start: startCode, end: endCode })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const fareData = await response.json();

      // 显示结果
      document.getElementById('ticketResult').style.display = 'block';
      document.getElementById('ticketStartStation').textContent = fareData.start_name || startCode;
      document.getElementById('ticketEndStation').textContent = fareData.end_name || endCode;
      document.getElementById('ticketPrice').textContent = fareData.price ? fareData.price.toFixed(2) : '0.00';
      document.getElementById('ticketDistance').textContent = fareData.distance ? `${fareData.distance} km` : '-';
      document.getElementById('ticketFareType').textContent = fareData.fare_type || '普通票';
      document.getElementById('ticketNumber').textContent = fareData.ticket_number || `TICKET-${Date.now().toString().slice(-6)}`;

      // 存储车票数据供导出使用
      window.currentTicketData = fareData;
      window.currentTicketData.start_code = startCode;
      window.currentTicketData.end_code = endCode;

    } catch (error) {
      console.error('计算票价失败:', error);
      alert('计算票价失败: ' + (error.message || '未知错误'));
    }
  });

  // 车票生成功能
  document.getElementById('generateTicketBtn')?.addEventListener('click', () => {
    if (!window.currentTicketData) {
      alert('请先使用车票计算器计算票价');
      return;
    }

    const canvas = document.getElementById('ticketCanvas');
    const ctx = canvas.getContext('2d');

    // 清空画布
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 设置背景
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // 绘制车票边框
    ctx.strokeStyle = '#3b82f6';
    ctx.lineWidth = 4;
    ctx.strokeRect(10, 10, canvas.width - 20, canvas.height - 20);

    // 绘制标题
    ctx.fillStyle = '#1f2937';
    ctx.font = 'bold 32px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('火车票', canvas.width / 2, 70);

    // 绘制票号
    ctx.fillStyle = '#6b7280';
    ctx.font = '18px monospace';
    ctx.textAlign = 'left';
    ctx.fillText(`票号: ${window.currentTicketData.ticket_number || 'TICKET-000000'}`, 40, 120);

    // 绘制路线信息
    ctx.fillStyle = '#1f2937';
    ctx.font = 'bold 24px Arial';
    ctx.fillText(`${window.currentTicketData.start_name || window.currentTicketData.start_code} → ${window.currentTicketData.end_name || window.currentTicketData.end_code}`, 40, 180);

    // 绘制票价
    ctx.fillStyle = '#ef4444';
    ctx.font = 'bold 48px Arial';
    ctx.textAlign = 'right';
    ctx.fillText(`¥${window.currentTicketData.price ? window.currentTicketData.price.toFixed(2) : '0.00'}`, canvas.width - 40, 180);

    // 绘制详细信息
    ctx.fillStyle = '#4b5563';
    ctx.font = '18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText(`距离: ${window.currentTicketData.distance || '0'} km`, 40, 240);
    ctx.fillText(`票价类型: ${window.currentTicketData.fare_type || '普通票'}`, 40, 270);

    // 绘制时间和日期
    const now = new Date();
    ctx.fillText(`日期: ${now.toLocaleDateString('zh-CN')}`, 40, 320);
    ctx.fillText(`时间: ${now.toLocaleTimeString('zh-CN', { hour12: false })}`, 40, 350);

    // 绘制底部信息
    ctx.fillStyle = '#9ca3af';
    ctx.font = '14px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('请妥善保管车票，遗失不补 | 铁路票务系统', canvas.width / 2, 380);

    // 启用下载和打印按钮
    document.getElementById('downloadTicketBtn').disabled = false;
    document.getElementById('printTicketBtn').disabled = false;
  });

  // 车票下载功能
  document.getElementById('downloadTicketBtn')?.addEventListener('click', () => {
    const canvas = document.getElementById('ticketCanvas');
    const link = document.createElement('a');
    link.download = `ticket-${window.currentTicketData?.ticket_number || Date.now()}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  });

  // 车票打印功能
  document.getElementById('printTicketBtn')?.addEventListener('click', () => {
    const canvas = document.getElementById('ticketCanvas');
    const dataUrl = canvas.toDataURL('image/png');
    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
      <html>
        <head><title>打印车票</title></head>
        <body style="margin: 0; padding: 20px; text-align: center;">
          <img src="${dataUrl}" style="max-width: 100%;" />
          <script>
            window.onload = function() { window.print(); }
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  });

  // 票价图功能
  // 切换普通/特急票价表
  document.getElementById('showRegularChart')?.addEventListener('click', () => {
    document.querySelectorAll('.fare-chart-view').forEach(view => view.classList.remove('active'));
    document.getElementById('fare-chart-regular').classList.add('active');
    document.querySelectorAll('.fare-chart-toolbar .sub-tab').forEach(btn => btn.classList.remove('active'));
    document.getElementById('showRegularChart').classList.add('active');
  });

  document.getElementById('showExpressChart')?.addEventListener('click', () => {
    document.querySelectorAll('.fare-chart-view').forEach(view => view.classList.remove('active'));
    document.getElementById('fare-chart-express').classList.add('active');
    document.querySelectorAll('.fare-chart-toolbar .sub-tab').forEach(btn => btn.classList.remove('active'));
    document.getElementById('showExpressChart').classList.add('active');
  });

  // 刷新票价数据
  document.getElementById('refreshFareChart')?.addEventListener('click', () => {
    loadFareChart();
  });

  // 导出PNG（支持html2canvas与Canvas回退）
  document.getElementById('exportChartPng')?.addEventListener('click', async () => {
    try {
      const activeView = document.querySelector('.fare-chart-view.active');
      if (!activeView) {
        alert('请先选择要导出的票价表');
        return;
      }

      const table = activeView.querySelector('table.fare-grid');
      if (!table) {
        alert('未找到票价表格');
        return;
      }

      const isRegular = activeView.id === 'fare-chart-regular';
      const name = `fare-chart-${isRegular ? 'regular' : 'express'}`;
      let canvas;

      if (typeof html2canvas === 'function') {
        // 使用html2canvas库
        const bg = getComputedStyle(document.body).getPropertyValue('--bg') || '#000';
        const fullW = Math.max(table.scrollWidth, table.offsetWidth);
        const fullH = Math.max(table.scrollHeight, table.offsetHeight);

        canvas = await html2canvas(table, {
          backgroundColor: bg,
          useCORS: true,
          allowTaint: true,
          scale: 2,
          width: fullW,
          height: fullH,
          windowWidth: fullW,
          windowHeight: fullH,
          scrollX: 0,
          scrollY: 0,
          onclone: (doc) => {
            const clonedTable = doc.querySelector('table.fare-grid');
            if (clonedTable) {
              clonedTable.style.overflow = 'visible';
              clonedTable.style.maxHeight = 'none';
              clonedTable.style.maxWidth = 'none';
            }
          }
        });
      } else {
        // Canvas回退方案
        const rows = Array.from(table.querySelectorAll('tr'));
        const cols = Array.from(rows[0].children);
        const cw = cols.map(c => (c.scrollWidth || c.offsetWidth || 80));
        const ch = rows.map(r => (r.scrollHeight || r.offsetHeight || 28));
        const pad = 12, bw = 1;
        const W = cw.reduce((a, b) => a + b, 0) + pad * 2;
        const H = ch.reduce((a, b) => a + b, 0) + pad * 2;

        canvas = document.createElement('canvas');
        canvas.width = W;
        canvas.height = H;
        const ctx = canvas.getContext('2d');
        const bg = getComputedStyle(document.body).getPropertyValue('--bg') || '#000';
        ctx.fillStyle = bg;
        ctx.fillRect(0, 0, W, H);
        ctx.font = '12px Segoe UI';
        ctx.textBaseline = 'middle';

        let y = pad;
        rows.forEach((r, ri) => {
          let x = pad;
          const cells = Array.from(r.children);
          cells.forEach((cell, ci) => {
            const w = cw[ci] || 80;
            const h = ch[ri] || 28;
            const bg = window.getComputedStyle(cell).backgroundColor;
            ctx.fillStyle = (bg && bg !== 'rgba(0, 0, 0, 0)') ? bg : (ri === 0 || cell.tagName === 'TH' ? '#0b0b0b' : '#111');
            ctx.fillRect(x, y, w, h);
            ctx.strokeStyle = '#1a1a1a';
            ctx.lineWidth = bw;
            ctx.strokeRect(x, y, w, h);
            ctx.fillStyle = '#e5e5e5';
            const txt = (cell.innerText || '').trim();
            ctx.save();
            ctx.beginPath();
            ctx.rect(x + 4, y + 2, w - 8, h - 4);
            ctx.clip();
            ctx.fillText(txt, x + 6, y + h / 2);

            // 画彩色小点（若存在）
            const dots = cell.querySelectorAll('.line-dots .dot');
            if (dots && dots.length) {
              let dx = x + 8;
              const dy = y + 8;
              dots.forEach(d => {
                ctx.beginPath();
                ctx.fillStyle = getComputedStyle(d).backgroundColor;
                ctx.arc(dx, dy, 3, 0, Math.PI * 2);
                ctx.fill();
                dx += 10;
              });
            }
            ctx.restore();
            x += w;
          });
          y += ch[ri] || 28;
        });
      }

      const url = canvas.toDataURL('image/png');
      const a = document.createElement('a');
      a.href = url;
      a.download = `${name}.png`;
      a.click();
      showToast('票价表已导出为PNG图片');
    } catch (error) {
      console.error('导出PNG失败:', error);
      alert('导出PNG失败: ' + (error.message || '未知错误'));
    }
  });

  // 导出Excel（支持XLSX与CSV回退，导出当前票价图表格）
  document.getElementById('exportChartExcel')?.addEventListener('click', async () => {
    try {
      const activeView = document.querySelector('.fare-chart-view.active');
      if (!activeView) {
        alert('请先选择要导出的票价表');
        return;
      }

      const table = activeView.querySelector('table.fare-grid');
      if (!table) {
        alert('未找到票价表格');
        return;
      }

      const isRegular = activeView.id === 'fare-chart-regular';
      const name = `fare-chart-${isRegular ? 'regular' : 'express'}`;

      if (typeof XLSX !== 'undefined' && XLSX && XLSX.utils && XLSX.writeFile) {
        // 使用SheetJS库导出为xlsx
        const wb = XLSX.utils.table_to_book(table, { sheet: isRegular ? 'Regular' : 'Express' });
        XLSX.writeFile(wb, name + '.xlsx');
      } else {
        // 回退：CSV格式
        const rows = [];
        table.querySelectorAll('tr').forEach(tr => {
          const cells = Array.from(tr.querySelectorAll('th,td')).map(td =>
            JSON.stringify(td.innerText.replace(/\s+/g, ' ').trim())
          );
          rows.push(cells.join(','));
        });
        const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = name + '.csv';
        a.click();
      }
      showToast('票价表已导出');
    } catch (error) {
      console.error('导出票价表失败:', error);
      alert('导出失败: ' + (error.message || '未知错误'));
    }
  });

  // 初始渲染
  renderDashboard();
  renderLineEditor();
})();