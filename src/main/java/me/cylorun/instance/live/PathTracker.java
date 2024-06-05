package me.cylorun.instance.live;

import kaptainwutax.mcutils.state.Dimension;
import me.cylorun.enums.LogEventType;
import me.cylorun.instance.LogEvent;
import me.cylorun.instance.logs.LogEventListener;
import me.cylorun.instance.WorldFile;
import me.cylorun.io.TrackerOptions;
import me.cylorun.utils.Vec2i;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PathTracker implements LogEventListener {
    private Vec2i lastCoord;
    private WorldFile world;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public PathTracker(WorldFile world) {
        this.world = world;
        this.world.logHandler.addListener(this);
        this.lastCoord = world.getPlayerLocation();
        this.executor.scheduleAtFixedRate(this::tick, 0L, TrackerOptions.getInstance().path_interval, TimeUnit.SECONDS);
    }

    private void tick() {
        Dimension dim = this.world.getPlayerDimension();
        Vec2i currCoord = this.world.getPlayerLocation();
        Pair<Pair<Vec2i, Vec2i>, Dimension> p = Pair.of(Pair.of(this.lastCoord, currCoord), dim);

        this.world.playerPath.add(p);
        this.lastCoord = currCoord;
    }

    @Override
    public void onLogEvent(LogEvent e) {
        if (e.type.equals(LogEventType.DEATH)) {
            executor.schedule(() -> {
                this.lastCoord = world.getPlayerLocation();
            }, 10L, TimeUnit.SECONDS);
        }
    }
}
