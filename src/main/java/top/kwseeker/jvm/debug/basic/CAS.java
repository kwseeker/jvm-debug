package top.kwseeker.jvm.debug.basic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 模拟使用CAS修改服务状态，中间件源码中很常见的场景
 */
public class CAS {

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.start();
        server.stop();
    }

    static class Server {
        private static final int STARTED = 1;
        private static final int CLOSED = 0;
        //state: 1 Server开启 0 Server关闭, 实际可能有多种状态不要纠结
        private volatile int state;

        public void start() throws Exception {
            boolean ret = updateStateField(CLOSED, STARTED);
            System.out.println("start " + (ret ? "success" : "failed"));
        }

        public void stop() throws NoSuchFieldException {
            boolean ret = updateStateField(STARTED, CLOSED);
            System.out.println("stop " + (ret ? "success" : "failed"));
        }

        private boolean updateStateField(int expected, int target) throws NoSuchFieldException {
            Unsafe unsafe = UnsafeOperator.getUnsafe();
            Field field = Server.class.getDeclaredField("state");
            long stateOffset =  unsafe.objectFieldOffset(field);
            return unsafe.compareAndSwapInt(this, stateOffset, expected, target);
        }
    }
}
