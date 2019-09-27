### 1. 基础准备

clone下来之后导入android studio并做以下改动

由于demo需要使用华为云用户名密码clone 下来之后自行创建config.gradle 文件此文件为igonore掉的 不用担心会泄漏只会存在本地

config.gradle里面内容格式如下,

```
ext.uname = ""
ext.dname = ""
ext.pwd = ""
```
在引号中填写用户名和密码dname一般和uname保持一致,dname的获取可以在华为云官网进入我的凭证--IAM用户名即是

### 2. 启动已经部署的在线服务

![在线服务](https://github.com/zxzxzxygithub/hwmodelartdemo/blob/master/1540568x9dv8nrwukbihkn.png)

2.1)将MainActivity中的region替换为你所在的region，region取值可参考：https://developer.huaweicloud.com/endpoint

2.2)将MainActivity中的url替换为你的在线服务的url

2.3)将HWMLClientToken中的requestmlTokenServiceByFile方法中的下面这行
```requestBody.addFormDataPart("images", filename, body);```
的images修改为你的调用参数（有可能不是images，比如有些是input_img，如果参数不一致是不会调用成功的）

### 3. 测试在线服务

运行代码在模拟器或者手机上面点拍照或者从相册选择一张图片进行识别

界面截图为

![界面截图](https://github.com/zxzxzxygithub/hwmodelartdemo/blob/master/1543248gpg8yeldm2kw1sf.png)



