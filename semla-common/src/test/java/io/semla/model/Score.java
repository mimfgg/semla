package io.semla.model;

public class Score {

    public String name;
    public int score;

    public static Score with(String name, int score) {
        Score player = new Score();
        player.name = name;
        player.score = score;
        return player;
    }
}
