package org.javaforge.plusplusbot;

import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.*;

/**
 * Implements plusplusbot logic.
 *
 * @author Jared Klett
 */

public class PlusPlusBot extends PircBot {

    private static final long PP_THROTTLE_LIMIT = 10 * 1000L;
    private static final String UNDERSCORE = "_";
    private static final String HYPHEN = "-";

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
            "you go girl! ",
            "booyakasha! ",
            "heyoooo! ",
            "sweet! ",
            "fist bump! "
    };
    private String[] minusminusExclamations = {
            "ouch! ",
            "daaaang! ",
            "denied! ",
            "ooooh! ",
            "owie! ",
            "awwww snap! ",
            "ya dun goofed! ",
            "boom! ",
            "oh no you did not! "
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
        autoJoinList.add("#bliptv");
        autoJoinList.add("#dev");
        autoJoinList.add("#lunch");
        autoJoinList.add("#techops");
        autoJoinList.add("#cute");
        autoJoinList.add("#ppbtest");
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

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.contains("++")) {
            process("+", message, channel, sender);
            return;
        }
        if (message.contains("--")) {
            process("-", message, channel, sender);
            return;
        }
        if (message.startsWith("plusplusbot")) {
            respondToCommand(message, channel, sender);
        }
    }

    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            try { Thread.sleep(2000); } catch (InterruptedException e) { /* ignored */ }
            joinChannel(channel);
            sendMessage(channel, "Ow. That hurt.");
        }
    }

    private void process(String trigger, String message, String channel, String sender) {
        // throw away anything where the ++ or -- is not flush to the nick
        if (message.charAt(message.indexOf(trigger) - 1) == ' ') {
            return;
        }
        // make sure sender is actually in the chat room
        if (!isNickInChannel(channel, sender)) {
            return;
        }
        boolean isPlusPlus = trigger.equals("+");
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
        String nick = message.substring(0, message.indexOf(trigger));
        if (nick.equalsIgnoreCase(sender)) {
            sendMessage(channel, "Sorry " + sender + ", you can't " + (isPlusPlus ? "award yourself" : "decrement your own") + " points.");
            return;
        }
        User[] users = getUsers(channel);
        boolean found = false;
        String originalNick = nick;
        nick = splitNick(nick);
        for (User user : users) {
            if (user.getNick().equalsIgnoreCase(originalNick)) {
                found = true;
                break;
            }
        }
        if (!found) {
            sendMessage(channel, "Sorry " + sender + ", I don't see a user with the name '" + originalNick + "'");
            return;
        }
        Score score = scoreMap.get(nick);
        if (score == null) {
            score = new Score(nick);
        }
        if (isPlusPlus)
            score.bump();
        else
            score.diss();
        if (isPlusPlus)
            sendMessage(channel, plusplusExclamations[random.nextInt(plusplusExclamations.length)] + originalNick + " now at " + score.getScore() + "!");
        else
            sendMessage(channel, minusminusExclamations[random.nextInt(minusminusExclamations.length)] + originalNick + " now at " + score.getScore() + "!");
        scoreMap.put(nick, score);
    }

    private boolean isNickInChannel(String channel, String nick) {
        User[] users = getUsers(channel);
        boolean found = false;
        for (User user : users) {
            if (user.getNick().equalsIgnoreCase(nick)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void respondToCommand(String message, String channel, String sender) {
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
            nick = splitNick(nick);
            if (score == null) {
                sendMessage(channel, sender + ": sorry, but " + nick + " hasn't received any points yet.");
                return;
            }
            sendMessage(channel, nick + " has received " + score.getScore() + " point" + (score.getScore() == 1 ? "" : "s") + " so far.");
        } else {
            sendMessage(channel, sender + ": sorry, I don't understand the command '" + command + "'");
        }
    }

    private String splitNick(String nick) {
        // Split on _ or - and award points to the nick. Takes care of jklett++ vs jklett-laptop++
        if (nick.contains(UNDERSCORE)) {
            nick = nick.split(UNDERSCORE)[0];
        } else if (nick.contains(HYPHEN)) {
            nick = nick.split(HYPHEN)[0];
        }
        return nick;
    }

    public void loadScoresFromDisk() {
        if (scoreFile.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(scoreFile));
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        String decoded = new String(DatatypeConverter.parseBase64Binary(line), "UTF-8");
                        String[] parts = decoded.split("\\s");
                        scoreMap.put(parts[0], new Score(parts[0], Integer.parseInt(parts[1])));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            PrintWriter out = new PrintWriter(new FileWriter(scoreFile));
            for (Score score : values) {
                out.println(DatatypeConverter.printBase64Binary(score.toString().getBytes("UTF-8")));
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

} // class PlusPlusBot
