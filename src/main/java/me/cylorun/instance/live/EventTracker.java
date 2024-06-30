package me.cylorun.instance.live;

import kaptainwutax.mcutils.state.Dimension;
import me.cylorun.enums.LogEventType;
import me.cylorun.enums.SpeedrunEventType;
import me.cylorun.instance.LogEvent;
import me.cylorun.instance.SpeedrunEvent;
import me.cylorun.instance.logs.LogEventListener;
import me.cylorun.instance.world.WorldEventListener;
import me.cylorun.instance.WorldFile;
import me.cylorun.utils.Vec2i;
import org.apache.commons.lang3.tuple.Pair;

public class EventTracker implements WorldEventListener, LogEventListener {
    private WorldFile world;

    public EventTracker(WorldFile world) {
        this.world = world;
        this.world.eventHandler.addListener(this);
        this.world.logHandler.addListener(this);
    }

    private void addEvent(String asset) {
        Vec2i loc = this.world.getPlayerLocation();
        Dimension dim = this.world.getPlayerDimension();
        Pair<Pair<String, Vec2i>, Dimension> p = Pair.of(Pair.of(asset, loc), dim);

        this.world.playerEvents.add(p);
    }


    private void addEvent(Vec2i loc, Dimension dim, String asset) {
        Pair<Pair<String, Vec2i>, Dimension> p = Pair.of(Pair.of(asset, loc), dim);
        this.world.playerEvents.add(p);
    }

    private Vec2i getPrevLoc() {
        if (!this.world.playerPath.isEmpty()) {
            return this.world.playerPath.get(this.world.playerPath.size() - 1).getLeft().getRight();
        }

        return this.world.getPlayerLocation(); // shouldnt happen
    }

    @Override
    public void onLogEvent(LogEvent e) {
        if (this.world.track && !this.world.finished) {
            if (e.type.equals(LogEventType.DEATH)) {
                this.addEvent("icons/map/death.png");
            }
            if (e.type.equals(LogEventType.HUNGER_RESET)) {
                this.addEvent("icons/map/hunger_reset.png");
            }
        }
    }

    @Override
    public void onSpeedrunEvent(SpeedrunEvent e) {
        if (this.world.track && !this.world.finished) {
            if (e.type.equals(SpeedrunEventType.ENTER_NETHER)) {
                Vec2i loc = this.getPrevLoc();
                Dimension dim = this.world.getPlayerDimension();
                this.addEvent(loc, dim, "icons/map/enter_portal.png");
            }
            if (e.type.equals(SpeedrunEventType.FIRST_PORTAL)) {
                Vec2i loc = this.getPrevLoc();
                Dimension dim = this.world.getPlayerDimension();
                this.addEvent(loc, dim, "icons/map/first_portal.png");
            }
            if (e.type.equals(SpeedrunEventType.SECOND_PORTAL)) {
                Vec2i loc = this.getPrevLoc();
                Dimension dim = this.world.getPlayerDimension();
                this.addEvent(loc, dim, "icons/map/second_portal.png");
            }
        }
    }
}
