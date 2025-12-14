#!/bin/bash

# Git仓库初始化脚本
# 使用方法: ./init_git.sh [GitHub仓库URL]
# 例如: ./init_git.sh https://github.com/你的用户名/你的仓库名.git

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查git是否安装
check_git() {
    if ! command -v git &> /dev/null; then
        error "git未安装，请先安装git"
        exit 1
    fi
}

# 初始化git仓库
init_repo() {
    if [ -d .git ]; then
        warn "当前目录已经是git仓库"
        echo -n "是否重新初始化？(y/N): "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            info "已取消"
            exit 0
        fi
        # 备份现有的.git目录（可选）
        if [ -d .git ]; then
            mv .git .git.backup.$(date +%s)
            warn "已备份现有.git目录到 .git.backup.*"
        fi
    fi

    info "初始化git仓库..."
    git init

    info "配置git用户信息..."
    if [ -z "$(git config user.name)" ]; then
        echo -n "请输入你的用户名（用于git提交）: "
        read -r username
        git config user.name "$username"
    fi

    if [ -z "$(git config user.email)" ]; then
        echo -n "请输入你的邮箱（用于git提交）: "
        read -r email
        git config user.email "$email"
    fi

    info "添加文件到git..."
    git add .

    info "创建初始提交..."
    git commit -m "Initial commit: Minecraft Ticket System Mod

- 基于GPLv3许可证
- 包含MIT许可证的前端代码引用
- Minecraft 1.20.1模组
- 自动构建和发布配置"

    info "✅ Git仓库初始化完成"
}

# 设置远程仓库
setup_remote() {
    local remote_url="$1"

    if [ -z "$remote_url" ]; then
        echo ""
        info "现在需要设置GitHub远程仓库"
        echo "请先在GitHub上创建新仓库："
        echo "  1. 访问 https://github.com/new"
        echo "  2. 创建新仓库（不要初始化README、.gitignore等）"
        echo "  3. 复制仓库的HTTPS或SSH URL"
        echo ""
        echo -n "请输入GitHub仓库URL（例如 https://github.com/用户名/仓库名.git）: "
        read -r remote_url
    fi

    if [ -z "$remote_url" ]; then
        error "必须提供远程仓库URL"
        exit 1
    fi

    info "设置远程仓库 origin 为: $remote_url"
    git remote add origin "$remote_url"

    info "验证远程仓库..."
    if git ls-remote --exit-code origin &> /dev/null; then
        info "远程仓库验证成功"
    else
        warn "无法连接远程仓库，请检查URL和网络连接"
        echo -n "是否继续？(y/N): "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            info "已取消"
            exit 0
        fi
    fi

    # 设置上游分支
    info "推送代码到远程仓库..."
    git branch -M main
    if git push -u origin main; then
        info "✅ 代码已成功推送到GitHub"
    else
        warn "推送失败，请手动执行: git push -u origin main"
    fi
}

# 显示后续步骤
show_next_steps() {
    echo ""
    info "🎉 初始化完成！"
    echo ""
    info "后续步骤:"
    info "1. 确保GitHub仓库设置了GitHub Token"
    info "   - 访问 https://github.com/settings/tokens"
    info "   - 创建具有repo权限的token"
    info "   - 在仓库Settings → Secrets → Actions中添加GITHUB_TOKEN"
    echo ""
    info "2. 测试自动发布流程:"
    info "   ./release.sh v1.0.0-test"
    echo ""
    info "3. 正式发布时:"
    info "   ./release.sh v1.3.2  # 使用实际版本号"
    echo ""
    info "4. 查看GitHub Actions状态:"
    info "   https://github.com/你的用户名/你的仓库名/actions"
    echo ""
    info "5. 下载发布的JAR文件:"
    info "   https://github.com/你的用户名/你的仓库名/releases"
    echo ""
    warn "⚠️  注意: 首次运行GitHub Actions可能需要授权"
    warn "    在仓库Settings → Actions → General中启用Workflow权限"
}

# 主函数
main() {
    check_git

    info "开始初始化Minecraft模组Git仓库..."
    echo ""

    # 初始化仓库
    init_repo

    # 设置远程仓库
    setup_remote "$1"

    # 显示后续步骤
    show_next_steps
}

# 运行主函数
main "$@"