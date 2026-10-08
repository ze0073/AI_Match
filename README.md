# AI Match 智能人才匹配系统

面向求职者、企业 HR 和管理员的人才匹配系统，包含简历管理、岗位管理、AI 匹配、能力图谱、面试准备和系统管理等功能。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Pinia、ECharts。
- 后端：Java 17、Spring Boot 3.2.5、Spring Security、JWT。
- AI：OpenAI 兼容接口，默认使用 DeepSeek。
- 数据：本地 JSON 文件，运行时生成在后端的 `data/` 目录。

## 项目结构

```text
ai-match-frontend/   前端源码
ai-match-server/     后端源码与测试
deploy/             部署脚本和配置示例
```

## 本地运行

需要安装 JDK 17、Maven 和 Node.js 18 或更高版本。

首次克隆后，在项目根目录使用 PowerShell 复制后端配置示例：

```powershell
Copy-Item ai-match-server/src/main/resources/application.example.yml ai-match-server/src/main/resources/application.yml
$env:JWT_SECRET = 'replace-with-a-random-secret-at-least-32-bytes-long'
$env:OPENAI_API_KEY = 'your-api-key'
$env:AI_BASE_URL = 'https://api.deepseek.com'
$env:AI_MODEL = 'deepseek-chat'
cd ai-match-server
mvn spring-boot:run
```

后端默认端口为 `8080`。首次启动时自动创建管理员账号 `ADMIN`，初始密码为 `admin123`。

另开终端启动前端：

```powershell
cd ai-match-frontend
npm ci
npm run dev
```

打开 `http://localhost:5173`。开发服务器将 `/api` 请求代理到本地后端。

## 构建与测试

```powershell
# 前端构建
cd ai-match-frontend
npm run build

# 在另一个终端进入后端目录
cd ai-match-server
mvn test
mvn package
```

后端安装包生成于 `ai-match-server/target/`。部署时可将生成的 JAR 放到 `deploy/`，将该目录的 `application.example.yml` 复制为 `application.yml`，设置相同的环境变量后运行 `start.sh`。

后端 `src/main/resources/static/` 包含现有前端静态资源；更新前端后可将 `ai-match-frontend/dist/` 的内容复制到该目录，再打包后端。

## 配置与上传范围

真实 API 密钥和本机配置保留在本地。仓库提供从环境变量读取 `JWT_SECRET`、`OPENAI_API_KEY`、`AI_BASE_URL`、`AI_MODEL` 和 `SERVER_PORT` 的配置示例。

依赖目录、构建安装包、压缩包、运行日志、本地用户与简历数据、IDE 配置和生成的 `outputs/` 目录通过 `.gitignore` 排除。比赛文档、演示 PPT 和视频保留在本地 `docs/` 目录，因可能包含个人信息，不上传到公共仓库。
