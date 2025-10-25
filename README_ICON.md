# PLC Manager 图标配置说明

## 已完成的工作

1. 已在项目中生成了PNG格式的图标：`src/main/resources/app-icon.png`
2. 已修改`pom.xml`配置文件，在launch4j插件中设置了图标路径：`src/main/resources/app-icon.ico`
3. 已创建转换说明脚本：`convert_png_to_ico.bat`

## 如何完成ICO图标配置

由于Windows命令行环境中没有内置的PNG到ICO转换工具，您需要按照以下步骤手动完成转换：

### 方法1：使用Windows自带的画图工具
1. 在Windows资源管理器中找到：`f:\codebuddy\iot_ap\src\main\resources\app-icon.png`
2. 右键点击该文件，选择"编辑"（这会在画图工具中打开）
3. 在画图工具中，点击"文件" -> "另存为" -> "浏览"
4. 在保存对话框中，设置保存类型为"ICO文件(*.ico)"
5. 文件名为：`app-icon.ico`
6. 确保保存位置为：`f:\codebuddy\iot_ap\src\main\resources\`
7. 点击"保存"

### 方法2：使用在线转换工具
1. 访问在线PNG到ICO转换网站（如：https://icoconvert.com/）
2. 上传`app-icon.png`文件
3. 转换并下载为`app-icon.ico`
4. 将下载的`app-icon.ico`文件复制到`f:\codebuddy\iot_ap\src\main\resources\`目录

### 方法3：使用第三方软件
- 使用图像编辑软件如GIMP、IrfanView等打开PNG文件
- 导出为ICO格式并保存到指定位置

## 验证图标配置

完成转换并将ICO文件放置在正确位置后：

1. 运行构建脚本：`f:\codebuddy\iot_ap\build_exe.bat`
2. 构建成功后，检查`target`文件夹中的`PLCManager.exe`文件图标
3. 在Windows资源管理器中查看EXE文件，确认图标是否正确显示

## 故障排除

如果构建失败或图标未显示：

1. 确保ICO文件格式正确（标准Windows ICO格式）
2. 检查文件权限，确保Maven可以读取图标文件
3. 尝试使用不同尺寸的图标（建议使用32x32或64x64像素）
4. 如果仍然有问题，可以临时移除`pom.xml`中的图标配置，先确保构建成功

---

图标配置完成后，生成的PLC Manager应用程序将会显示自定义图标，提升应用的专业外观！