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

    public Main(String host, int port) {
        registerShutdownHook();
        bot = new PlusPlusBot(host, port);
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
        if (args.length < 1) {
            System.out.println("Usage: java -jar plusplusbot.jar <host> [<port>]");
        }
        int port = 6667;
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Could not parse port number: " + args[1] + ", defaulting to " + port);
            }
        }
        Main main = new Main(args[0], port);
        main.start();
    }


}
