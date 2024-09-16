import com.cylorun.io.sheets.GoogleSheetsClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

public class SpreadSheetRunUploadTest {
    public static void main(String[] args) {
        HashMap<String, Object> data = new HashMap<>();

        data.put("stone_mined", "3");
        data.put("time_first_portal", "00:06:07");
        data.put("diamond_sword_crafted", "0");
        data.put("netherrack_mined", "0");
        data.put("seed", 8802654848425114236L);
        data.put("time_stronghold", "00:07:10");
        data.put("eyes_used", "13");
        data.put("jumps", "243");
        data.put("spawn_biome", "forest");
        data.put("deaths_total", "1");
        data.put("gravel_mined", "10");
        data.put("world_name", "wrseed");
        data.put("gold_dropped", 85);
        data.put("igt", "00:08:36");
        data.put("diamond_picks_crafted", "1");
        data.put("time_end", "00:07:40");
        data.put("time_second_portal", "00:07:05");
        data.put("iron_source", "Misc");
        data.put("rta", "00:09:01");
        data.put("ender_pearls_used", "12");
        data.put("time_bastion", "00:02:01");
        data.put("blazes_killed", "7");
        data.put("run_id", 1);
        data.put("gold_source", "Bastion");
        data.put("time_iron_pick", "");
        data.put("time_wood", "00:00:28");
        data.put("time_nether", "00:01:47");
        data.put("obsidian_placed", "20");
        data.put("date_played_est", "07/29/2024 19:13:11");
        data.put("time_fortress", "00:04:27");
        data.put("enter_type", "Magma Ravine");
        data.put("blaze_rods", "6");
        data.put("flint_picked_up", "2");

        data.put("recent_version","false");

        try {
            GoogleSheetsClient.appendRowTop(data);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
