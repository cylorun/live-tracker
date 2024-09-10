package com.cylorun.io.dto;

public class Travel {
    public int walk_on_water;
    public int walk;
    public int walk_under_water;
    public int swim;
    public int boat;
    public int sprint;

    @Override
    public String toString() {
        return "Travel{" +
                "walk_on_water=" + walk_on_water +
                ", walk=" + walk +
                ", walk_under_water=" + walk_under_water +
                ", swim=" + swim +
                ", boat=" + boat +
                ", sprint=" + sprint +
                '}';
    }
}
