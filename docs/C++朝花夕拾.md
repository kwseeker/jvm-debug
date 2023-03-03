# C++ 朝花夕拾

大学里选修C++以及做些玩具项目后基本再没用过C++, 语法也都还给老师了，看JVM hotspot 源码(基本都是C++)有些费劲。

这里将遇到的遗忘的知识点再记录下。

+ 面向对象

  + 虚方法、纯虚方法

  + 运算符重载 / 函数重载

    ```cpp
    //C++ new 是个运算符，也可以被重载，从而改变内存的分配方式
    //JVM ostream_init() 
    defaultStream::instance = new(ResourceObj::C_HEAP, mtInternal) defaultStream();
    //new 重载运算符声明
    void* operator new(size_t size, allocation_type type, MEMFLAGS flags) throw();
    ```

  + 类访问修饰符

    类成员可以被定义为 public、private 或 protected。默认情况下是定义为 private。

    