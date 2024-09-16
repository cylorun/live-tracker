package com.cylorun.instance;

import com.cylorun.Tracker;
import com.cylorun.instance.live.DistanceTracker;
import com.cylorun.instance.live.EventTracker;
import com.cylorun.instance.live.HungerResetHandler;
import com.cylorun.instance.live.PathTracker;
import com.cylorun.instance.logs.LogEventListener;
import com.cylorun.instance.logs.LogHandler;
import com.cylorun.instance.world.WorldEventHandler;
import com.cylorun.instance.world.WorldEventListener;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.ExceptionUtil;
import com.cylorun.utils.Vec2i;
import kaptainwutax.mcutils.state.Dimension;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class WorldFile extends File implements WorldEventListener, LogEventListener {
    private Runnable completionHandler;
    private PathTracker pathTracker;
    private EventTracker eventTracker;
    public final NBTReader reader;
    public final WorldEventHandler eventHandler;
    public HungerResetHandler hungerResetHandler;
    public DistanceTracker strongholdTracker;
    public LogHandler logHandler;
    public final List<Pair<Pair<Vec2i, Vec2i>, Dimension>> playerPath; // just the path the player takes
    public final List<Pair<Pair<String, Vec2i>, Dimension>> playerEvents; // locations of deaths and other special events
    public final Inventory inv;
    public boolean track = true;
    public boolean finished = false;

    public WorldFile(String path) {
        super(path);

        this.playerPath = new ArrayList<>();
        this.playerEvents = new ArrayList<>();

        this.inv = new Inventory(this);
        this.eventHandler = new WorldEventHandler(this);
        this.eventHandler.addListener(this);

        this.reader = NBTReader.from(this);

        if (TrackerOptions.getInstance().use_experimental_tracking) {
            this.hungerResetHandler = new HungerResetHandler(this);
            this.strongholdTracker = new DistanceTracker(this, SpeedrunEvent.SpeedrunEventType.FIRST_PORTAL, SpeedrunEvent.SpeedrunEventType.ENTER_STRONGHOLD);

            this.pathTracker = new PathTracker(this);
            this.eventTracker = new EventTracker(this);

            this.logHandler = new LogHandler(this);
            this.logHandler.addListener(this);
        }
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
            return Long.parseLong(NBTReader.from(this).get(NBTReader.SEED_PATH));
        } catch (NumberFormatException | NullPointerException e) {
            Tracker.log(Level.WARN, "Failed to get the seed");
            return -1;
        }
    }

    public Vec2i getPlayerLocation() {
        String stringData = this.reader.get(NBTReader.PLAYER_POS); // TODO reader.has func that checks nested values
        if (stringData == null) {
            return null;
        }

        String[] s = stringData.replaceAll("[\\[\\]]", "").split(",");
        if (s.length != 3) {
            return null;
        }
        Integer[] loc;

        try {
            loc = Arrays.stream(s)
                    .map((l) -> (int) Double.parseDouble(l))
                    .toArray(Integer[]::new);
        } catch (NumberFormatException e) {
            Tracker.log(Level.ERROR, "Failed to parse player location: " + ExceptionUtil.toDetailedString(e));
            return null;
        }
        return new Vec2i(loc[0], loc[2]);
    }

    public Dimension getPlayerDimension() {
        String data = this.reader.get(NBTReader.PLAYER_DIMENSION);
        if (data == null) {
            return null;
        }

        String[] split = data.replace("\"", "").split(":");
        if(split.length != 2) {
            return null;
        }

        return Dimension.fromString(split[1]);
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


    public void setCompletionHandler(Runnable completionHandler) {
        this.completionHandler = completionHandler;
    }

    public void onCompletion() { // not necessarily on credits, just whenever the run is over
        if (this.completionHandler != null && !this.finished) {
            this.completionHandler.run();
            this.finished = true;
        }
    }


    @Override
    public void onSpeedrunEvent(SpeedrunEvent e) {
        if (!this.finished) {
            if (e.type.equals(SpeedrunEvent.SpeedrunEventType.REJOIN_WORLD)) {
                this.track = true;
            }

            if (this.track) {
                if (e.type.equals(SpeedrunEvent.SpeedrunEventType.LEAVE_WORLD)) {
                    this.track = false;
                }

                if (e.type.equals(SpeedrunEvent.SpeedrunEventType.CREDITS)) {
                    this.onCompletion();
                }
            }
        }
    }

    @Override
    public void onLogEvent(LogEvent e) {
        if (!this.finished) {
            if (this.track) {
                if (e.type.equals(LogEvent.LogEventType.DEATH)) {
                    Vec2i loc = this.getPlayerLocation();
                    Dimension dim = this.getPlayerDimension();
                    if (loc == null || dim == null) {
                        return;
                    }
                    this.playerEvents.add(Pair.of(Pair.of("icons/map/death.png", loc), dim));
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
