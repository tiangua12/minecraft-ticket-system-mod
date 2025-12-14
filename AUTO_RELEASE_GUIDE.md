# 自动发布到GitHub指南

本指南将帮助您将Minecraft模组自动发布到GitHub Releases。

## 整体流程

1. **准备阶段** - 初始化Git仓库并推送到GitHub
2. **配置阶段** - 设置GitHub Token和工作流权限
3. **发布阶段** - 使用脚本创建标签并触发自动构建
4. **下载阶段** - 从GitHub Releases下载编译好的JAR文件

## 第一步：准备GitHub仓库

### 1.1 创建GitHub仓库（如果还没有）
1. 访问 https://github.com/new
2. 输入仓库名称（如 `minecraft-ticket-system`）
3. **重要**：不要初始化README、.gitignore或许可证文件
4. 点击"Create repository"

### 1.2 获取GitHub Token（必需）

GitHub Actions需要Token来创建Release：

1. **访问Token创建页面**：
   - 登录GitHub
   - 访问 https://github.com/settings/tokens
   - 点击 "Generate new token" → "Generate new token (classic)"

2. **配置Token权限**：
   - Token描述：`Minecraft Mod Auto Release`
   - 选择权限：
     - `repo` (Full control of private repositories) - **必需**
     - `workflow` - **必需**
   - 点击 "Generate token"

3. **保存Token**：
   - **立即复制Token**，离开页面后无法再次查看
   - 如果丢失，需要重新生成

### 1.3 在仓库中配置Token
1. 进入你的仓库页面
2. 点击 "Settings" → "Secrets and variables" → "Actions"
3. 点击 "New repository secret"
4. 输入：
   - Name: `GITHUB_TOKEN` (必须使用这个名称)
   - Secret: 粘贴刚才复制的Token
5. 点击 "Add secret"

## 第二步：初始化本地Git仓库

### 2.1 运行初始化脚本
```bash
# 使脚本可执行
chmod +x init_git.sh

# 运行初始化脚本
./init_git.sh
```

脚本将引导你：
1. 初始化本地git仓库
2. 配置用户信息
3. 提交所有文件
4. 设置远程仓库URL

### 2.2 手动初始化（可选）
如果不想使用脚本，可以手动执行：

```bash
# 初始化git
git init

# 配置用户
git config user.name "你的用户名"
git config user.email "你的邮箱"

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit: Minecraft Ticket System Mod"

# 添加远程仓库
git remote add origin https://github.com/你的用户名/你的仓库名.git

# 推送
git branch -M main
git push -u origin main
```

## 第三步：启用GitHub Actions权限

1. 进入仓库页面：`https://github.com/你的用户名/你的仓库名`
2. 点击 "Settings" → "Actions" → "General"
3. 向下滚动到 "Workflow permissions"
4. 选择：
   - ✅ "Read and write permissions"
   - ✅ "Allow GitHub Actions to create and approve pull requests"
5. 点击 "Save"

## 第四步：测试自动发布

### 4.1 运行发布脚本
```bash
# 使脚本可执行
chmod +x release.sh

# 测试发布（使用测试标签）
./release.sh v1.0.0-test
```

脚本将：
1. 检查git状态
2. 创建标签 `v1.0.0-test`
3. 推送标签到GitHub
4. 触发GitHub Actions工作流

### 4.2 监控构建进度
1. 进入仓库页面
2. 点击 "Actions" 标签页
3. 查看 "Build and Release" 工作流状态
4. 点击运行中的工作流查看详细日志

### 4.3 查看发布结果
构建完成后：
1. 点击仓库的 "Releases" 标签页
2. 找到 `v1.0.0-test` 版本
3. 下载 `ticketsystem-*.jar` 文件

## 第五步：正式发布

### 5.1 更新版本号
编辑 `gradle.properties` 文件：
```properties
mod_version=1.3.3  # 更新版本号
```

### 5.2 创建正式发布
```bash
# 使用新版本号
./release.sh v1.3.3
```

### 5.3 或者使用自动版本（推荐）
```bash
# 脚本会自动从gradle.properties读取版本号
./release.sh
```

## 工作原理

### GitHub Actions工作流 (.github/workflows/release.yml)
触发条件：当推送 `v*` 标签时（如 `v1.0.0`）

工作流步骤：
1. **检出代码** - 获取仓库内容
2. **设置Java环境** - JDK 17（Minecraft 1.20.1要求）
3. **缓存依赖** - 加速后续构建
4. **构建项目** - 运行 `./gradlew build`
5. **提取版本信息** - 从gradle.properties和标签名
6. **创建GitHub Release** - 包含：
   - 编译的JAR文件
   - 许可证文件
   - 说明文档
   - 变更日志

### 发布脚本 (release.sh)
- 验证git状态
- 提取版本号
- 创建并推送git标签
- 触发GitHub Actions

## 常见问题

### Q: 编译失败怎么办？
**A:** 检查GitHub Actions日志：
1. 进入仓库 → Actions → 失败的运行
2. 查看错误信息
3. 常见问题：
   - Java版本不匹配（需要JDK 17）
   - 依赖下载失败（网络问题）
   - 构建脚本错误

### Q: Token无效或权限不足？
**A:** 重新生成Token并检查权限：
1. 确认Token具有 `repo` 和 `workflow` 权限
2. 确认仓库Secrets中的Token名称是 `GITHUB_TOKEN`
3. 确认仓库Settings → Actions权限已启用

### Q: Release已创建但没有JAR文件？
**A:** 可能原因：
1. 构建失败但Release仍被创建
2. JAR文件路径不正确
3. 检查Actions日志中的构建步骤

### Q: 如何更新许可证或添加文件到Release？
**A:** 编辑 `.github/workflows/release.yml` 的 `files` 部分：
```yaml
files: |
  build/libs/*.jar
  LICENSE
  MIT-LICENSE.txt
  NOTICE.txt
  CREDITS.txt
  changelog.txt
  README.md  # 添加其他文件
```

## 文件说明

| 文件 | 用途 |
|------|------|
| `.github/workflows/release.yml` | GitHub Actions工作流配置 |
| `release.sh` | 本地发布脚本 |
| `init_git.sh` | Git仓库初始化脚本 |
| `LICENSE` | GPLv3许可证文件 |
| `MIT-LICENSE.txt` | 引用的MIT许可证 |
| `NOTICE.txt` | 许可证引用说明 |
| `add_copyright.sh` | 添加版权头脚本（可删除） |

## 高级配置

### 自定义发布说明
编辑 `.github/workflows/release.yml` 中的 `body` 部分来自定义Release说明。

### 添加测试步骤
在工作流中添加测试步骤：
```yaml
- name: Run tests
  run: ./gradlew test
```

### 多版本发布
可以配置工作流同时构建多个Minecraft版本（需要修改构建配置）。

## 安全提示

1. **保护GitHub Token**：不要将Token提交到代码中
2. **定期更新Token**：建议每90天更新一次
3. **最小权限原则**：Token只授予必要的权限
4. **撤销泄露的Token**：如果Token意外泄露，立即撤销

## 支持

如果遇到问题：
1. 查看GitHub Actions日志
2. 检查本指南中的常见问题
3. 确保所有步骤正确执行

**记住**：首次运行可能需要一些时间，GitHub Actions需要准备环境和下载依赖。