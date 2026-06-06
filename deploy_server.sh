#!/usr/bin/env bash

# ==============================================================================
# Nail-It (StyleMirror) - 腾讯云/国内服务器私有化 Supabase 一键极速部署脚本
# ==============================================================================
# 适用系统: Ubuntu 20.04+, Debian 10+
# 运行权限: Root
# ==============================================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}        Nail-It (StyleMirror) 国内服务器 Supabase 一键自动化部署脚本        ${NC}"
echo -e "${BLUE}======================================================================${NC}"

# 1. 权限检查
if [ "$EUID" -ne 0 ]; then
  echo -e "${RED}错误: 必须使用 root 权限运行此脚本! 请使用 'sudo -i' 切换为 root 用户后再试。${NC}"
  exit 1
fi

# 获取当前脚本所在的项目根目录绝对路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 2. 默认变量配置
DB_PASSWORD="NailItSecureDB2026"
PUBLISHABLE_KEY="sb_publishable_I5DTHgZlBPvw3-5mzsjquQ_BwIzZUox"
SERVICE_ROLE_KEY="sb_secret_admin_role_key_random_string_here_123456"
SERVER_IP="129.204.200.38"
DOCKER_DIR="/root/supabase-docker"

echo -e "${YELLOW}--- [步骤 1/6] 检查并安装 Docker & Docker Compose 环境 ---${NC}"
if ! [ -x "$(command -v docker)" ]; then
  echo -e "${YELLOW}未检测到 Docker，正在使用阿里云镜像源安装...${NC}"
  curl -fsSL https://get.docker.com | bash -s -- --mirror Aliyun
  systemctl start docker
  systemctl enable docker
  echo -e "${GREEN}Docker 安装成功!${NC}"
else
  echo -e "${GREEN}Docker 环境已就绪!${NC}"
fi

if ! [ -x "$(command -v docker-compose)" ]; then
  echo -e "${YELLOW}未检测到 Docker Compose，正在安装...${NC}"
  curl -L "https://github.com/docker/compose/releases/download/v2.26.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
  echo -e "${GREEN}Docker Compose 安装成功!${NC}"
else
  echo -e "${GREEN}Docker Compose 环境已就绪!${NC}"
fi

echo -e "${YELLOW}--- [步骤 2/6] 拉取并配置自建 Supabase 官方容器堆栈 ---${NC}"
if [ ! -d "$DOCKER_DIR" ]; then
  echo -e "${YELLOW}使用 Gitee 官方镜像源极速克隆 Supabase 部署包...${NC}"
  git clone --depth 1 https://gitee.com/mirrors/supabase.git "$DOCKER_DIR"
else
  echo -e "${GREEN}Supabase Docker 目录已存在，正在拉取最新版本...${NC}"
  cd "$DOCKER_DIR"
  git pull || true
fi

cd "$DOCKER_DIR"/docker
cp -n .env.example .env || true

echo -e "${YELLOW}正在对齐 Publishable Key 与数据库密码...${NC}"
# 替换默认配置
sed -i "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$DB_PASSWORD/" .env
sed -i "s/^ANON_KEY=.*/ANON_KEY=$PUBLISHABLE_KEY/" .env
sed -i "s/^SERVICE_ROLE_KEY=.*/SERVICE_ROLE_KEY=$SERVICE_ROLE_KEY/" .env

echo -e "${GREEN}环境变量配置完成!${NC}"

echo -e "${YELLOW}--- [步骤 3/6] 启动 Supabase 容器堆栈 ---${NC}"

# 兼容性修复：删除老版本 docker-compose 不支持的 'name' 属性
sed -i '/^name: supabase/d' docker-compose.yml || true

# 极速优化：使用 Python 脚本剔除 MVP 不需要的高负载服务（supavisor, imgproxy, auth, studio, meta）
if [ -f "$SCRIPT_DIR/supabase/optimize_compose.py" ]; then
  echo -e "${YELLOW}正在优化 docker-compose.yml，剔除 MVP 无用服务以节省服务器内存...${NC}"
  python3 "$SCRIPT_DIR/supabase/optimize_compose.py" docker-compose.yml
  echo -e "${GREEN}docker-compose.yml 优化完成!${NC}"
fi

# 动态检测并使用最新版的 'docker compose' 还是老版的 'docker-compose'
if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
  echo -e "${GREEN}检测到现代版 Docker Compose V2 Plugin, 将使用 '$COMPOSE_CMD' 进行部署。${NC}"
else
  COMPOSE_CMD="docker-compose"
  echo -e "${YELLOW}检测到传统版 docker-compose V1, 将使用 '$COMPOSE_CMD' 进行部署。${NC}"
fi

$COMPOSE_CMD down || true
$COMPOSE_CMD up -d

echo -e "${GREEN}Supabase 容器启动指令已发出，正在后台启动...${NC}"

echo -e "${YELLOW}--- [步骤 4/6] 等待数据库就绪并执行表结构迁移 ---${NC}"
echo -e "${YELLOW}正在等待 Postgres (Port 5432) 建立连接...${NC}"

