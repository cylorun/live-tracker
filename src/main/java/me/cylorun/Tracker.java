package me.cylorun;

import me.cylorun.io.minecraft.logs.LogHandler;
import me.cylorun.io.minecraft.world.WorldCreationEventHandler;


public class Tracker {
    public static void main(String[] args) {
        WorldCreationEventHandler worldHandler = new WorldCreationEventHandler(); // only one WorldFile object is created per world path
        worldHandler.addListener(world -> {
            LogHandler logH = new LogHandler(world);

        });

    }

}