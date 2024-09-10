package com.cylorun.instance;

import com.cylorun.Tracker;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.Level;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class Run extends HashMap<String, Object> {
    public final WorldFile worldFile;
    public final JsonObject recordFile;
    public JsonObject stats;
    public JsonObject adv;
    private List<SpeedrunEvent> eventLog;
    private boolean hasData = false;


    public Run(WorldFile worldFile) throws IOException{
        this.worldFile = worldFile;
        this.recordFile = JSONUtil.parseFile(worldFile.getRecordPath().toFile());
        if (recordFile == null) {
            throw new IOException("Failed to load record file: " + worldFile.getRecordPath().toString());
        }

        this.eventLog = this.worldFile.eventHandler.events;
        this.adv = JSONUtil.getOptionalJsonObj(this.recordFile, "advancements").orElseThrow(IOException::new);
        this.stats = this.getStats();
    }

    public Run gatherAll() {
        String[] majorSplits = {"rsg.obtain_iron_pickaxe",
                "rsg.enter_nether",
                "rsg.enter_bastion",
                "rsg.enter_fortress",
                "rsg.first_portal",
                "rsg.second_portal",
                "rsg.enter_stronghold",
                "rsg.enter_end"};

        String[] splitNames = {"time_iron_pick",
                "time_nether",
                "time_bastion",
                "time_fortress",
                "time_first_portal",
                "time_second_portal",
                "time_stronghold",
                "time_end"};

        this.put("run_id", getNextRunID());
        this.put("date_played_est", this.getDate());
        this.put("date_played_est_2", this.getDate());
        this.put("iron_source", this.getIronSource());
        this.put("enter_type", this.getEnterType());
        this.put("gold_source", this.getGoldSource());
        this.put("spawn_biome", this.getSpawnBiome());
        this.put("rta", msToString(this.recordFile.get("final_rta").getAsLong()));
        this.put("time_wood", this.getWoodTime());

        for (int i = 0; i < majorSplits.length; i++) {
            this.put(splitNames[i], this.getSplitTime(majorSplits[i]));
        }

        this.put("igt", msToString(this.recordFile.get("final_igt").getAsLong()));
        this.put("igt_2", msToString(this.recordFile.get("final_igt").getAsLong()));

        this.put("gold_dropped", this.getGoldDropped());
        this.putAll(this.getMiscStats());
        this.put("world_name", worldFile.getName());
        this.putAll(this.getMobKills());
        this.putAll(this.getFoods());
        this.putAll(this.getTravelled());
        if (TrackerOptions.getInstance().use_experimental_tracking) {
            this.putAll(this.getFinalBarters());
            this.put("sh_dist", this.worldFile.strongholdTracker.getFinalData());
            this.put("sh_ring", getStrongholdRing(this.worldFile.strongholdTracker.endPoint));
            this.put("explosives_used", this.getExplosivesUsed());
        }
        this.put("seed", String.valueOf(this.worldFile.getSeed()));

        this.hasData = true;
        return this;
    }

    private JsonObject getStats() throws IOException {
        Set<String> uuids = this.recordFile.get("stats").getAsJsonObject().keySet();
        if (!uuids.isEmpty()) {
            return this.recordFile.get("stats").getAsJsonObject().get(uuids.toArray()[0].toString()).getAsJsonObject().get("stats").getAsJsonObject();
        }
        throw new IOException("Could not get game stats");
    }



    private String getDate() {
        Date date = new Date(this.recordFile.get("date").getAsLong());
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(date);
    }


    private String getWoodTime() {
        try {
            long ms = this.adv.get("minecraft:recipes/misc/charcoal").getAsJsonObject().get("criteria").getAsJsonObject().get("has_log").getAsJsonObject().get("igt").getAsLong();
            return msToString(ms);
        } catch (Exception ignored) {
            return "";
        }
    }

    private int getExplosivesUsed() {
        if (!this.stats.has("minecraft:used")) return 0;
        int explUsed = 0;
        JsonObject used = this.stats.get("minecraft:used").getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : used.asMap().entrySet()) {
            if (e.getKey().contains("_bed") || e.getKey().equals("minecraft:respawn_anchor"))
                explUsed += e.getValue().getAsInt();
        }

        return explUsed - this.worldFile.hungerResetHandler.respawnPointsSet;
    }

    private int getGoldDropped() {
        if (this.stats.has("minecraft:dropped") && this.stats.getAsJsonObject("minecraft:dropped").has("minecraft:gold_ingot")) {
            int pickedup = this.stats.getAsJsonObject("minecraft:picked_up").has("minecraft:gold_ingot") ? this.stats.getAsJsonObject("minecraft:picked_up").get("minecraft:gold_ingot").getAsInt() : 0;
            return this.stats.getAsJsonObject("minecraft:dropped").get("minecraft:gold_ingot").getAsInt() - pickedup;
        }
        return 0;
    }

    public static int getStrongholdRing(Vec2i strongholdLoc) {
        if (strongholdLoc == null) {
            return 0;
        }

        int dist = strongholdLoc.distanceTo(Vec2i.ZERO);
        if (dist <= 2816) return 1;
        if (dist <= 5888) return 2;
        if (dist <= 8960) return 3;
        if (dist <= 12032) return 4;
        if (dist <= 15104) return 5;
        if (dist <= 18146) return 6;
        if (dist <= 21248) return 7;
        return 8;
    }

    private String getSplitTime(String splitName) {
        long time = Long.MAX_VALUE;
        for (SpeedrunEvent e : this.eventLog) {
            if (e.type.label.equals(splitName)) {
                time = e.igt;
            }
        }

        if (time == Long.MAX_VALUE) {
            return "";
        }

        return Run.msToString(time);
    }

    private String getSpawnBiome() { // copied from pncakespoon tracker
        String spawnBiome = "None";
        if (adv.has("minecraft:adventure/adventuring_time")) {
            JsonObject adventuringTime = adv.getAsJsonObject("minecraft:adventure/adventuring_time");
            if (adventuringTime.has("criteria")) {
                JsonObject criteria = adventuringTime.getAsJsonObject("criteria");
                for (String biome : criteria.keySet()) {
                    JsonObject biomeData = criteria.getAsJsonObject(biome);
                    if (biomeData.has("igt") && biomeData.get("igt").getAsInt() == 0) {
                        spawnBiome = biome.split(":")[1];
                        break;
                    }
                }
            }
        }
        return spawnBiome;
    }

    private String getEnterType() { // copied from pncakespoon tracker
        String enterType = "None";
        if (this.adv.has("minecraft:story/enter_the_nether")) {
            enterType = "Obsidian";
            if (this.stats.has("minecraft:mined") && this.stats.get("minecraft:mined").getAsJsonObject().has("minecraft:magma_block")) {
                if (this.adv.has("minecraft:story/lava_bucket")) {
                    enterType = "Magma Ravine";
                } else {
                    enterType = "Bucketless";
                }
            } else if (this.adv.has("minecraft:story/lava_bucket")) {
                enterType = "Lava Pool";

            }
        }
        return enterType;
    }

    private String getGoldSource() { // copied from pncakespoon tracker
        String goldSource = "None";

        // Check if "gold_ingot" is either dropped or picked up
        if ((stats.has("minecraft:dropped") && stats.get("minecraft:dropped").getAsJsonObject().has("minecraft:gold_ingot")) ||
                (stats.has("minecraft:picked_up") && (stats.get("minecraft:picked_up").getAsJsonObject().has("minecraft:gold_ingot") ||
                        stats.get("minecraft:picked_up").getAsJsonObject().has("minecraft:gold_block")))) {
            goldSource = "Classic";

            // Check if "dark_prismarine" is mined
            if (stats.has("minecraft:mined") && stats.get("minecraft:mined").getAsJsonObject().has("minecraft:dark_prismarine")) {
                goldSource = "Monument";
            } else if (adv.has("minecraft:nether/find_bastion")) {
                goldSource = "Bastion";
            }
        }
        return goldSource;
    }

    private String getIronSource() { // copied from pncakespoon tracker
        String ironSource = "None";

        if (this.adv.has("minecraft:story/smelt_iron") || this.adv.has("minecraft:story/iron_tools") ||
                (stats.has("minecraft:crafted") && (stats.getAsJsonObject("minecraft:crafted").has("minecraft:diamond_pickaxe")))) {
            ironSource = "Misc";

            // If iron not obtained before nether enter
            if (this.adv.has("minecraft:story/enter_the_nether") && this.adv.has("minecraft:story/smelt_iron")) {
                int netherIgt = this.adv.getAsJsonObject("minecraft:story/enter_the_nether").get("igt").getAsInt();
                int ironIgt = this.adv.getAsJsonObject("minecraft:story/smelt_iron").get("igt").getAsInt();
                if (netherIgt < ironIgt) {
                    ironSource = "Nether";
                }
            } else if (stats.has("minecraft:crafted") && stats.getAsJsonObject("minecraft:crafted").has("minecraft:furnace") &&
                    stats.has("minecraft:mined") && stats.getAsJsonObject("minecraft:mined").has("minecraft:iron_ore")) {
                ironSource = "Structureless";
            }
            // If haybale mined or iron golem killed or iron pickaxe obtained from chest
            else if ((stats.has("minecraft:mined") && stats.getAsJsonObject("minecraft:mined").has("minecraft:hay_block")) ||
                    (stats.has("minecraft:killed") && stats.getAsJsonObject("minecraft:killed").has("minecraft:iron_golem")) ||
                    (!(stats.has("minecraft:crafted") && (stats.getAsJsonObject("minecraft:crafted").has("minecraft:iron_pickaxe") ||
                            stats.getAsJsonObject("minecraft:crafted").has("minecraft:diamond_pickaxe"))) &&
                            this.adv.has("minecraft:story/iron_tools"))) {
                ironSource = "Village";
            }
            // If more than 7 tnt mined
            else if (stats.has("minecraft:mined") && stats.getAsJsonObject("minecraft:mined").has("minecraft:tnt") &&
                    stats.getAsJsonObject("minecraft:mined").get("minecraft:tnt").getAsInt() > 7) {
                ironSource = "Desert Temple";
            }
            // If stepped foot in ocean or beach under 3 minutes
            else if (this.adv.has("minecraft:adventure/adventuring_time")) {
                JsonObject adventuringTime = this.adv.getAsJsonObject("minecraft:adventure/adventuring_time");
                for (String biome : adventuringTime.getAsJsonObject("criteria").keySet()) {
                    if ((biome.contains("beach") || biome.contains("ocean")) &&
                            adventuringTime.getAsJsonObject("criteria").getAsJsonObject(biome).get("igt").getAsInt() < 180000) {
                        // If potato, wheat, or carrot obtained
                        if (this.adv.has("minecraft:recipes/food/baked_potato") ||
                                this.adv.has("minecraft:recipes/food/bread") ||
                                this.adv.has("minecraft:recipes/transportation/carrot_on_a_stick")) {
                            ironSource = "Full Shipwreck";
                        }
                        // If sus stew or rotten flesh eaten
                        else if (this.stats.has("minecraft:used") &&
                                (this.stats.getAsJsonObject("minecraft:used").has("minecraft:suspicious_stew") ||
                                        this.stats.getAsJsonObject("minecraft:used").has("minecraft:rotten_flesh"))) {
                            ironSource = "Full Shipwreck";
                        }
                        // If tnt exploded
                        else if (this.stats.has("minecraft:used") && this.stats.getAsJsonObject("minecraft:used").has("minecraft:tnt")) {
                            ironSource = "Buried Treasure w/ tnt";
                        }
                        // If cooked salmon or cod eaten
                        else if (this.stats.has("minecraft:used") &&
                                (this.stats.getAsJsonObject("minecraft:used").has("minecraft:cooked_salmon") ||
                                        this.stats.getAsJsonObject("minecraft:used").has("minecraft:cooked_cod"))) {
                            ironSource = "Buried Treasure";
                        }
                        // If sand/gravel mined before iron acquired
                        else if (this.adv.has("minecraft:recipes/building_blocks/magenta_concrete_powder") &&
                                this.adv.getAsJsonObject("minecraft:recipes/building_blocks/magenta_concrete_powder").getAsJsonObject("criteria")
                                        .getAsJsonObject("has_the_recipe").get("igt").getAsInt() < this.adv.getAsJsonObject("minecraft:story/smelt_iron").get("igt").getAsInt()) {
                            ironSource = "Buried Treasure";
                        }
                        // If wood mined before iron obtained
                        else if (((this.adv.has("minecraft:story/smelt_iron") && this.adv.has("minecraft:recipes/misc/charcoal")) &&
                                this.adv.getAsJsonObject("minecraft:story/smelt_iron").get("igt").getAsInt() > this.adv.getAsJsonObject("minecraft:recipes/misc/charcoal").get("igt").getAsInt()) ||
                                (this.adv.has("minecraft:recipes/misc/charcoal") && !(this.adv.has("minecraft:story/iron_tools"))) &&
                                        ((this.stats.has("minecraft:custom") && (this.stats.getAsJsonObject("minecraft:custom").get("minecraft:open_chest").getAsInt() == 1)) ||
                                                this.adv.has("minecraft:nether/find_bastion"))) {
                            ironSource = "Half Shipwreck";
                        } else if ((this.stats.has("minecraft:crafted") && this.stats.getAsJsonObject("minecraft:crafted").has("minecraft:diamond_pickaxe")) ||
                                (this.stats.has("minecraft:crafted") && this.stats.getAsJsonObject("minecraft:crafted").has("minecraft:diamond_sword"))) {
                            ironSource = "Buried Treasure";
                        } else if (this.stats.has("minecraft:mined") &&
                                ((this.stats.getAsJsonObject("minecraft:mined").has("minecraft:oak_log") &&
                                        this.stats.getAsJsonObject("minecraft:mined").get("minecraft:oak_log").getAsInt() <= 4) ||
                                        (this.stats.getAsJsonObject("minecraft:mined").has("minecraft:dark_oak_log") &&
                                                this.stats.getAsJsonObject("minecraft:mined").get("minecraft:dark_oak_log").getAsInt() <= 4) ||
                                        (this.stats.getAsJsonObject("minecraft:mined").has("minecraft:birch_log") &&
                                                this.stats.getAsJsonObject("minecraft:mined").get("minecraft:birch_log").getAsInt() <= 4) ||
                                        (this.stats.getAsJsonObject("minecraft:mined").has("minecraft:jungle_log") &&
                                                this.stats.getAsJsonObject("minecraft:mined").get("minecraft:jungle_log").getAsInt() <= 4) ||
                                        (this.stats.getAsJsonObject("minecraft:mined").has("minecraft:spruce_log") &&
                                                this.stats.getAsJsonObject("minecraft:mined").get("minecraft:spruce_log").getAsInt() <= 4) ||
                                        (this.stats.getAsJsonObject("minecraft:mined").has("minecraft:acacia_log") &&
                                                this.stats.getAsJsonObject("minecraft:mined").get("minecraft:acacia_log").getAsInt() <= 4))) {
                            ironSource = "Half Shipwreck";
                        } else {
                            ironSource = "Buried Treasure";
                        }
                    }
                }
            }
        }
        return ironSource;
    }

    public Map<String, String> getFinalBarters() {
        Map<String, String> res = new HashMap<>();
        URL url = Tracker.class.getClassLoader().getResource("tracked.json");
        Assert.isNotNull(url, "Resource not found: events/tracked.json");

        JsonObject o = ResourceUtil.loadJsonResource(url);
        JsonArray barters = o.get("TRACKED_BARTERS").getAsJsonArray();
        JsonObject pickedUp;

        if (this.stats.has("minecraft:picked_up")) {
            pickedUp = this.stats.get("minecraft:picked_up").getAsJsonObject();
        } else {
            for (JsonElement e : barters) {
                String itemName = e.getAsString().split(":")[1];
                res.put(itemName, "0");
            }
            return res;
        }

        for (JsonElement barterItem : barters) {
            String itemName = barterItem.getAsString().split(":")[1];
            if (pickedUp.has(barterItem.getAsString())) {
                int diff = 0;
                if (this.worldFile.hungerResetHandler.itemDiffs.containsKey(barterItem.getAsString())) {
                    diff = this.worldFile.hungerResetHandler.itemDiffs.get(barterItem.getAsString());
                }
                res.put(itemName, String.valueOf(pickedUp.get(barterItem.getAsString()).getAsInt() - diff));
            } else {
                res.put(itemName, "0");
            }
        }

        return res;
    }

    public Map<String, String> getMiscStats() {
        Map<String, String> res = new HashMap<>();
        URL url = Tracker.class.getClassLoader().getResource("tracked.json");

        JsonObject checks = ResourceUtil.loadJsonResource(url).getAsJsonObject("MISC_CHECKS");
        Assert.isNotNull(checks);
        Map<String, JsonElement> m = checks.asMap();

        for (Map.Entry<String, JsonElement> e : m.entrySet()) {
            JsonArray check = e.getValue().getAsJsonArray();
            try {
                String value = this.stats.getAsJsonObject(check.get(0).getAsString()).get(check.get(1).getAsString()).getAsString();
                res.put(e.getKey(), value);
            } catch (Exception ex) {
                res.put(e.getKey(), "0");
            }

        }
        return res;
    }

    public Map<String, String> getMobKills() {
        Map<String, String> res = new HashMap<>();
        URL url = Tracker.class.getClassLoader().getResource("tracked.json");
        JsonArray mobs = ResourceUtil.loadJsonResource(url).getAsJsonArray("TRACKED_MOBS");
        for (JsonElement mob : mobs) {
            try {
                String val = this.stats.getAsJsonObject("minecraft:killed").get(mob.getAsString()).getAsString();
                res.put(mob.getAsString().split(":")[1], val);
            } catch (Exception e) {
                res.put(mob.getAsString().split(":")[1], "0");
            }
        }
        return res;
    }

    public Map<String, String> getFoods() {
        Map<String, String> res = new HashMap<>();
        URL url = Tracker.class.getClassLoader().getResource("tracked.json");
        JsonArray foods = ResourceUtil.loadJsonResource(url).getAsJsonArray("TRACKED_FOODS");
        for (JsonElement food : foods) {
            try {
                String val = this.stats.getAsJsonObject("minecraft:used").get(food.getAsString()).getAsString();
                res.put(food.getAsString().split(":")[1], val);
            } catch (Exception e) {
                res.put(food.getAsString().split(":")[1], "0");
            }
        }
        return res;
    }

    public Map<String, String> getTravelled() {
        Map<String, String> res = new HashMap<>();
        URL url = Tracker.class.getClassLoader().getResource("tracked.json");
        JsonArray methods = ResourceUtil.loadJsonResource(url).getAsJsonArray("TRAVEL_METHODS");
        for (JsonElement method : methods) {
            String itemName = method.getAsString().split(":")[1].replace("_one_cm","");
            try {
                String val = this.stats.getAsJsonObject("minecraft:custom").get(method.getAsString()).getAsString();
                res.put(itemName, val);
            } catch (Exception e) {
                res.put(itemName, "0");
            }
        }
        return res;
    }

    public boolean hasFinished() {
        return JSONUtil.getOptionalBool(this.recordFile, "is_completed").orElse(false);
    }

    public boolean shouldPush() {
        TrackerOptions options = TrackerOptions.getInstance();
        if (options.only_track_completions) {
            return this.hasFinished();
        }

        Assert.isNotNull(this.stats, "Stats is null");
        String runType = this.recordFile.get("run_type").getAsString();
        if ((options.detect_ssg && runType.equals("set_seed")) || recordFile.get("category").getAsString().equals("pratice_world")) { // yes its pratice not practice
            return false;
        }

        return this.adv.has("minecraft:story/smelt_iron") || this.adv.has("minecraft:story/enter_the_nether");
    }

    public static String msToString(long time) {

        long totalSeconds = time / 1000;
        long hours = totalSeconds / 3600;
        long remainingSeconds = totalSeconds % 3600;
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static int getNextRunID() {
        if (TrackerOptions.getInstance().upload_remote_server) {
            return getNextRunIDFromServer();
        }
        return getNextRunIDLocal();
    }
    private static int getNextRunIDLocal() {
        File localFolder = TrackerOptions.getTrackerDir().resolve("local").toFile();
        if (!localFolder.exists()) {
            return 1;
        }

        File latest = FileUtil.getLastModified(localFolder);
        if (latest == null) {
            return 1;
        }

        JsonObject o = JSONUtil.parseFile(latest);
        if (o == null) {
            return 1;
        }

        return JSONUtil.getOptionalInt(o, "run_id").orElse(1) + 1;
    }
    private static int getNextRunIDFromServer() {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .get()
                .url(TrackerOptions.getInstance().api_url + "/runs/latest")
                .build();

        try (Response res = client.newCall(req).execute()) {
            String bodyJson = res.body().string();
            JsonObject jsonData = JsonParser.parseString(bodyJson).getAsJsonObject();
            JsonElement runIdElement = jsonData.get("run_id");
            if (runIdElement != null) {
                return runIdElement.getAsInt() + 1;
            }
        } catch (IOException | NullPointerException e) {
            Tracker.log(Level.ERROR, "Something went wrong while trying to fetch the last run");
        }
        return 1;
    }


    public boolean save(Path folderPath) {
        return this.save(folderPath, false);
    }

    public boolean save(Path folderPath, boolean isFailed) {
        if (!this.hasData) {
            Tracker.log(Level.ERROR, "Run data not gathered, will not save run");
            return false;
        }

        if (!Files.exists(folderPath)) {
            folderPath.toFile().mkdirs();
        }

        String data = JSONUtil.prettify(APIUtil.getRunJson(this));
        Path jsonPath = folderPath.resolve((isFailed ? "failed_" : "" ) + this.get("world_name").toString() + ".json");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonPath.toFile()));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

}
