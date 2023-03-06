package top.kwseeker.jvm.debug.basic;

/**
 * synchronized可以用在方法、静态方法、代码块，这里只举一种
 */
public class SynchronizedDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) {
        int i = 0;
        synchronized (lock) {
            i++;
        }
        System.out.println(i);
    }
}
