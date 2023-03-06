# JVM执行流程

## OpenJDK源码目录结构

```
openjdk
├── build
│   └── linux-x86_64-normal-server-slowdebug
├── cmake-build-debug
├── common
│   ├── autoconf
│   ├── bin
│   ├── nb_native
│   └── src
├── configure
├── corba						不流行的多语言、分布式通讯接口
├── get_source.sh
├── hotspot						虚拟机的一种实现
	├─agent                            	Serviceability Agent的客户端实现
    ├─make                             	用来build出HotSpot的各种配置文件
    ├─src                              	HotSpot VM的源代码
    │  ├─cpu                            CPU相关代码（汇编器、模板解释器、ad文件、部分runtime函数在这里实现）
    │  ├─os                             操作系相关代码
    │  ├─os_cpu                         操作系统+CPU的组合相关的代码
    │  └─share                          平台无关的共通代码
    │      ├─tools                        	工具
    │      │  ├─hsdis                      		反汇编插件
    │      │  ├─IdealGraphVisualizer       		将server编译器的中间代码可视化的工具
    │      │  ├─launcher                   		启动程序“java”
    │      │  ├─LogCompilation             		将-XX:+LogCompilation输出的日志（hotspot.log）整理成更容易阅读的格式的工具
    │      │  └─ProjectCreator             		生成Visual Studio的project文件的工具
    │      └─vm                           	HotSpot VM的核心代码
    │          ├─adlc                       	平台描述文件（上面的cpu或os_cpu里的*.ad文件）的编译器
    │          ├─asm                        	汇编器接口
    │          ├─c1                         	client编译器（又称“C1”）
    │          ├─ci                         	动态编译器的公共服务/从动态编译器到VM的接口
    │          ├─classfile                  	类文件的处理（包括类加载和系统符号表等）
    │          ├─code                       	动态生成的代码的管理
    │          ├─compiler                   	从VM调用动态编译器的接口
    │          ├─gc_implementation          	GC的实现
    │          │  ├─concurrentMarkSweep      		Concurrent Mark Sweep GC的实现
    │          │  ├─g1                       		Garbage-First GC的实现（不使用老的分代式GC框架）
    │          │  ├─parallelScavenge         		ParallelScavenge GC的实现（server VM默认，不使用老的分代式GC框架）
    │          │  ├─parNew                   		ParNew GC的实现
    │          │  └─shared                   		GC的共通实现
    │          ├─gc_interface               	GC的接口
    │          ├─interpreter                	解释器，包括“模板解释器”（官方版在用）和“C++解释器”（官方版不在用）
    │          ├─libadt                     	一些抽象数据结构
    │          ├─memory                     	内存管理相关（老的分代式GC框架也在这里）
    │          ├─oops                       	HotSpot VM的对象系统的实现
    │          ├─opto                       	server编译器（又称“C2”或“Opto”）
    │          ├─prims                      	HotSpot VM的对外接口，包括部分标准库的native部分和JVMTI实现
    │          ├─runtime                    	运行时支持库（包括线程管理、编译器调度、锁、反射等）
    │          ├─services                   	主要是用来支持JMX之类的管理功能的接口
    │          ├─shark                      	基于LLVM的JIT编译器（官方版里没有使用）
    │          └─utilities                  	一些基本的工具类
    └─test                             	单元测试
├── jaxp						Java API for XML Processing ，解析与较验xml文件，支持DOM\SAX\StAX
├── jaxws						XML Web Services 的 Java API
├── jdk							Java开发工具包
│   ├── ASSEMBLY_EXCEPTION
│   ├── LICENSE
│   ├── make
│   ├── README
│   ├── src
		├── aix
        │   ├── classes
        │   ├── lib
        │   ├── native
        │   └── porting
        ├── bsd
        │   └── doc
        ├── linux
        │   └── doc
        ├── macosx
        │   ├── bin
        │   ├── bundle
        │   ├── classes
        │   ├── javavm
        │   ├── lib
        │   └── native
        ├── share
        │   ├── back
        │   ├── bin
        │   ├── classes
        │   ├── demo
        │   ├── doc
        │   ├── instrument
        │   ├── javavm
        │   ├── lib
        │   ├── native
        │   ├── npt
        │   ├── sample
        │   └── transport
        ├── solaris
        │   ├── back
        │   ├── bin
        │   ├── classes
        │   ├── demo
        │   ├── doc
        │   ├── instrument
        │   ├── javavm
        │   ├── lib
        │   ├── native
        │   ├── npt
        │   ├── sample
        │   └── transport
        └── windows
            ├── back
            ├── bin
            ├── classes
            ├── demo
            ├── instrument
            ├── javavm
            ├── lib
            ├── native
            ├── npt
            ├── resource
            └── transport
│   ├── test
│   └── THIRD_PARTY_README
├── langtools					Java 语言工具, 包含 javac、javap 等实用程序的源码
├── LICENSE
├── make
├── Makefile
├── nashorn						JVM 上的 JavaScript 运行时，基于 JSR-223 协议，Java 开发者可在 Java 程序中嵌入 JavaScript 代码
├── README
├── README-builds.html
├── test
└── THIRD_PARTY_README
```



