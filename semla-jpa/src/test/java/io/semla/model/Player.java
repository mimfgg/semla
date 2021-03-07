package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Managed
public class Player implements Serializable {

    @Id
    public int id;
    public String name;
    public int score;

    public static Player with(int id, String name, int score) {
        Player player = new Player();
        player.id = id;
        player.name = name;
        player.score = score;
        return player;
    }

}
