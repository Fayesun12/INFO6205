package edu.neu.coe.info6205.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Timer {

    /**
     * Construct a new Timer and set it running.
     */
    public Timer() {
        resume();
    }

    /**
     * Run the given function n times, once per "lap" and then return the result of calling stop().
     * 运行给定函数 n 次，每“圈”一次，然后返回调用 stop() 的结果。
     *
     * @param n        the number of repetitions.
     * @param function a function which yields a T (T may be Void).
     * @return the average milliseconds per repetition.
     * * @param n 重复次数。
     *       * @param function 一个产生 T 的函数（T 可能是 Void）。
     *       * @return 每次重复的平均毫秒数。
     */
    public <T> double repeat(int n, Supplier<T> function) {
        for (int i = 0; i < n; i++) {
            function.get();
            lap();
        }
        pause();
        final double result = meanLapTime();
        resume();
        return result;
    }

    /**
     * Run the given functions n times, once per "lap" and then return the result of calling stop().
     *
     * @param n        the number of repetitions.
     * @param supplier a function which supplies a different T value for each repetition.
     * @param function a function T=>U and which is to be timed (U may be Void).
     * @return the average milliseconds per repetition.
     * * 运行给定函数 n 次，每“圈”一次，然后返回调用 stop() 的结果。
     *       *
     *       * @param n 重复次数。
     *       * @param supplier 一个函数，它为每次重复提供不同的 T 值。
     *       * @param function 一个函数 T=>U 并且要被计时（U 可以是 Void）。
     *       * @return 每次重复的平均毫秒数。
     */
    public <T, U> double repeat(int n, Supplier<T> supplier, Function<T, U> function) {
        return repeat(n, supplier, function, null, null);
    }

    /**
     * Pause (without counting a lap); run the given functions n times while being timed, i.e. once per "lap", and finally return the result of calling meanLapTime().
     *
     * @param n            the number of repetitions.
     * @param supplier     a function which supplies a T value.
     * @param function     a function T=>U and which is to be timed.
     * @param preFunction  a function which pre-processes a T value and which precedes the call of function, but which is not timed (may be null).
     * @param postFunction a function which consumes a U and which succeeds the call of function, but which is not timed (may be null).
     * @return the average milliseconds per repetition.
     * *暂停（不计算一圈）； 在计时的同时运行给定函数 n 次，即每“圈”一次，最后返回调用 meanLapTime() 的结果。
     *       *
     *       * @param n 重复次数。
     *       * @param supplier 提供 T 值的函数。
     *       * @param function 函数 T=>U 并且要被计时。
     *       * @param preFunction 一个函数，它预处理一个 T 值并且在函数调用之前，但它不定时（可能为 null）。
     *       * @param postFunction 一个函数，它消耗一个 U 并在函数调用之后成功，但不定时（可能为 null）。
     *       * @return 每次重复的平均毫秒数。
     */
    public <T, U> double repeat(int n, Supplier<T> supplier, Function<T, U> function, UnaryOperator<T> preFunction, Consumer<U> postFunction) {
        logger.trace("repeat: with " + n + " runs");
        //fanyesun
        // FIXME: note that the timer is running when this method is called and should still be running when it returns. by replacing the following code
        for (int a = 0; a < n; a++) {
            pause();
            T preFunc = supplier.get();
            if (preFunction != null)
                preFunction.apply(preFunc);
            resume();
            U postFunc = function.apply(preFunc);
            pauseAndLap();
            if (postFunction != null)
                postFunction.accept(postFunc);
            resume();
        }
        pause();
        return meanLapTime();
//        return 0;
        // END
    }

    /**
     * Stop this Timer and return the mean lap time in milliseconds.
     *
     * @return the average milliseconds used by each lap.
     * @throws TimerException if this Timer is not running.
     * * 停止此计时器并以毫秒为单位返回平均单圈时间。
     *       *
     *       * @return 每圈使用的平均毫秒数。
     *       * @throws TimerException 如果这个定时器没有运行。
     */
    public double stop() {
        pauseAndLap();
        return meanLapTime();
    }

    /**
     * Return the mean lap time in milliseconds for this paused timer.
     *
     * @return the average milliseconds used by each lap.
     * @throws TimerException if this Timer is running.
     * * 返回此暂停计时器的平均单圈时间（以毫秒为单位）。
     *       *
     *       * @return 每圈使用的平均毫秒数。
     *       * @throws TimerException 如果这个定时器正在运行。
     */
    public double meanLapTime() {
        if (running) throw new TimerException();
        return toMillisecs(ticks) / laps;
    }

    /**
     * Pause this timer at the end of a "lap" (repetition).
     * The lap counter will be incremented by one.
     *
     * @throws TimerException if this Timer is not running.
     * * 在“圈”（重复）结束时暂停此计时器。
     *       * 计圈器将加一。
     *       *
     *       * @throws TimerException 如果这个定时器没有运行。
     */
    public void pauseAndLap() {
        lap();
        ticks += getClock();
        running = false;
    }

    /**
     * Resume this timer to begin a new "lap" (repetition).
     *恢复此计时器以开始新的“圈”（重复）。
     * @throws TimerException if this Timer is already running.
     */
    public void resume() {
        if (running) throw new TimerException();
        ticks -= getClock();
        running = true;
    }

    /**
     * Increment the lap counter without pausing.
     * This is the equivalent of calling pause and resume.
     * 在不暂停的情况下增加圈数计数器。
     *       * 这相当于调用pause和resume。
     *
     * @throws TimerException if this Timer is not running.
     */
    public void lap() {
        if (!running) throw new TimerException();
        laps++;
    }

    /**
     * Pause this timer during a "lap" (repetition).
     * The lap counter will remain the same.
     * * 在“圈”（重复）期间暂停此计时器。
     *       * 计圈器将保持不变。
     *
     * @throws TimerException if this Timer is not running.
     */
    public void pause() {
        pauseAndLap();
        laps--;
    }

    /**
     * Method to yield the total number of milliseconds elapsed.
     * 产生经过的总毫秒数的方法。
     * NOTE: an exception will be thrown if this is called while the timer is running.产生经过的总毫秒数的方法。
     *       * 注意：如果在计时器运行时调用它，将抛出异常。
     *
     *
     * @return the total number of milliseconds elapsed for this timer.
     */
    public double millisecs() {
        if (running) throw new TimerException();
        return toMillisecs(ticks);
    }

    @Override
    public String toString() {
        return "Timer{" +
                "ticks=" + ticks +
                ", laps=" + laps +
                ", running=" + running +
                '}';
    }

    private long ticks = 0L;
    private int laps = 0;
    private boolean running = false;

    // NOTE: Used by unit tests
    private long getTicks() {
        return ticks;
    }

    // NOTE: Used by unit tests
    private int getLaps() {
        return laps;
    }

    // NOTE: Used by unit tests
    private boolean isRunning() {
        return running;
    }

    /**
     * Get the number of ticks from the system clock.
     * <p>
     * NOTE: (Maintain consistency) There are two system methods for getting the clock time.
     * Ensure that this method is consistent with toMillisecs.
     *
     * @return the number of ticks for the system clock. Currently defined as nano time.
     *
     *
     * * 从系统时钟中获取滴答数。
     *       * <p>
     *       * 注意：（保持一致性）有两种获取时钟时间的系统方法。
     *       * 确保该方法与toMillisecs一致。
     *       *
     *       * @return 系统时钟的滴答数。 目前定义为纳米时间。
     */
    private static long getClock() {
        // FIXME by replacing the following code
        long time = System.nanoTime();
        return time;
//         return 0;
        // END
    }

    /**
     * NOTE: (Maintain consistency) There are two system methods for getting the clock time.
     * Ensure that this method is consistent with getTicks.
     *
     * @param ticks the number of clock ticks -- currently in nanoseconds.
     * @return the corresponding number of milliseconds.
     * * 注意：（保持一致性）有两种获取时钟时间的系统方法。
     *       * 确保该方法与getTicks一致。
     *       *
     *       * @param ticks 时钟滴答的数量——目前以纳秒为单位。
     *       * @return 对应的毫秒数。
     */
    private static double toMillisecs(long ticks) {
        // FIXME by replacing the following code
        return (double) (ticks / 1000000);
//         return 0;
        // END
    }

    final static LazyLogger logger = new LazyLogger(Timer.class);

    static class TimerException extends RuntimeException {
        public TimerException() {
        }

        public TimerException(String message) {
            super(message);
        }

        public TimerException(String message, Throwable cause) {
            super(message, cause);
        }

        public TimerException(Throwable cause) {
            super(cause);
        }
    }
}