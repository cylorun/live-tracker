package com.cylorun.io.dto;

public class RunDTO {
    public int run_id;
    public long date_played_est;
    public long rta;
    public long igt;
    public String iron_source;
    public String enter_type;
    public String gold_source;
    public String spawn_biome;
    public String bastion_type;
    public String end_fight_type;
    public int gold_dropped;
    public int explosives_used;
    public int blaze_rods;
    public int blazes_killed;
    public int deaths_total;
    public int real_deaths;
    public int jumps;
    public int eyes_used;
    public int ender_pearls_used;
    public int diamond_picks_crafted;
    public int obsidian_placed;
    public int stone_mined;
    public int netherrack_mined;
    public int gravel_mined;
    public int flint_picked_up;
    public int sh_dist;
    public int sh_ring;
    public int frame_eyes;
    public long seed;
    public String world_name;
    public String color;
    public String notes;
    public Timelines timelines;
    public Barters barters;
    public Kills kills;
    public Foods foods;
    public Travel travel;

    @Override
    public String toString() {
        return "RunDTO{" +
                "run_id=" + run_id +
                ", date_played_est=" + date_played_est +
                ", rta=" + rta +
                ", igt=" + igt +
                ", iron_source='" + iron_source + '\'' +
                ", enter_type='" + enter_type + '\'' +
                ", gold_source='" + gold_source + '\'' +
                ", spawn_biome='" + spawn_biome + '\'' +
                ", bastion_type='" + bastion_type + '\'' +
                ", end_fight_type='" + end_fight_type + '\'' +
                ", gold_dropped=" + gold_dropped +
                ", blaze_rods=" + blaze_rods +
                ", blazes_killed=" + blazes_killed +
                ", explosives_used=" + explosives_used +
                ", deaths_total=" + deaths_total +
                ", real_deaths=" + real_deaths +
                ", jumps=" + jumps +
                ", eyes_used=" + eyes_used +
                ", ender_pearls_used=" + ender_pearls_used +
                ", diamond_picks_crafted=" + diamond_picks_crafted +
                ", obsidian_place=" + obsidian_placed +
                ", stone_mined=" + stone_mined +
                ", netherrack_mined=" + netherrack_mined +
                ", gravel_mined=" + gravel_mined +
                ", flint_picked_up=" + flint_picked_up +
                ", sh_dist=" + sh_dist +
                ", sh_ring=" + sh_ring +
                ", frame_eyes=" + frame_eyes +
                ", seed=" + seed +
                ", world_name='" + world_name + '\'' +
                ", color='" + color + '\'' +
                ", notes='" + notes + '\'' +
                ", timelines=" + timelines +
                ", barters=" + barters +
                ", kills=" + kills +
                ", foods=" + foods +
                ", travel=" + travel +
                '}';
    }
}
