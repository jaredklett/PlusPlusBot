package org.javaforge.plusplusbot;

import java.io.Serializable;

/**
 * A serializable object to save scores to disk.
 * TODO: ditch serialization
 *
 * @author Jared Klett
 */

@SuppressWarnings("unused")
public class Score implements Serializable {

    private String nick;
    private int score;

    public Score(String nick) {
        this.nick = nick;
        this.score = 0;
    }

    public Score(String nick, int score) {
        this.nick = nick;
        this.score = score;
    }

    public String getNick() {
        return nick;
    }

    public void bump() {
        score++;
    }

    public void diss() {
        score--;
    }

    public int getScore() {
        return score;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(nick).append(" ").append(score);
        return builder.toString();
    }

} // class Score
