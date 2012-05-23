package org.javaforge.plusplusbot;

/**
 * TODO
 *
 * @author Jared Klett
 */

public class Main extends Thread {

    /** A mutex object for the internal thread to wait on. */
    private final Object mutex = new Object();
    /** Flag for the internal thread. */
    private boolean running = false;
    /** IRC bot instance. */
    private PlusPlusBot bot;

    public Main() {
        registerShutdownHook();
        bot = new PlusPlusBot();
        bot.loadScoresFromDisk();
        bot.start();
    }

    public void start() {
        running = true;
        super.start();
    }

    public void run() {
        while (running) {
            synchronized (mutex) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    /* ignored */
                }
            }
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread("PlusPlusBot.Main.shutdownHook") {
                    public void run() {
                        running = false;
                        synchronized (mutex) {
                            mutex.notify();
                        }
                        bot.saveScoresToDisk();
                        bot.disconnect();
                        try {
                            join(1000);
                        } catch (InterruptedException e) {
                            /* ignored */
                        }
                    }
                }
        );
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }


}