## JVM工作流程

参考 jvm-workflow.drawio, 这里只是对流程图的补充。

JVM 是 C/C++ 编写的入口是 jdk/src/share/bin/main.c 的 main() 函数，JLI_Launch() 是main()中调用的第一个函数也是惟一一个函数，是Java 虚拟机的启动函数。

主要步骤包括：

1. 解析命令行参数，包括 Java 应用程序的参数和 JVM 配置参数。

2. 初始化启动器，主要就是设置下日志开关 `_launcher_debug`，通过 JLDEBUG_ENV_ENTRY 环境变量开启，这个应该有用，调试时添加下这个环境变量。

   ```
   _JAVA_LAUNCHER_DEBUG=true
   ```

3. 初始化 JVM。首先会根据指定的 JVM 类路径查找 `libjvm.so` 库，并加载该库。然后，通过 `JNI_CreateJavaVM()` 函数创建一个 Java 虚拟机实例，并设置 JVM 的启动参数。

4. 加载 Java 应用程序类。`JLI_Launch()` 函数会根据 Java 应用程序的类路径和主类名加载 Java 应用程序的主类。如果找不到主类，则会抛出 `java/lang/NoClassDefFoundError` 异常。

5. 启动 Java 应用程序。`JLI_Launch()` 函数会调用 Java 应用程序的 `main()` 函数，并将命令行参数传递给 `main()` 函数。

6. 退出 JVM。Java 应用程序执行完成后，`JLI_Launch()` 函数会销毁 Java 虚拟机实例，并释放相关资源。

### 命令行参数解析

```C++
int argc, char ** argv,                 //main参数，包括 java命令、-options class args...， java [-options] class [args...]
int jargc, const char** jargv,          //Java参数, 指 args...
int appclassc, const char** appclassv,  //应用 classpath，通过 -cp 或 -classpath 指定
const char* fullversion,                //JDK完整版本信息
const char* dotversion,                 //JDK简短版本信息，如：1.8
const char* pname,                      //应用名（命令名），如：java、jmap等
const char* lname,                      //启动器名称，JVM的名称么？
jboolean javaargs,                      //是否附带java参数（即args...是否为空）
jboolean cpwildcard,                    //用于指示是否启用 classpath 通配符扩展
jboolean javaw,                         //是否是窗口程序
jint ergo
```

> 语法补漏：
>
> `__builtin_va_start` is a built-in function in the C programming language that is used to initialize a `va_list` object, which is a list of arguments whose number and types are not known at compile time.
>
> The `va_list` object is typically used in functions that accept a variable number of arguments.
>
> 总结: `va_list` 就是类似Java Object 或 Go interface{} 的对象，可以接收任何类型的参数，而且还可以接收不定数量的参数。



## 参考资料

+ [【JVM源码解析】虚拟机解释执行Java方法（上）](https://segmentfault.com/a/1190000041061285)
+ [【JVM源码解析】模板解释器解释执行Java字节码指令（上）](https://segmentfault.com/a/1190000041015395)
+ [JVM源码解读：Java方法main在虚拟机上解释执行](https://segmentfault.com/a/1190000040944497)