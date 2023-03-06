# CAS源码实现

举个最常见的例子：使用CAS修改服务状态。

参考DEMO: CAS.java。

代码编译好后，配置以 MODE_CLASS 模式在编译好的JVM源码中运行。

```java
private boolean updateStateField(int expected, int target) throws NoSuchFieldException {
    Unsafe unsafe = UnsafeOperator.getUnsafe();
    Field field = Server.class.getDeclaredField("state");
    long stateOffset = unsafe.objectFieldOffset(field);
    return unsafe.compareAndSwapInt(this, stateOffset, expected, target);
}
```

对应的字节码

```java
private updateStateField(II)Z throws java/lang/NoSuchFieldException 
   L0
    LINENUMBER 35 L0
    INVOKESTATIC top/kwseeker/jvm/debug/basic/UnsafeOperator.getUnsafe ()Lsun/misc/Unsafe;
    ASTORE 3
   L1
    LINENUMBER 36 L1
    LDC Ltop/kwseeker/jvm/debug/basic/CAS$Server;.class
    LDC "state"
    INVOKEVIRTUAL java/lang/Class.getDeclaredField (Ljava/lang/String;)Ljava/lang/reflect/Field;
    ASTORE 4
   L2
    LINENUMBER 37 L2
    ALOAD 3
    ALOAD 4
    INVOKEVIRTUAL sun/misc/Unsafe.objectFieldOffset (Ljava/lang/reflect/Field;)J
    LSTORE 5
   L3
    LINENUMBER 38 L3
    ALOAD 3
    ALOAD 0
    LLOAD 5
    ILOAD 1
    ILOAD 2
    INVOKEVIRTUAL sun/misc/Unsafe.compareAndSwapInt (Ljava/lang/Object;JII)Z
    IRETURN
   L4
    LOCALVARIABLE this Ltop/kwseeker/jvm/debug/basic/CAS$Server; L0 L4 0
    LOCALVARIABLE expected I L0 L4 1
    LOCALVARIABLE target I L0 L4 2
    LOCALVARIABLE unsafe Lsun/misc/Unsafe; L1 L4 3
    LOCALVARIABLE field Ljava/lang/reflect/Field; L2 L4 4
    LOCALVARIABLE stateOffset J L3 L4 5
    MAXSTACK = 6
    MAXLOCALS = 7
```

`INVOKEVIRTUAL sun/misc/Unsafe.compareAndSwapInt (Ljava/lang/Object;JII)Z` 这行字节码指令执行流程有些复杂，参考“参考文档”，包括栈帧创建、方法查找、方法调用等等，由于是汇编和C++混编不方便调试。

最终调用到：

```cpp
//unsafe.cpp
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x))
  UnsafeWrapper("Unsafe_CompareAndSwapInt");
  oop p = JNIHandles::resolve(obj);
  jint* addr = (jint *) index_oop_from_field_offset_long(p, offset);
  return (jint)(Atomic::cmpxchg(x, addr, e)) == e;
UNSAFE_END    

inline jint     Atomic::cmpxchg    (jint     exchange_value, volatile jint*     dest, jint     compare_value) {
  //是否是多处理器
  int mp = os::is_MP();
  //volatile C/C++中表示变量是易变的，禁止编译器对其优化，并且要求每个变量赋值时，需要显式从寄存器%eax拷贝，volatile变量间编译器能够保证不交换执行顺序
  //LOCK_IF_MP：如果是多处理器前面加上lock前缀，想当于加了内存屏障，cmpxchgl本身并不是原子指令
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  return exchange_value;
}

// Adding a lock prefix to an instruction on MP machine
// lock指令在执行后面指令的时候锁定一个北桥信号，锁定北桥信号比锁定总线轻量一些
#define LOCK_IF_MP(mp) "cmp $0, " #mp "; je 1f; lock; 1: "
```

