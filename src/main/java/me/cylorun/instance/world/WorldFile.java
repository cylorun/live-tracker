package me.cylorun.instance.world;

import kaptainwutax.mcutils.state.Dimension;
import me.cylorun.Tracker;
import me.cylorun.enums.LogEventType;
import me.cylorun.enums.SpeedrunEventType;
import me.cylorun.instance.LogEvent;
import me.cylorun.instance.NBTReader;
import me.cylorun.instance.SpeedrunEvent;
import me.cylorun.instance.live.DistanceTracker;
import me.cylorun.instance.live.HungerResetHandler;
import me.cylorun.instance.live.PathTracker;
import me.cylorun.instance.logs.LogEventListener;
import me.cylorun.instance.logs.LogHandler;
import me.cylorun.instance.player.Inventory;
import me.cylorun.utils.Vec2i;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.CallbackI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WorldFile extends File implements WorldEventListener, LogEventListener {
    private CompletionHandler completionHandler;
    private final PathTracker pathTracker;
    public final NBTReader reader;
    public final WorldEventHandler eventHandler;
    public final HungerResetHandler hungerResetHandler;
    public final DistanceTracker strongholdTracker;
    public final List<Pair<Pair<Vec2i, Vec2i>, Dimension>> playerPath; // just the path the player takes
    public final List<Pair<Pair<String, Vec2i>, Dimension>> playerLocations; // locations of deaths and other special events
    public final Inventory inv;
    public final LogHandler logHandler;
    public boolean track = true;
    public boolean finished = false;

    public WorldFile(String path) {
        super(path);
        this.inv = new Inventory(this);
        this.eventHandler = new WorldEventHandler(this);
        this.logHandler = new LogHandler(this);
        this.hungerResetHandler = new HungerResetHandler(this);
        this.strongholdTracker = new DistanceTracker(this, SpeedrunEventType.FIRST_PORTAL, SpeedrunEventType.ENTER_STRONGHOLD);
        this.reader = NBTReader.from(this);
        this.pathTracker = new PathTracker(this);
        this.playerPath = new ArrayList<>();
        this.playerLocations = new ArrayList<>();

        this.logHandler.addListener(this);
        this.eventHandler.addListener(this);
    }

    public Path getRecordPath() {
        return Paths.get(this.getAbsolutePath()).resolve("speedrunigt").resolve("record.json");
    }

    public Path getEventLog() {
        return Paths.get(this.getAbsolutePath()).resolve("speedrunigt").resolve("events.log");
    }

    public Path getLogPath() {
        return Paths.get(this.getAbsolutePath()).getParent().getParent().resolve("logs").resolve("latest.log");
    }
    public long getSeed() {
        try {
            return  Long.parseLong(NBTReader.from(this).get(NBTReader.SEED_PATH));
        } catch (NumberFormatException e) {
            Tracker.log(Level.WARN, "Failed to get the seed");
            return 0;
        }
    }

    public Path getLevelDatPath() {
        return Paths.get(this.getAbsolutePath()).resolve("level.dat");
    }

    public String getUsername() {
        // [11:30:07] [Render thread/INFO]: Setting user: cylorun
        String regex = "^\\[\\d{2}:\\d{2}:\\d{2}\\] \\[Render thread\\/INFO\\]: Setting user: .*$";
        Pattern pattern = Pattern.compile(regex);
        BufferedReader reader = null;
        String username = "";
        try {
            reader = new BufferedReader(new FileReader(this.getLogPath().toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (pattern.matcher(line).find()) {
                    String[] split = line.split(":");
                    username = split[split.length - 1].trim();
                }
            }
        } catch (IOException e) {
            Tracker.log(Level.ERROR, "Something went wrong while trying to get the players username");
        }
        return username;
    }


    public void setCompletionHandler(CompletionHandler completionHandler) {
        this.completionHandler = completionHandler;
    }

    public void onCompletion() { // not necessarily on credits, just whenever the run is over
        if (this.completionHandler != null && !this.finished) {
            this.completionHandler.handleCompletion();
            this.finished = true;
        }
    }


    @Override
    public void onSpeedrunEvent(SpeedrunEvent e) {
        if (!this.finished) {
            if (e.type.equals(SpeedrunEventType.REJOIN_WORLD)) {
                this.track = true;
            }

            if (this.track) {
                if (e.type.equals(SpeedrunEventType.LEAVE_WORLD)) {
                    this.track = false;
                }

                if (e.type.equals(SpeedrunEventType.CREDITS)) {
                    this.onCompletion();
                }
            }
        }
    }

    @Override
    public void onLogEvent(LogEvent e) {
        if (!this.finished) {
            if (this.track) {
                if (e.type.equals(LogEventType.DEATH)) {
                    Vec2i loc = this.reader.getPlayerLocation();
                    Dimension dim = this.reader.getPlayerDimension();
                    this.playerLocations.add(Pair.of(Pair.of("icons/death.png", loc), dim));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WorldFile f)) {
            return false;
        }

        return f.getAbsolutePath().equals(this.getAbsolutePath());
    }

    @Override
    public String toString() {
        return this.getName();
    }

}
