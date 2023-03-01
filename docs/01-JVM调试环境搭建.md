# JVM调试环境搭建

**本地PC环境**：

+ Linux Mint 19 (Ubuntu 18 的内核)
+ Clion2022.2
+ OpenJDK8u40

**搭建步骤**：

1. **源码下载**

   **从 mercurial 仓库克隆**：

   JVM源码（其实是JDK源码的一部分）在 https://openjdk.org/  `Source code` -> `Mercurial`下面， 有多个版本可以选择。其中最新的是jdk8u和jdk8u41; OpenJDK 8u是Java 8的稳定版本，而OpenJDK 8u41则是Java 8的一个更新版本，相当于OpenJDK 8u的一个子版本。这里选择[jdk8u41](https://hg.openjdk.org/jdk8u/jdk8u41)，页面左侧可以以压缩包格式下载。解压后可以发现里面没有源码，根据 README / README-builds.html 文件和 get_source.sh 文件下载完整代码。

   ```shell
   sudo apt-get install mercurial
   # 本地尝试不知为何报 abort: HTTP Error 403: Forbidden
   hg clone http://hg.openjdk.java.net/jdk8u/jdk8u41 openjdk8u41
   cd openjdk8u41 && sh ./get_source.sh
   ```

   **下载RI版本源码**：

   也可以选择直接下载RI版本源码 https://jdk.java.net/java-se-ri，左边可以选择 Reference Implementations版本。

   ```shell
   wget https://download.java.net/openjdk/jdk8u42/ri/openjdk-8u42-src-b03-14_jul_2022.zip
   ```

   安装前一个版本JDK（原因参考 README-builds.html）

   ```shell
   wget https://repo.huaweicloud.com/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz
   tar -zxf jdk-7u80-linux-x64.tar.gz		# jdk1.7.0_80
   # 将 default-java 软连接指向 jdk1.7.0_80
   sudo ln -s default-java java-1.7.0
   # 检查版本是否切换成功
   java -version
   ```

   也可以选择网上别人搭建JVM环境成功的案例所使用的版本，跟别人用的工具版本都保持一致会更稳妥，否则很容易碰到一堆版本不兼容导致的编译问题，而且一旦出现这种问题很难找到应该替换哪个版本。

2. **源码编译**

   配置编译环境：

   ```shell
   # 执行configure脚本，看看缺少什么依赖项，根据错误提示安装即可，然后重复执行直到提示成功
   # 参数说明
   # --disable-warnings-as-errors 禁止把warning 当成error
   # --with-debug-level=slowdebug 设置编译级别为slowdebug，将会输出较多的调试信息
   # --enable-debug-symbols 启用调试符号，将会生成调试信息文件
   # --disable-zip-debug-info 禁用调试信息压缩，否则，调试信息默认会被压缩成"libjvm.diz"文件，调试时只能看到汇编代码，不能跟进源码
   ./configure --with-target-bits=64 --disable-warnings-as-errors --with-debug-level=slowdebug --enable-debug-symbols --disable-zip-debug-info
   
   # 本机编译提示缺乏下面工具，依次安装
   sudo apt-get install libxext-dev libxrender-dev libxtst-dev libxt-dev
   sudo apt-get install libcups2-dev
   sudo apt-get install libfreetype6-dev
   sudo apt-get install libasound2-dev
   
   # 配置成功后会显示下面信息
   ====================================================
   A new configuration has been successfully created in
   ......
   ```

   编译：

   ```shell
   # vi ./hotspot/make/linux/makefiles/gcc.make，然后找到 WARNINGS_ARE_ERRORS =，将这行注释掉；不然一些警告也会当作错误
   # 也可以在前面 configure 中添加参数 --disable-warnings-as-errors
   # 安装 compiledb, 用户生成 compile_commands.json
   pip3 install compiledb
   # 编译
   # compiledb make all ZIP_DEBUGINFO_FILES=0 ALLOW_DOWNLOADS=true #编译报错
   make all ZIP_DEBUGINFO_FILES=0 ALLOW_DOWNLOADS=true
   # 编译报错
   # This OS is not supported: Linux lee-pc 5.0.0-32-generic #34~18.04.2-Ubuntu
   # 修改 hotspot/make/linux/Makefile
   # SUPPORTED_OS_VERSION = 2.4% 2.5% 2.6% 3% 改为
   SUPPORTED_OS_VERSION = 2.4% 2.5% 2.6% 3% 4% 5%
   
   # 继续编译还是报错，就调低make gcc g++ 的版本
   
   # 测试，编译成功后的内容在 build 文件夹下
   ./build/linux-x86_64-normal-server-release/jdk/bin/java -version
   # 确保"libjvm.debuginfo"文件存在，否则调试时将不能跟进源码
   ls ./build/linux-x86_64-normal-server-release/jdk/lib/amd64/server/
   ```

   降低make / gcc / g++ 版本：

   ```shell
   # 去https://ftp.gnu.org/gnu/make/下载make-3.81
   ./configure --prefix=/usr/local/make-3.81
   # 注释glob/glob.c　
   # #if !defined __alloca && !defined __GNU_LIBRARY__\
   # #endif
   make 
   sudo make install
   sudo apt install gcc-4.8 g++-4.8
   # 在 /opt/gnu/bin 下面创建 make gcc-4.8 g++-4.8 的软连接
   # /opt/gnu/bin 加入PATH
   # GNU 4.8 临时需要降低版本，用后注释掉就行
   export PATH=/opt/gnu/bin:$PATH
   source ~/.zshrc
   gcc -v
   g++ -v
   make -v
   ```

