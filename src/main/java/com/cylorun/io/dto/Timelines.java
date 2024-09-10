package com.cylorun.io.dto;

public class Timelines {
    public long iron_pick;
    public long nether;
    public long bastion;
    public long fortress;
    public long first_portal;
    public long second_portal;
    public long stronghold;
    public long end;

    @Override
    public String toString() {
        return "Timelines{" +
                "iron_pick=" + iron_pick +
                ", nether=" + nether +
                ", bastion=" + bastion +
                ", fortress=" + fortress +
                ", first_portal=" + first_portal +
                ", second_portal=" + second_portal +
                ", stronghold=" + stronghold +
                ", end=" + end +
                '}';
    }
}
