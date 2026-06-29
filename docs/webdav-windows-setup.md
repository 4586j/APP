# WebDAV 网络磁盘 — Windows 客户端配置

ERP 网络磁盘通过 WebDAV 协议提供。Windows 资源管理器映射网络驱动器后，可像本地磁盘一样浏览、双击用 Office/WPS/记事本打开，Ctrl+S 直接保存回服务器，全程受 ERP 部门权限管控。

> 服务器端口默认 **8081**（见 `application.yaml` 的 `server.port`）。下文以 `8081` 为例，若改过端口请同步替换。

## 1. 映射网络驱动器

资源管理器 → 此电脑 → 映射网络驱动器 → 选盘符（如 `Z`）→ 文件夹填：

```
http://<服务器IP>:8081/webdav
```

勾选「使用其他凭据」→ 输入 ERP 账号密码（与 ERP 网页登录同一账号）。

> 局域网 HTTP 即可。若部署了 HTTPS，地址改 `https://<服务器IP>/webdav`，且可跳过第 2 步注册表修改。

## 2. 注册表配置（HTTP 下必需）

Windows WebClient 默认不对 HTTP 发送 Basic Auth，且限制单文件 50MB。新建 `webdav-fix.reg`，内容如下：

```reg
Windows Registry Editor Version 5.00

[HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\WebClient\Parameters]
"BasicAuthLevel"=dword:00000002
"FileSizeLimitInBytes"=dword:ffffffff
```

- `BasicAuthLevel=2`：允许在 HTTP（非 SSL）连接上发送 Basic Auth。
- `FileSizeLimitInBytes=0xffffffff`：解除 50MB 单文件大小限制（约 4GB 上限）。

双击导入注册表，然后以管理员身份重启 WebClient 服务（或重启电脑）：

```cmd
net stop WebClient
net start WebClient
```

## 3. 路径结构

映射后根目录列出当前用户可见的部门（本部门 + 下级部门 + 被共享的部门）：

```
Z:\
├── 销售部\          （本部门，可读写、可新建）
│   ├── 报表\
│   │   └── 数据.xlsx
│   └── 周报.docx
├── 华东销售部\      （下级部门，只读）
└── 财务部\          （被共享给本部门，按共享权限读写）
```

- **本部门**：可创建文件夹、上传、编辑、删除、重命名、移动。
- **下级部门**：只读（`canCreate` 仅本部门）。
- **共享部门**：按文件级/文件夹级共享权限，可见可改。
- **跨部门移动**：被拒绝（源与目标必须同部门）。

## 4. 编辑保存流程（独占锁）

1. 双击文件 → Windows 用关联程序（Word/WPS/Notepad）打开，后台发起 `LOCK` 请求。
2. 服务器校验写权限后下发独占锁（30 分钟有效）。
3. 编辑完成 `Ctrl+S` → 客户端 `PUT` 覆盖文件内容 → 释放锁。
4. 他人此时双击同一文件：服务器返回 `423 Locked`，客户端提示只读/被占用。

## 5. 验收清单

部署后逐项验证：

- [ ] 映射成功，Z 盘出现
- [ ] 根目录列出本部门 + 下级部门 + 共享部门
- [ ] 双击 `.docx` 用 Word 打开，`Ctrl+S` 保存无报错
- [ ] 关闭后他人打开看到新内容
- [ ] 并发：A 打开编辑时，B 双击同一文件收到锁定/只读提示
- [ ] 跨部门目录无法新建文件（权限拒绝）
- [ ] 下级部门目录只读
- [ ] 删除文件后资源管理器刷新消失
- [ ] 重命名/移动在本部门内正常

## 6. 排查

| 现象 | 排查 |
|---|---|
| 映射时报「网络名不再可用」 | 检查 `webdav-fix.reg` 是否导入并重启 WebClient；确认 `BasicAuthLevel=2` |
| 能连上但提示需凭据循环 | 账号密码错误，或用户被禁用；用 ERP 网页登录验证 |
| 大文件上传失败 | 确认 `FileSizeLimitInBytes` 已设为 `ffffffff`；检查 `spring.servlet.multipart.max-file-size`（默认 1024MB） |
| 中文文件名乱码 | 确保数据库与连接 `characterEncoding=utf8`（已配置） |
| 401 频繁 | 锁定 TTL 30 分钟，长时间空闲后重新打开即可 |