3. **源码调试**

   尝试用工具生成compile_commands.json发现都不靠谱，还是用添加CMakeLists.txt的方式。找到了一个可用的配置文件 https://github.com/zhangyongheng/jdkbuild/blob/master/openjdk8/CMakeLists.txt, 略加修改后，就可以使用了。
   
   **测试 java -version **:
   
   在 CLion 中添加一个测试用的 CMake Application:
   
   Name: java-version
   Target: openjdk8
   Executable: /home/lee/mywork/java/openjdk/openjdk8u40/build/linux-x86_64-normal-server-slowdebug/jdk/bin/java
   Program arguments: -version
   
   > 注意将 Before launch 中的 build 删掉（这个build是用CMake编译的），代码已经编译好了只要不改代码就不需要重新编译。
   
   执行debug测试，发现报 SIGSEGV 信号异常信息。SIGSEGV是指一个进程执行了一个无效的内存引用或发生了段错误。
   
   看源码发现是因为 get_cpu_info_stub 方法指针为 NULL；可以将这个方法调用注释掉然后重新编译或者在GDB中忽略这个信号（在gdb命令行中输入 handle SIGSEGV nostop noprint pass）。
   
   ```
   static get_cpu_info_stub_t get_cpu_info_stub = NULL;
   
   void VM_Version::get_cpu_info_wrapper() {
     get_cpu_info_stub(&_cpuid_info);
   }
   ```
   
   **测试 Hello  World 程序**：
   
   在 CLion 中再添加一个 CMake Application:
   
   Name: hello-jvm
   Program arguments: top.kwseeker.jvm.debug.HelloJVM
   Working directory: /home/lee/mywork/java/jvm-debug/target/classes
   
   执行：
   
   ```
   /home/lee/mywork/java/openjdk/openjdk8u40/build/linux-x86_64-normal-server-slowdebug/jdk/bin/java top.kwseeker.jvm.debug.HelloJVM
   Hello JVM!
   
   Process finished with exit code 0
   ```
   
   

**参考**：

+ [Ubuntu18.04编译OpenJdk8](https://blog.csdn.net/dghgfhk/article/details/103356051)
+ [2020年，在 Linux 下动手编译 OpenJDK 8](https://risehere.net/posts/building-openjdk/)
+ [JVM 源码分析（二）：搭建 JDK 8 源码调试环境](https://www.cnblogs.com/yonghengzh/p/14266121.html) 

