package me.cylorun.instance.live;

import me.cylorun.Tracker;
import me.cylorun.enums.SpeedrunEventType;
import me.cylorun.instance.NBTReader;
import me.cylorun.instance.SpeedrunEvent;
import me.cylorun.instance.world.WorldEventListener;
import me.cylorun.instance.world.WorldFile;
import me.cylorun.io.TrackerOptions;
import me.cylorun.utils.Vec2i;
import org.apache.logging.log4j.Level;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DistanceTracker implements WorldEventListener {
    private final NBTReader reader;
    private final SpeedrunEventType startEvent;
    private final SpeedrunEventType endEvent;
    private final WorldFile file;
    public Vec2i startPoint;
    public Vec2i endPoint;

    public DistanceTracker(WorldFile file, SpeedrunEventType startEvent, SpeedrunEventType endEvent) {
        this.endEvent = endEvent;
        this.startEvent = startEvent;
        this.file = file;

        this.file.eventHandler.addListener(this);
        this.reader = NBTReader.from(file);
    }

    public String getFinalData() {
        if (this.startPoint == null || this.endPoint == null) {
            return "";
        }

        return String.valueOf(this.startPoint.distanceTo(this.endPoint));
    }


    @Override
    public void onSpeedrunEvent(SpeedrunEvent e) {
        if (e.type.equals(this.endEvent)) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                this.endPoint = reader.getPlayerLocation();
                Tracker.log(Level.DEBUG, this.endEvent + " reached at: " + this.endPoint);
            }, TrackerOptions.getInstance().game_save_interval + 2, TimeUnit.SECONDS);
        }

        if (e.type.equals(this.startEvent)) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                this.startPoint = reader.getPlayerLocation();
                Tracker.log(Level.DEBUG, this.startEvent + " reached at: " + this.startPoint);
            }, TrackerOptions.getInstance().game_save_interval + 2, TimeUnit.SECONDS);
        }
    }
}
