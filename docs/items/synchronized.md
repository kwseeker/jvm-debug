# synchronized 实现原理

参考DEMO: SynchronizedDemo.java。

代码编译好后，配置以 MODE_CLASS 模式在编译好的JVM源码中运行。

```java
public class SynchronizedDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) {
        int i = 0;
        synchronized (lock) {	//12行
            i++;
        }
        System.out.println(i);
    }
}

//synchronized代码块对应的字节码
   L5
    LINENUMBER 12 L5
    //从常量池获取静态字段lock，并压入操作数栈
    //javap反编译的是 getstatic #2, 下面是jclasslib插件反编译的字节码直接将#2替换了
    GETSTATIC top/kwseeker/jvm/debug/basic/SynchronizedDemo.lock : Ljava/lang/Object;
	//复制栈顶数据并入栈
    DUP
    //astore_2 将栈顶引用类型值存入局部变量2
    ASTORE 2
    //获取对象监视器并进入代码块
    MONITORENTER
   L0
    LINENUMBER 13 L0
    IINC 1 1
   L6
    LINENUMBER 14 L6
    //从局部变量2中装载引用类型值
    ALOAD 2
    //释放并退出对象监视器
    MONITOREXIT
   L1
```

然后分析`monitorenter`指令的执行：

TODO：

发现过程没有想象中的简单，源码调试由于汇编和C++混编导致堆栈信息不连续思路很容易断，同时还存在很多操作系统、汇编的知识盲区，且没有找到任何一个资料有详细讲解是怎么从main方法一步步调用到 monitorenter 指令，以及monitorenter monitorexit 指令又是怎么在JVM中处理的。

找到的资料基本都是复制粘贴出来的一堆概念：讲synchronized锁状态存储在对象头中，会根据竞争强度升级锁状态：无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁，但缺乏JVM源码级别的讲解。

找到一篇文章还有些源码的分析，临时参考：[jdk源码剖析二: 对象内存布局、synchronized终极原理](https://www.cnblogs.com/dennyzhangdd/p/6734638.html)

