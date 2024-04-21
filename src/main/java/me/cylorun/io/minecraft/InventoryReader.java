package me.cylorun.io.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.cylorun.io.minecraft.world.WorldFile;

import java.util.ArrayList;

public class InventoryReader extends ArrayList<InventoryItem> {

    private final WorldFile file;

    public InventoryReader(WorldFile file) {
        this.file = file;
    }

    public InventoryReader read() {
        NBTReader reader = new NBTReader(this.file.getLevelDatPath());
        this.clear();

        JsonArray inventory = JsonParser.parseString(reader.get(NBTReader.INVENTORY_PATH)).getAsJsonArray();

        for (JsonElement e : inventory) {
            JsonObject item = e.getAsJsonObject();
            String name = item.get("id").getAsJsonObject().get("value").getAsString();
            int count = item.get("Count").getAsJsonObject().get("value").getAsInt();
            InventoryItem invItem = new InventoryItem(name, count);

            this.add(invItem);
        }


        return this;
    }

}