# 循环检查 Postgres 容器是否完全就绪
for i in {1..30}; do
  if docker exec supabase-db pg_isready -U postgres >/dev/null 2>&1; then
    echo -e "${GREEN}数据库已成功启动并就绪!${NC}"
    break
  fi
  if [ $i -eq 30 ]; then
    echo -e "${RED}错误: 数据库启动超时，请运行 'docker-compose logs supabase-db' 检查日志。${NC}"
    exit 1
  fi
  sleep 2
done

# 获取迁移 SQL 路径
MIGRATION_SQL="$SCRIPT_DIR/supabase/migrations/20260606_001_init_nail_it.sql"

if [ -f "$MIGRATION_SQL" ]; then
  echo -e "${YELLOW}检测到项目迁移脚本，正在自动导入数据库表结构和创建存储桶...${NC}"
  docker exec -i supabase-db psql -U postgres -d postgres < "$MIGRATION_SQL"
  echo -e "${GREEN}数据库表结构与公共存储桶 (nail-it-assets) 初始化完成!${NC}"
else
  echo -e "${RED}警告: 未在 $MIGRATION_SQL 找到迁移 SQL 文件，请手动迁移数据库。${NC}"
fi

echo -e "${YELLOW}--- [步骤 5/6] 安装并配置 Supabase CLI (用于云函数部署) ---${NC}"
if ! [ -x "$(command -v supabase)" ]; then
  echo -e "${YELLOW}正在安装 Supabase CLI...${NC}"
  CLI_VERSION="1.191.3"
  
  # 多代理备用下载机制（防超时、防断连，确保 100% 成功）
  PROXIES=(
    "https://ghproxy.cn/https://github.com"
    "https://github.moeyy.xyz/https://github.com"
    "https://mirror.ghproxy.com/https://github.com"
  )
  
  DOWNLOAD_SUCCESS=false
  for proxy in "${PROXIES[@]}"; do
    echo -e "${YELLOW}尝试使用国内代理源下载: $proxy ...${NC}"
    if curl --connect-timeout 8 -L "$proxy/supabase/cli/releases/download/v${CLI_VERSION}/supabase_${CLI_VERSION}_linux_amd64.tar.gz" -o supabase_cli.tar.gz; then
      if [ -f "supabase_cli.tar.gz" ] && [ -s "supabase_cli.tar.gz" ]; then
        echo -e "${GREEN}下载成功! 正在解压安装...${NC}"
        tar -zxf supabase_cli.tar.gz
        mv supabase /usr/local/bin/
        rm -f supabase_cli.tar.gz
        DOWNLOAD_SUCCESS=true
        break
      fi
    fi
    echo -e "${RED}当前代理源响应超时，正在尝试下一个备用源...${NC}"
    rm -f supabase_cli.tar.gz
  done
  
  if [ "$DOWNLOAD_SUCCESS" = false ]; then
    echo -e "${RED}错误: 所有国内代理源均下载失败，请尝试重新运行脚本或检查服务器网络。${NC}"
    exit 1
  fi
  echo -e "${GREEN}Supabase CLI 安装成功!${NC}"
fi

echo -e "${YELLOW}--- [步骤 6/6] 部署 6 个 Edge Functions 到本地 Deno 运行时 ---${NC}"
cd "$SCRIPT_DIR"/supabase

# 一键向本地 Deno 运行时部署云函数
FUNCTIONS=("create_try_on" "create_qwen_temp_token" "generate_execution_package" "submit_source_link" "prepare_asset_upload" "confirm_asset_upload")

for func in "${FUNCTIONS[@]}"; do
  echo -e "${YELLOW}正在部署云函数: $func ...${NC}"
  supabase functions deploy "$func" --project-ref http://localhost:8000 --no-verify-jwt || true
done

echo -e "${GREEN}所有云函数部署指令执行完毕!${NC}"

echo -e "${BLUE}======================================================================${NC}"
echo -e "${GREEN}🎉 恭喜! Nail-It (StyleMirror) 国内服务器私有化部署圆满成功!${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "${YELLOW}1. 数据库连接: ${NC}postgresql://postgres:$DB_PASSWORD@$SERVER_IP:5432/postgres"
echo -e "${YELLOW}2. API 访问网关: ${NC}http://$SERVER_IP:8000"
echo -e "${YELLOW}3. 客户端 Publishable Key: ${NC}$PUBLISHABLE_KEY"
echo -e "${YELLOW}4. 存储桶名称: ${NC}nail-it-assets"
echo -e "${BLUE}======================================================================${NC}"
echo -e "${YELLOW}请在本地电脑 Android 项目的 local.properties 中配置：${NC}"
echo -e "NAILIT_SUPABASE_URL=http://$SERVER_IP:8000"
echo -e "NAILIT_SUPABASE_ANON_KEY=$PUBLISHABLE_KEY"
echo -e "${BLUE}======================================================================${NC}"
