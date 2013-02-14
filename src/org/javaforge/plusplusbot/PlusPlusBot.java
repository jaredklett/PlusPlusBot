package org.javaforge.plusplusbot;

import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.*;
import java.util.*;

/**
 * Implements plusplusbot logic.
 *
 * @author Jared Klett
 */

public class PlusPlusBot extends PircBot {

    private static final long PP_THROTTLE_LIMIT = 30 * 1000L;

    private File scoreFile = new File("/home/jklett/ppb.obj");
    private Random random = new Random();
    private List<String> autoJoinList = new ArrayList<String>();
    private Map<String,Score> scoreMap = new HashMap<String,Score>();
    private Map<String,Giver> giverMap = new HashMap<String,Giver>();
    private String[] prefixes = {
            "gee whiz ",
            "golly ",
            "my goodness ",
            "good heavens ",
            "my my ",
            "well butter my biscuit ",
            "why "
    };
    private String[] plusplusExclamations = {
            "w00t! ",
            "nice! ",
            "suh-weet! ",
            "well played! ",
            "zing! ",
            "heyoooo! ",
            "sweet! "
    };
    private String[] minusminusExclamations = {
            "ouch! ",
            "daaaang! ",
            "denied! ",
            "ooooh! ",
            "owie! ",
            "ya dun goofed! ",
            "boom! "
    };
    private String[] throttled = {
            "Sorry, you're trying to award too fast.",
            "Seriously, slow your roll.",
            "Now you're just embarassing yourself."
    };
    private String[] snark = {
            ", that's so creative of you.",
            ", you have such a way with words.",
            ", you must really really like me!",
            ", I'm not sure if I should dignify that with a response.",
            ", I wuv you too.",
            ", that just warms my the cockles of my CPU.",
            ", do you need a time out?"
    };

    private String host;
    private int port;

    public PlusPlusBot(String host, int port) {
        super();
        this.host = host;
        this.port = port;
        String myNick = "plusplusbot";
        setIdentity(myNick);
        setVersion("0.1");
        setVerbose(true);
//        autoJoinList.add("#bliptv");
//        autoJoinList.add("#dev");
//        autoJoinList.add("#lunch");
//        autoJoinList.add("#cute");
        autoJoinList.add("#ppb");
    }

    private void setIdentity(String ident) {
        setName(ident);
        setLogin(ident);
        setFinger(ident);
    }

    public void start() {
        try {
            connect(host, port);
        } catch (NickAlreadyInUseException e) {
            try { Thread.sleep(30 * 1000L); } catch (InterruptedException ie) { /* ignored */ }
            setIdentity("realplusplusbot");
            try {
                connect(host, port);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String channel : autoJoinList) {
            joinChannel(channel);
        }
    }

 /* Not sure what this does yet, but I know it's not what I thought originally...

    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
        if (oldNick.equalsIgnoreCase("plusplusbot")) {
            setIdentity("plusplusbot");
        }
    }
*/

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.contains("++")) {
            // throw away anything where the ++ is not flush to the nick
            if (message.charAt(message.indexOf("+") - 1) == ' ') {
                return;
            }
            Giver giver = giverMap.get(sender);
            if (giver == null) {
                // First time. Carry on.
                giverMap.put(sender, new Giver(sender, System.currentTimeMillis()));
            } else {
                boolean withinLimit = (System.currentTimeMillis() - giver.getPlusplusTime()) < PP_THROTTLE_LIMIT;
                if (withinLimit) {
                    sendMessage(channel, throttled[giver.getAttempts() % throttled.length]);
                    giver.setAttempts(giver.getAttempts() + 1);
                    giverMap.put(sender, giver);
                    return;
                } else {
                    giver.setPlusplusTime(System.currentTimeMillis());
                    giver.setAttempts(0);
                    giverMap.put(sender, giver);
                }
            }
            String nick = message.substring(0, message.indexOf("+"));
            if (nick.equalsIgnoreCase(sender)) {
                sendMessage(channel, "Sorry " + sender + ", you can't award yourself points.");
                return;
            }
            User[] users = getUsers(channel);
            boolean found = false;
            String originalNick = nick;
            // Split on _ or - and award points to the nick.
            // Takes care of jklett++ vs jklett-laptop++
            if (nick.contains("_")) {
                nick = nick.split("_")[0];
            } else if (nick.contains("-")) {
                nick = nick.split("-")[0];
            }
            for (User user : users) {
                if (user.getNick().equalsIgnoreCase(originalNick)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                sendMessage(channel, "Sorry " + sender + ", I don't see a user with the name '" + nick + "'");
                return;
            }
            Score score = scoreMap.get(nick);
            if (score == null) {
                score = new Score(nick);
            } else {
                score.bump();
            }
            sendMessage(channel, plusplusExclamations[random.nextInt(plusplusExclamations.length)] + originalNick + " now at " + score.getScore() + "!");
            scoreMap.put(nick, score);
            return;
        }
        if (message.startsWith("plusplusbot")) {
            String[] parts = message.split("\\s");
            if (parts.length > 3) {
                sendMessage(channel, prefixes[random.nextInt(prefixes.length)] + sender + snark[random.nextInt(snark.length)]);
                return;
            }
            if (parts.length <= 1) {
                sendMessage(channel, sender + ": sorry, I didn't understand that.");
                return;
            }
            String command = parts[1];
            if (command.startsWith("help")) {
                sendMessage(channel, "How to check a score: plusplusbot score <nick>");
                return;
            }
            if (command.startsWith("scores")) {
                if (scoreMap.size() == 0) {
                    sendMessage(channel, "Sorry, I don't have any scores to report.");
                    return;
                }
                for (Score score : scoreMap.values()) {
                    sendMessage(channel, score.getNick() + " has received " + score.getScore() + " point" + (score.getScore() == 1 ? "" : "s") + " so far.");
                }
                return;
            }
            if (command.startsWith("score")) {
                if (parts.length <= 2) {
                    sendMessage(channel, sender + ": sorry, you have to ask about a specific nick. For example: plusplusbot score <nick>");
                    return;
                }
                String nick = parts[2];
                Score score = scoreMap.get(nick);
                if (score == null) {
                    sendMessage(channel, sender + ": sorry, but " + nick + " hasn't received any points yet.");
                    return;
                }
                sendMessage(channel, nick + " has received " + score.getScore() + " point" + (score.getScore() == 1 ? "" : "s") + " so far.");
            } else {
                sendMessage(channel, sender + ": sorry, I don't understand the command '" + command + "'");
            }
        }
    }
/*
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
        sendMessage(sender, "hi");
    }
*/
    public void loadScoresFromDisk() {
        if (scoreFile.exists()) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(scoreFile));
                Object object = in.readObject();
                Score[] scores = (Score[])object;
                for (Score score : scores) {
                    scoreMap.put(score.getNick(), score);
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveScoresToDisk() {
        try {
            Collection<Score> values = scoreMap.values();
            Score[] scores = values.toArray(new Score[values.size()]);
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(scoreFile));
            out.writeObject(scores);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

} // class PlusPlusBot
