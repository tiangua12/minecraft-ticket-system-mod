#!/bin/bash

# Minecraft模组自动发布脚本
# 使用方法: ./release.sh [标签名]
# 例如: ./release.sh v1.0.0
# 如果不提供标签名，将使用gradle.properties中的版本号自动生成

set -e  # 遇到错误时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 输出带颜色的消息
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查必需的命令
check_commands() {
    local missing=0
    for cmd in git grep cut; do
        if ! command -v $cmd &> /dev/null; then
            error "命令 '$cmd' 未找到，请安装后重试"
            missing=1
        fi
    done

    if [ $missing -eq 1 ]; then
        exit 1
    fi
}

# 检查git仓库状态
check_git() {
    if [ ! -d .git ]; then
        error "当前目录不是git仓库"
        echo "请先初始化git仓库:"
        echo "  git init"
        echo "  git add ."
        echo "  git commit -m 'Initial commit'"
        echo "  git remote add origin <你的GitHub仓库URL>"
        exit 1
    fi

    # 检查是否有未提交的更改
    if [ -n "$(git status --porcelain)" ]; then
        warn "检测到未提交的更改"
        echo -n "是否继续？(y/N): "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            info "已取消"
            exit 0
        fi
    fi

    # 检查远程仓库是否设置
    if ! git remote get-url origin &> /dev/null; then
        error "未设置远程仓库 (origin)"
        echo "请使用以下命令设置:"
        echo "  git remote add origin <你的GitHub仓库URL>"
        exit 1
    fi
}

# 从gradle.properties提取版本号
extract_version() {
    if [ -f "gradle.properties" ]; then
        VERSION=$(grep 'mod_version' gradle.properties | cut -d'=' -f2)
        echo "$VERSION"
    else
        error "gradle.properties文件不存在"
        exit 1
    fi
}

# 主函数
main() {
    check_commands

    info "检查git仓库状态..."
    check_git

    # 确定标签名
    local tag_name=""
    if [ -n "$1" ]; then
        tag_name="$1"
        info "使用指定的标签名: $tag_name"
    else
        local version=$(extract_version)
        tag_name="v${version}"
        info "使用自动生成的标签名: $tag_name (基于版本号 $version)"
    fi

    # 验证标签格式（可选，但推荐）
    if [[ ! "$tag_name" =~ ^v[0-9]+\.[0-9]+\.[0-9]+ ]]; then
        warn "标签名 '$tag_name' 不符合常见版本格式 (推荐: v1.0.0)"
        echo -n "是否继续？(y/N): "
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            info "已取消"
            exit 0
        fi
    fi

    # 检查标签是否已存在
    if git tag -l | grep -q "^$tag_name$"; then
        error "标签 '$tag_name' 已存在"
        echo "请使用不同的标签名或删除现有标签:"
        echo "  git tag -d $tag_name"
        exit 1
    fi

    # 提示用户确认
    echo ""
    info "发布准备完成:"
    echo "  - 标签名: $tag_name"
    echo "  - 远程仓库: $(git remote get-url origin)"
    echo ""
    echo "这将执行以下操作:"
    echo "  1. 创建并推送标签 $tag_name"
    echo "  2. 触发GitHub Actions构建和发布"
    echo "  3. 在GitHub Releases中创建新版本"
    echo ""
    echo -n "是否继续？(y/N): "
    read -r response

    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        info "已取消"
        exit 0
    fi

    # 创建标签
    info "创建标签 $tag_name..."
    git tag -a "$tag_name" -m "Release $tag_name"

    # 推送标签到远程仓库
    info "推送标签到远程仓库..."
    git push origin "$tag_name"

    echo ""
    info "✅ 标签已成功推送!"
    info "GitHub Actions将自动开始构建和发布流程"
    info ""
    info "你可以在这里查看构建进度:"
    info "  https://github.com/$(git remote get-url origin | sed -n 's/.*github.com[:/]\(.*\)\.git/\1/p')/actions"
    info ""
    info "发布完成后，可以在这里下载编译好的JAR文件:"
    info "  https://github.com/$(git remote get-url origin | sed -n 's/.*github.com[:/]\(.*\)\.git/\1/p')/releases"
    info ""
    info "💡 提示: 首次发布可能需要几分钟完成构建"
}

# 运行主函数
main "$@"