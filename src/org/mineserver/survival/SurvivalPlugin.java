package org.mineserver.survival;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

public class SurvivalPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final float TEMP_MIN          = 0f;
    private static final float TEMP_MAX          = 40f;
    private static final float TEMP_NORMAL       = 20f;
    private static final float TEMP_LERP         = 0.05f;
    private static final float TEMP_HYPO_DAMAGE  = 5f;
    private static final float TEMP_HYPO_EFFECT  = 10f;
    private static final float TEMP_HYPER_EFFECT = 30f;
    private static final float TEMP_HYPER_DAMAGE = 35f;
    private static final int   THIRST_MAX        = 20;

    private final Map<UUID, Float>   temperature   = new HashMap<>();
    private final Map<UUID, Integer> thirst        = new HashMap<>();
    private final Map<UUID, Integer> thirstTimer   = new HashMap<>();
    private final Map<UUID, Integer> tempDmgTimer  = new HashMap<>();
    private final Map<UUID, BossBar> bossBars      = new HashMap<>();
    private final Map<UUID, Long>    parasiteEnd   = new HashMap<>();
    private final Map<UUID, Integer> parasiteTimer = new HashMap<>();
    private final Map<UUID, Integer> iceMeltTimer  = new HashMap<>();
    private final Map<UUID, Material>activeIceType = new HashMap<>();
    private final Map<UUID, Long>    heaterToggleCd   = new HashMap<>();
    private final Map<UUID, Integer> heaterNotifyTimer= new HashMap<>();
    private final Set<UUID>          sprintLocked     = new HashSet<>();
    private org.bukkit.NamespacedKey HEATER_EXPIRY_KEY;
    // === Термос ===
    private org.bukkit.NamespacedKey THERMOS_SIPS_KEY;
    private org.bukkit.NamespacedKey THERMOS_DIRTY_KEY;
    private static final int THERMOS_CUSTOM_MODEL = 3001;
    private static final int THERMOS_MAX_SIPS = 8;
    private final Map<UUID, Long>      thermosDrinkCd     = new HashMap<>();
    private final Map<UUID, Long>      waterDrinkCd       = new HashMap<>();

    private static final String HEATER_NAME_1 = "\u00a76\u0413\u0440\u0435\u043b\u043a\u0430 I";
    private static final String HEATER_NAME_2 = "\u00a7e\u0413\u0440\u0435\u043b\u043a\u0430 II";
    private static final String HEATER_NAME_3 = "\u00a7c\u0413\u0440\u0435\u043b\u043a\u0430 III";
    private static final String BALANCE_BOOK_NAME    = "\u00a7b\u00a7l\u041a\u043d\u0438\u0433\u0430 \u0420\u0430\u0432\u043d\u043e\u0432\u0435\u0441\u0438\u044f";
    private static final String BALANCE_ENCHANT_TAG  = "\u00a7b\u041e\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0420\u0430\u0432\u043d\u043e\u0432\u0435\u0441\u0438\u044f";

    private static final String CLEAN_WATER_NAME = "\u00a7bОчищенная вода";
    private static final String DIRTY_WATER_NAME = "\u00a7cГрязная вода";
    private static final String RAIN_WATER_NAME  = "\u00a73Дождевая вода";

    /** Температуры биомов Terralith (из data/terralith/worldgen/biome/*.json). */
    private static final java.util.Map<String, Double> TERRALITH_TEMPS;
    static {
        java.util.Map<String, Double> m = new java.util.HashMap<>();
        m.put("alpha_islands",        0.70);  m.put("alpha_islands_winter",  0.00);
        m.put("alpine_grove",        -0.20);  m.put("alpine_highlands",      0.45);
        m.put("amethyst_canyon",      0.95);  m.put("amethyst_rainforest",   0.95);
        m.put("ancient_sands",        2.00);  m.put("arid_highlands",        1.60);
        m.put("ashen_savanna",        1.00);  m.put("basalt_cliffs",         0.50);
        m.put("birch_taiga",          0.22);  m.put("blooming_plateau",      0.50);
        m.put("blooming_valley",      0.70);  m.put("brushland",             1.20);
        m.put("bryce_canyon",         2.00);  m.put("caldera",               0.45);
        m.put("cloud_forest",         0.25);  m.put("cold_shrubland",        0.14);
        m.put("desert_canyon",        2.00);  m.put("desert_oasis",          2.00);
        m.put("desert_spires",        2.00);  m.put("emerald_peaks",         0.10);
        m.put("forested_highlands",   0.36);  m.put("fractured_savanna",     1.10);
        m.put("frozen_cliffs",        0.00);  m.put("glacial_chasm",         0.00);
        m.put("granite_cliffs",       0.40);  m.put("gravel_beach",          0.80);
        m.put("gravel_desert",        0.14);  m.put("haze_mountain",         0.30);
        m.put("highlands",            0.40);  m.put("hot_shrubland",         0.80);
        m.put("ice_marsh",            0.14);  m.put("jungle_mountains",      0.95);
        m.put("lavender_forest",      0.70);  m.put("lavender_valley",       0.70);
        m.put("lush_desert",          2.00);  m.put("lush_valley",           0.40);
        m.put("mirage_isles",         0.70);  m.put("moonlight_grove",       0.70);
        m.put("moonlight_valley",     0.70);  m.put("mountain_steppe",       0.40);
        m.put("orchid_swamp",         0.80);  m.put("painted_mountains",     1.00);
        m.put("red_oasis",            2.00);  m.put("rocky_jungle",          0.95);
        m.put("rocky_mountains",      0.30);  m.put("rocky_shrubland",       0.14);
        m.put("sakura_grove",         0.70);  m.put("sakura_valley",         0.70);
        m.put("sandstone_valley",     2.00);  m.put("savanna_badlands",      1.00);
        m.put("savanna_slopes",       1.00);  m.put("scarlet_mountains",     0.10);
        m.put("shield",               0.40);  m.put("shield_clearing",       0.40);
        m.put("shrubland",            1.20);  m.put("siberian_grove",        0.13);
        m.put("siberian_taiga",       0.13);  m.put("skylands",              0.50);
        m.put("skylands_autumn",      0.50);  m.put("skylands_spring",       0.50);
        m.put("skylands_summer",      0.50);  m.put("skylands_winter",       0.20);
        m.put("snowy_badlands",       0.00);  m.put("snowy_cherry_grove",    0.10);
        m.put("snowy_maple_forest",   0.10);  m.put("snowy_shield",          0.10);
        m.put("steppe",               0.40);  m.put("stony_spires",          0.70);
        m.put("temperate_highlands",  0.50);  m.put("tropical_jungle",       0.95);
        m.put("valley_clearing",      0.40);  m.put("volcanic_crater",       1.00);
        m.put("volcanic_peaks",       1.00);  m.put("warm_river",            0.50);
        m.put("warped_mesa",          2.00);  m.put("white_cliffs",          0.40);
        m.put("white_mesa",           2.00);  m.put("windswept_spires",      0.20);
        m.put("wintry_forest",       -0.50);  m.put("wintry_lowlands",      -0.50);
        m.put("yellowstone",          0.25);  m.put("yosemite_cliffs",       0.375);
        m.put("yosemite_lowlands",    0.25);
        // Пещерные биомы Terralith
        m.put("cave/andesite_caves",  1.00);  m.put("cave/crystal_caves",    1.00);
        m.put("cave/deep_caves",      0.60);  m.put("cave/desert_caves",     0.80);
        m.put("cave/diorite_caves",   1.00);  m.put("cave/frostfire_caves",  0.80);
        m.put("cave/fungal_caves",    1.00);  m.put("cave/granite_caves",    1.00);
        m.put("cave/ice_caves",       1.00);  m.put("cave/infested_caves",   1.00);
        m.put("cave/mantle_caves",    2.00);  m.put("cave/thermal_caves",    0.80);
        m.put("cave/tuff_caves",      1.00);  m.put("cave/underground_jungle",0.50);
        TERRALITH_TEMPS = java.util.Collections.unmodifiableMap(m);
    }

    /**
     * Возвращает ключ биома в формате "namespace:path" через NMS-рефлексию.
     * Работает на Mohist/Forge-гибридах где getBiome() возвращает null для мод-биомов.
     * Возвращает null при любой ошибке (graceful fallback).
     */
    private static String getBiomeResourceKey(org.bukkit.block.Block block) {
        try {
            Object serverLevel = block.getWorld().getClass()
                .getMethod("getHandle").invoke(block.getWorld());
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Object pos = blockPosClass
                .getConstructor(int.class, int.class, int.class)
                .newInstance(block.getX(), block.getY(), block.getZ());
            // LevelReader.getBiome(BlockPos) → Holder<Biome>
            Object holder = null;
            for (java.lang.reflect.Method m : serverLevel.getClass().getMethods()) {
                if ("getBiome".equals(m.getName()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(pos.getClass())) {
                    holder = m.invoke(serverLevel, pos);
                    break;
                }
            }
            if (holder == null) return null;
            // Holder.unwrapKey() → Optional<ResourceKey<Biome>>
            @SuppressWarnings("unchecked")
            java.util.Optional<Object> optKey = (java.util.Optional<Object>)
                holder.getClass().getMethod("unwrapKey").invoke(holder);
            if (!optKey.isPresent()) return null;
            Object resourceKey = optKey.get();
            // ResourceKey.location() → ResourceLocation
            Object loc = resourceKey.getClass().getMethod("location").invoke(resourceKey);
            String ns   = (String) loc.getClass().getMethod("getNamespace").invoke(loc);
            String path = (String) loc.getClass().getMethod("getPath").invoke(loc);
            return ns + ":" + path;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Возвращает температуру биома: для Terralith — из нашей таблицы (точные значения из JSON),
     * для остальных — стандартный Bukkit getTemperature().
     */
    private static double getBiomeTemperature(org.bukkit.block.Block block) {
        String key = getBiomeResourceKey(block);
        if (key != null && key.startsWith("terralith:")) {
            String path = key.substring("terralith:".length());
            Double t = TERRALITH_TEMPS.get(path);
            if (t != null) return t;
        }
        try { return block.getTemperature(); } catch (Exception ex) { return 0.8; }
    }

    @Override
    public void onEnable() {
        HEATER_EXPIRY_KEY = new org.bukkit.NamespacedKey(this, "heater_expires");
        THERMOS_SIPS_KEY  = new org.bukkit.NamespacedKey(this, "thermos_sips");
        THERMOS_DIRTY_KEY = new org.bukkit.NamespacedKey(this, "thermos_dirty");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tan").setExecutor(this);
        registerFurnaceRecipe();
        registerHeaterRecipes();
        registerThermosRecipes();
        for (Player p : Bukkit.getOnlinePlayers()) { initPlayer(p.getUniqueId()); createBossBar(p); }
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) tickPlayer(p);
                // Очистка предметов на земле — если грелка истекла, удалить
                for (World w : Bukkit.getWorlds()) {
                    for (Item item : w.getEntitiesByClass(Item.class)) {
                        ItemStack stack = item.getItemStack();
                        if (isHeater(stack)) {
                            long exp = getHeaterExpiry(stack);
                            if (exp > 0 && exp <= System.currentTimeMillis()) {
                                item.remove();
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        getLogger().info("ToughSurvival включён!");
    }

    @Override
    public void onDisable() {
        for (BossBar bar : bossBars.values()) bar.removeAll();
        bossBars.clear(); temperature.clear(); thirst.clear();
        thirstTimer.clear(); tempDmgTimer.clear(); parasiteEnd.clear(); parasiteTimer.clear();
        iceMeltTimer.clear(); activeIceType.clear();
        heaterToggleCd.clear(); heaterNotifyTimer.clear();
        thermosDrinkCd.clear();
        waterDrinkCd.clear();
        getLogger().info("ToughSurvival выключен.");
    }

    private void registerFurnaceRecipe() {
        org.bukkit.inventory.FurnaceRecipe recipe = new org.bukkit.inventory.FurnaceRecipe(
            new NamespacedKey(this, "clean_water"), makeCleanWaterBottle(), Material.POTION, 0f, 200);
        try { getServer().addRecipe(recipe); } catch (IllegalStateException ignored) {}
    }

    private void registerHeaterRecipes() {
        // Грелка I: железо + трут + кожа + ведро
        org.bukkit.inventory.ShapedRecipe r1 = new org.bukkit.inventory.ShapedRecipe(
            new NamespacedKey(this, "heater_1"), makeHeater(1));
        r1.shape("IFI", "LBL", "LLL");
        r1.setIngredient('I', Material.IRON_INGOT);
        r1.setIngredient('F', Material.FLINT_AND_STEEL);
        r1.setIngredient('L', Material.LEATHER);
        r1.setIngredient('B', Material.WATER_BUCKET);
        try { getServer().addRecipe(r1); } catch (IllegalStateException ignored) {}

        // Грелка II: золотой слиток (верх) + порошок + железо (бока/низ) + грелка I
        org.bukkit.inventory.ShapedRecipe r2 = new org.bukkit.inventory.ShapedRecipe(
            new NamespacedKey(this, "heater_2"), makeHeater(2));
        r2.shape("OZO", "IHI", "III");
        r2.setIngredient('O', Material.GOLD_INGOT);
        r2.setIngredient('Z', Material.BLAZE_POWDER);
        r2.setIngredient('I', Material.IRON_INGOT);
        r2.setIngredient('H', Material.IRON_INGOT); // валидация на грелку I — через PrepareItemCraftEvent
        try { getServer().addRecipe(r2); } catch (IllegalStateException ignored) {}

        // Грелка III: алмаз (верх) + порошок + золотой слиток (бока/низ) + грелка II
        org.bukkit.inventory.ShapedRecipe r3 = new org.bukkit.inventory.ShapedRecipe(
            new NamespacedKey(this, "heater_3"), makeHeater(3));
        r3.shape("DZD", "GHG", "GGG");
        r3.setIngredient('D', Material.DIAMOND);
        r3.setIngredient('Z', Material.BLAZE_POWDER);
        r3.setIngredient('G', Material.GOLD_INGOT);
        r3.setIngredient('H', Material.IRON_INGOT); // валидация на грелку II — через PrepareItemCraftEvent
        try { getServer().addRecipe(r3); } catch (IllegalStateException ignored) {}
    }

    private ItemStack makeHeater(int level) {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(switch (level) {
            case 2 -> HEATER_NAME_2;
            case 3 -> HEATER_NAME_3;
            default -> HEATER_NAME_1;
        });
        meta.setCustomModelData(1000 + level); // 1001 / 1002 / 1003
        meta.setLore(java.util.Arrays.asList(
            "\u00a77\u041dагрев: \u00a7c+" + (int)(heaterTempMod(level) * 1.75f) + "\u00b0C",
            "\u00a77\u0414лительность: \u00a7e" + heaterDuration(level) / 60 + " \u043cин",
            "\u00a78Shift+\u041f\u041a\u041c для активации"));
        item.setItemMeta(meta);
        return item;
    }

    // ======== ТЕРМОС: хелперы и регистрация рецептов ========

    private void registerThermosRecipes() {
        // Крафт пустого термоса: I W I / I B I / I I I
        org.bukkit.inventory.ShapedRecipe r = new org.bukkit.inventory.ShapedRecipe(
            new org.bukkit.NamespacedKey(this, "thermos"), makeThermos(0, false));
        r.shape("IWI", "IBI", "III");
        r.setIngredient('I', Material.IRON_INGOT);
        r.setIngredient('W', new org.bukkit.inventory.RecipeChoice.MaterialChoice(
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL));
        r.setIngredient('B', Material.BUCKET);
        try { getServer().addRecipe(r); } catch (IllegalStateException ignored) {}

        // Шаблон печи (ножницы → термос): реальная логика перехватывается в onFurnaceSmelt
        org.bukkit.inventory.FurnaceRecipe fr = new org.bukkit.inventory.FurnaceRecipe(
            new org.bukkit.NamespacedKey(this, "thermos_boil"),
            makeThermos(8, false), Material.SHEARS, 0f, 200);
        try { getServer().addRecipe(fr); } catch (IllegalStateException ignored) {}
    }

    private boolean isThermos(ItemStack item) {
        if (item == null || item.getType() != Material.SHEARS) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
            .has(THERMOS_SIPS_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
    }

    private int getThermosSips(ItemStack item) {
        if (!isThermos(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
            .getOrDefault(THERMOS_SIPS_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
    }

    private boolean isThermoDirty(ItemStack item) {
        if (!isThermos(item)) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .getOrDefault(THERMOS_DIRTY_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 0) == 1;
    }

    private ItemStack makeThermos(int sips, boolean dirty) {
        sips = Math.max(0, Math.min(THERMOS_MAX_SIPS, sips));
        ItemStack item = new ItemStack(Material.SHEARS);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7l\u0422\u0435\u0440\u043c\u043e\u0441");
        meta.setCustomModelData(THERMOS_CUSTOM_MODEL);
        meta.setUnbreakable(false);

        // Шкала заполненности через полоску прочности
        if (meta instanceof org.bukkit.inventory.meta.Damageable d) {
            int maxDur = Material.SHEARS.getMaxDurability(); // 238
            int dmg = (sips == 0) ? maxDur - 1
                : (int) Math.round((THERMOS_MAX_SIPS - sips) / (double) THERMOS_MAX_SIPS * (maxDur - 1));
            d.setDamage(dmg);
        }

        // Описание
        String sipCol = sips == 0 ? "\u00a7c" : sips <= 2 ? "\u00a7c" : sips <= 4 ? "\u00a7e" : "\u00a7a";
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("\u00a77\u0413\u043b\u043e\u0442\u043a\u043e\u0432: " + sipCol + sips + "\u00a77/\u00a7a" + THERMOS_MAX_SIPS);
        if (sips > 0 && dirty)
            lore.add("\u00a7c\u0412\u043e\u0434\u0430 \u043d\u0435 \u043a\u0438\u043f\u044f\u0447\u0451\u043d\u0430 \u2014 \u0440\u0438\u0441\u043a \u043f\u0430\u0440\u0430\u0437\u0438\u0442\u0430!");
        if (sips == 0)
            lore.add("\u00a78\u041d\u0430\u043f\u043e\u043b\u043d\u0438 \u0438\u0437 \u0432\u043e\u0434\u043e\u0451\u043c\u0430 (\u00a7c\u0433\u0440\u044f\u0437\u043d\u0430\u044f\u00a78) \u0438\u043b\u0438 \u043a\u043e\u0442\u043b\u0430 (\u00a7b\u0447\u0438\u0441\u0442\u0430\u044f\u00a78)");
        else
            lore.add("\u00a78\u041f\u041a\u041c \u0432 \u0432\u043e\u0437\u0434\u0443\u0445\u0435 \u2014 \u0432\u044b\u043f\u0438\u0442\u044c | \u041f\u041a\u041c \u043d\u0430 \u0432\u043e\u0434\u0443/\u043a\u043e\u0442\u0451\u043b \u2014 \u043d\u0430\u043f\u043e\u043b\u043d\u0438\u0442\u044c");

        // NBT
        meta.getPersistentDataContainer().set(THERMOS_SIPS_KEY,
            org.bukkit.persistence.PersistentDataType.INTEGER, sips);
        meta.getPersistentDataContainer().set(THERMOS_DIRTY_KEY,
            org.bukkit.persistence.PersistentDataType.BYTE, dirty ? (byte) 1 : (byte) 0);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeBalanceBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(BALANCE_BOOK_NAME);
        meta.setCustomModelData(2001);
        meta.setLore(java.util.Arrays.asList(
            "\u00a77Наложите на любую броню через наковальню",
            "\u00a77Броня будет держать §b20\u00b0C \u00a77в любом биоме"));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isBalanceBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return ChatColor.stripColor(meta.getDisplayName()).equals("\u041a\u043d\u0438\u0433\u0430 \u0420\u0430\u0432\u043d\u043e\u0432\u0435\u0441\u0438\u044f");
    }

    /** Проверяет, есть ли на предмете метка "Очарование Равновесия" в lore. */
    private boolean hasBalanceLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        return meta.getLore().stream().anyMatch(l ->
            ChatColor.stripColor(l).contains("\u041e\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0420\u0430\u0432\u043d\u043e\u0432\u0435\u0441\u0438\u044f"));
    }

    /** Труе если хотя бы один надетый бронедолемент несёт чары. */
    private boolean hasBalanceEnchant(Player p) {
        for (ItemStack piece : p.getEquipment().getArmorContents()) {
            if (hasBalanceLore(piece)) return true;
        }
        return false;
    }

    private boolean isHeater(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_INGOT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String n = ChatColor.stripColor(meta.getDisplayName());
        return n.startsWith("\u0413релка");
    }

    private int getHeaterLevel(ItemStack item) {
        if (!isHeater(item)) return 1;
        String n = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (n.contains("III")) return 3;
        if (n.contains("II"))  return 2;
        return 1;
    }

    /** Читает время истечения грелки из NBT предмета. Возвращает -1 если не активирована. */
    private long getHeaterExpiry(ItemStack item) {
        if (!isHeater(item)) return -1L;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1L;
        return meta.getPersistentDataContainer().getOrDefault(
            HEATER_EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG, -1L);
    }

    /** Записывает время истечения в NBT предмета. */
    private void setHeaterExpiry(ItemStack item, long expiry) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(
            HEATER_EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG, expiry);
        item.setItemMeta(meta);
    }

    /** Труе если грелка активирована и таймер ещё идёт. */
    private boolean isHeaterActive(ItemStack item) {
        long exp = getHeaterExpiry(item);
        return exp > 0 && exp > System.currentTimeMillis();
    }

    private float heaterTempMod(int level) {
        return switch (level) { case 2 -> 10f; case 3 -> 14f; default -> 8f; };
    }

    private int heaterDuration(int level) {
        return switch (level) { case 2 -> 1200; case 3 -> 2400; default -> 900; };
    }

    /** Помечает грелку как уже использованную (в lore). */
    private void markHeaterUsed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        java.util.List<String> lore = meta.hasLore() ?
            new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        lore.add("\u00a78[\u0418спользована]");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** Возвращает true если грелка уже была активирована. */
    private boolean isHeaterUsed(ItemStack item) {
        if (!isHeater(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        return meta.getLore().stream().anyMatch(l ->
            ChatColor.stripColor(l).contains("Использована"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeCleanWaterBottle() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setDisplayName(CLEAN_WATER_NAME);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeRainWaterBottle() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setDisplayName(RAIN_WATER_NAME);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeDirtyWaterBottle() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setDisplayName(DIRTY_WATER_NAME);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCleanWater(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        // stripColor: убираем цветовые коды перед сравнением (Mohist может хранить иначе)
        String n = ChatColor.stripColor(meta.getDisplayName());
        return n.equals(ChatColor.stripColor(CLEAN_WATER_NAME))
            || n.equals(ChatColor.stripColor(RAIN_WATER_NAME));
    }

    // Грязная вода = бутылка с именем DIRTY_WATER_NAME
    private boolean isDirtyWater(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (isCleanWater(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (meta.hasDisplayName()) {
            String n = ChatColor.stripColor(meta.getDisplayName());
            return n.equals(ChatColor.stripColor(DIRTY_WATER_NAME));
        }
        // Ванильная водяная бутылка без названия тоже считается грязной
        if (!(meta instanceof PotionMeta pm)) return false;
        try { return pm.getBasePotionData().getType() == PotionType.WATER; }
        catch (Exception ex) { return !pm.hasCustomEffects(); }
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        initPlayer(id);
        loadPlayerData(id);
        createBossBar(p);
        // Сразу скрываем бар если зашёл в креативе/спектаторе
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            BossBar bar = bossBars.get(id);
            if (bar != null) bar.setVisible(false);
        }
    }

    /** При смене геймода мгновенно прячем/показываем индикаторы. */
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        BossBar bar = bossBars.get(e.getPlayer().getUniqueId());
        if (bar == null) return;
        GameMode gm = e.getNewGameMode();
        bar.setVisible(gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        savePlayerData(p);
        UUID id = p.getUniqueId();
        temperature.remove(id); thirst.remove(id); thirstTimer.remove(id);
        tempDmgTimer.remove(id); parasiteEnd.remove(id); parasiteTimer.remove(id);
        iceMeltTimer.remove(id); activeIceType.remove(id);
        heaterNotifyTimer.remove(id); heaterToggleCd.remove(id);
        thermosDrinkCd.remove(id); waterDrinkCd.remove(id);
        BossBar bar = bossBars.remove(id);
        if (bar != null) bar.removeAll();
    }

    /** Игрок выбросил активную грелку — сообщаем в чат. */
    @EventHandler
    public void onDropHeater(PlayerDropItemEvent e) {
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (!isHeaterActive(dropped)) return;
        long secsLeft = (getHeaterExpiry(dropped) - System.currentTimeMillis()) / 1000;
        long mLeft = secsLeft / 60, sLeft = secsLeft % 60;
        e.getPlayer().sendMessage("\u00a78[\u00a77❄\u00a78] \u00a77Грелка выброшена \u2014 таймер идёт (осталось \u00a7e" + mLeft + " мин " + sLeft + " сек\u00a77)");
    }

    /** Игрок подобрал грелку — сообщаем в чат. */
    @EventHandler
    public void onPickupHeater(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack picked = e.getItem().getItemStack();
        if (!isHeater(picked)) return;
        long exp = getHeaterExpiry(picked);
        if (exp <= 0) return; // не активирована
        long secsLeft = (exp - System.currentTimeMillis()) / 1000;
        if (secsLeft <= 0) {
            p.sendMessage("\u00a78[\u00a7c\u274c\u00a78] \u00a7cГрелка подобрана, но она уже остыла");
        } else {
            long mLeft = secsLeft / 60, sLeft = secsLeft % 60;
            p.sendMessage("\u00a78[\u00a76\u2764\u00a78] \u00a76Грелка подобрана \u2014 осталось \u00a7e" + mLeft + " мин " + sLeft + " сек\u00a76, обогрев продолжается");
            // Инициализируем флаги уведомлений, пропуская уже прошедшие пороги —
            // иначе после подбора снова сработает "осталось 5 мин" при <1 мин
            UUID pid = p.getUniqueId();
            if (!heaterNotifyTimer.containsKey(pid)) {
                int flags = 0;
                if (secsLeft < 300) flags |= 1; // порог 5 мин уже позади
                if (secsLeft < 60)  flags |= 2; // порог 1 мин уже позади
                heaterNotifyTimer.put(pid, flags);
            }
        }
    }

    private void savePlayerData(Player p) {
        UUID id = p.getUniqueId();
        File file = new File(getDataFolder(), "players/" + id.toString() + ".yml");
        file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("temperature", (double) temperature.getOrDefault(id, TEMP_NORMAL));
        cfg.set("thirst", thirst.getOrDefault(id, THIRST_MAX));
        try { cfg.save(file); }
        catch (Exception ex) { getLogger().warning("Не удалось сохранить данные " + p.getName()); }
    }

    private void loadPlayerData(UUID id) {
        File file = new File(getDataFolder(), "players/" + id.toString() + ".yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.contains("temperature")) temperature.put(id, (float) cfg.getDouble("temperature"));
        if (cfg.contains("thirst"))      thirst.put(id, cfg.getInt("thirst"));
    }

    private void initPlayer(UUID id) {
        temperature.put(id, TEMP_NORMAL); thirst.put(id, THIRST_MAX);
        thirstTimer.put(id, 180); tempDmgTimer.put(id, 10); parasiteTimer.put(id, 9);
    }

    private void createBossBar(Player p) {
        BossBar bar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        bar.setProgress(1.0); bar.addPlayer(p);
        bossBars.put(p.getUniqueId(), bar);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        // Молоко и бутылки — обработаны через onInteract (совместимость с Mohist)
        if (item.getType() == Material.MILK_BUCKET
                || isCleanWater(item) || isDirtyWater(item)) {
            e.setCancelled(true); // предотвращаем ванильное двойное процессирование
        }
    }

    @EventHandler public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        // Грязная вода в печи → очищенная
        if (isDirtyWater(e.getSource())) { e.setResult(makeCleanWaterBottle()); return; }
        // Термос в печи → кипятим воду (очищаем)
        if (isThermos(e.getSource())) {
            e.setResult(makeThermos(getThermosSips(e.getSource()), false));
            return;
        }
        // Обычные ножницы нельзя жарить (попали через наш шаблонный рецепт)
        if (e.getSource().getType() == Material.SHEARS) e.setCancelled(true);
    }

    /** Получает ItemsAdder ID предмета из PersistentDataContainer, либо null. */
    private String getItemsAdderId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("itemsadder", "id");
        return item.getItemMeta().getPersistentDataContainer()
            .get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    /** Определяет IA-напиток по CustomModelData (фоллбэк если PDC недоступен на Mohist). */
    private String getIaDrinkIdByCmd(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return null;
        if (!item.getItemMeta().hasCustomModelData()) return null;
        switch (item.getItemMeta().getCustomModelData()) {
            case 10000: return "iasurvival:bloody_mary";
            case 10001: return "iasurvival:coffee";
            case 10002: return "iasurvival:cola";
            case 10003: return "iasurvival:hot_chocolate";
            default:    return null;
        }
    }

    /** Применяет эффекты IA-напитка игроку. */
    private void applyIaDrinkEffect(Player p, String id) {
        switch (id) {
            case "iasurvival:cola":
                addThirst(p, 5);
                msg(p, "\u00a7bГлоток колы. (+5)");
                break;
            case "iasurvival:hot_chocolate":
                addThirst(p, 5);
                msg(p, "\u00a76Горячий шоколад согрел жажду. (+5)");
                break;
            case "iasurvival:coffee":
                addThirst(p, 2);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, true, true, true));
                msg(p, "\u00a7eКофе немного утолил жажду. (+2)");
                break;
            case "iasurvival:bloody_mary":
                addThirst(p, 6);
                p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 0, true, true, true));
                msg(p, "\u00a7cКровавая Мери ударила в голову. (+6)");
                break;
        }
    }



    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        // Только основная рука — иначе событие стреляет дважды
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        // Shift+ПКМ пустой рукой — питьё из котла или водоёма
        if (p.isSneaking() && (item == null || item.getType() == Material.AIR)) {
            long _now = System.currentTimeMillis();
            if (_now - waterDrinkCd.getOrDefault(p.getUniqueId(), 0L) < 500L) return;
            waterDrinkCd.put(p.getUniqueId(), _now);

            // Котёл с водой — хранимая вода, чище чем водоём
            if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.WATER_CAULDRON) {
                e.setCancelled(true);
                org.bukkit.block.Block cauldron = e.getClickedBlock();
                org.bukkit.block.data.Levelled lvl =
                    (org.bukkit.block.data.Levelled) cauldron.getBlockData();
                int level = lvl.getLevel();
                if (level <= 1) {
                    cauldron.setType(Material.CAULDRON); // пустой котёл
                } else {
                    lvl.setLevel(level - 1);
                    cauldron.setBlockData(lvl);
                }
                addThirst(p, 6);
                if (Math.random() < 0.30) {
                    applyParasite(p);
                    msg(p, "\u00a7cВода в котле оказалась заражённой. (+6)");
                } else {
                    msg(p, "\u00a7bВы попили из котла. (+6)"
                        + (level <= 1 ? " \u00a77[Котёл пуст]" :
                           " \u00a77[Уровень: " + (level - 1) + "/3]"));
                }
                return;
            }

            // Водоём — грязная вода, 60% заражения
            boolean foundWater = false;
            if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.WATER) {
                foundWater = true;
            }
            if (!foundWater) {
                try {
                    @SuppressWarnings("deprecation")
                    org.bukkit.block.Block target = p.getTargetBlock(null, 4);
                    if (target != null && target.getType() == Material.WATER) foundWater = true;
                } catch (Exception ex) { /* fallback не поддерживается */ }
            }
            if (foundWater) {
                e.setCancelled(true);
                addThirst(p, 4);
                if (Math.random() < 0.60) {
                    applyParasite(p);
                    msg(p, "\u00a7cВода оказалась заражённой. (+4)");
                } else {
                    msg(p, "\u00a77Вы попили из водоёма... Пронесло. (+4)");
                }
                return;
            }
        }

        if (item == null) return;

        // === ВЕДРО МОЛОКА ===
        if (item.getType() == Material.MILK_BUCKET) {
            // Не отменяем — ванильная анимация 40 тиков, слот становится BUCKET
            e.setUseInteractedBlock(Event.Result.DENY);
            final int _milkSlot = p.getInventory().getHeldItemSlot();
            final Player _milkP = p;
            final UUID _milkUid = p.getUniqueId();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!_milkP.isOnline()) return;
                ItemStack _cur = _milkP.getInventory().getItem(_milkSlot);
                if (_cur != null && _cur.getType() == Material.BUCKET) {
                    addThirst(_milkP, 15);
                    parasiteEnd.remove(_milkUid);
                    msg(_milkP, "\u00a7fМолоко утолило жажду и очистило желудок! (+15)");
                }
            }, 42L);
            return;
        }

        // === БУТЫЛОЧКИ С ВОДОЙ ===
        if (item.getType() == Material.POTION) {
            if (isCleanWater(item)) {
                // Не отменяем — ванильная анимация 32 тика, слот → GLASS_BOTTLE
                e.setUseInteractedBlock(Event.Result.DENY);
                final int _wbSlot = p.getInventory().getHeldItemSlot();
                final Player _wbP = p;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (!_wbP.isOnline()) return;
                    ItemStack _cur = _wbP.getInventory().getItem(_wbSlot);
                    if (_cur != null && _cur.getType() == Material.GLASS_BOTTLE) {
                        addThirst(_wbP, 10);
                        msg(_wbP, "\u00a7bЧистая вода — жажда утолена. (+10)");
                    }
                }, 35L);
                return;
            }
            if (isDirtyWater(item)) {
                // Не отменяем — ванильная анимация 32 тика, слот → GLASS_BOTTLE
                e.setUseInteractedBlock(Event.Result.DENY);
                final int _dwSlot = p.getInventory().getHeldItemSlot();
                final Player _dwP = p;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (!_dwP.isOnline()) return;
                    ItemStack _cur = _dwP.getInventory().getItem(_dwSlot);
                    if (_cur != null && _cur.getType() == Material.GLASS_BOTTLE) {
                        addThirst(_dwP, 4);
                        if (Math.random() < 0.60) { applyParasite(_dwP); msg(_dwP, "\u00a7cВода оказалась заражённой. (+4)"); }
                        else msg(_dwP, "\u00a77Вода оказалась грязной, но пронесло. (+4)");
                    }
                }, 35L);
                return;
            }
            // IA-напитки (Кола, Кофе, Горячий шоколад, Кровавая Мери)
            // Паттерн как у обычной воды: ждём конца анимации, проверяем что предмет ушёл
            String _iaId = getItemsAdderId(item);
            if (_iaId == null) _iaId = getIaDrinkIdByCmd(item); // фоллбэк для Mohist
            if (_iaId != null && (_iaId.equals("iasurvival:cola") || _iaId.equals("iasurvival:coffee")
                    || _iaId.equals("iasurvival:hot_chocolate") || _iaId.equals("iasurvival:bloody_mary"))) {
                e.setUseInteractedBlock(Event.Result.DENY);
                final String _drinkId = _iaId;
                final int _iaSlot = p.getInventory().getHeldItemSlot();
                final Player _iaP = p;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (!_iaP.isOnline()) return;
                    ItemStack _cur = _iaP.getInventory().getItem(_iaSlot);
                    String _curId = getItemsAdderId(_cur);
                    if (_curId == null) _curId = getIaDrinkIdByCmd(_cur); // фоллбэк
                    if (!_drinkId.equals(_curId)) {
                        applyIaDrinkEffect(_iaP, _drinkId);
                    }
                }, 35L);
                return;
            }
            // Другие зелья — не трогаем
        }

        // ======== ТЕРМОС ========
        if (isThermos(item)) {
            // Блокируем ванильное действие ножниц
            e.setUseItemInHand(Event.Result.DENY);
            org.bukkit.block.Block tBlock = e.getClickedBlock();

            // ПКМ на котёл → заполнить чистой водой (2 глотка за уровень котла)
            if (tBlock != null && tBlock.getType() == Material.WATER_CAULDRON) {
                e.setUseInteractedBlock(Event.Result.DENY);
                int sips = getThermosSips(item);
                if (sips >= THERMOS_MAX_SIPS) { msg(p, "\u00a7e\u0422\u0435\u0440\u043c\u043e\u0441 \u0443\u0436\u0435 \u043f\u043e\u043b\u043d\u044b\u0439!"); return; }
                org.bukkit.block.data.Levelled cld =
                    (org.bukkit.block.data.Levelled) tBlock.getBlockData();
                int lvl = cld.getLevel();
                if (lvl <= 0) { msg(p, "\u00a7c\u041a\u043e\u0442\u0451\u043b \u043f\u0443\u0441\u0442!"); return; }
                int add = Math.min(2, THERMOS_MAX_SIPS - sips);
                int newSips = sips + add;
                if (lvl <= 1) tBlock.setType(Material.CAULDRON);
                else { cld.setLevel(lvl - 1); tBlock.setBlockData(cld); }
                p.getInventory().setItemInMainHand(makeThermos(newSips, false));
                msg(p, "\u00a7b\u0417\u0430\u043f\u043e\u043b\u043d\u0435\u043d\u043e \u0438\u0437 \u043a\u043e\u0442\u043b\u0430 (\u0447\u0438\u0441\u0442\u0430\u044f). \u0413\u043b\u043e\u0442\u043a\u043e\u0432: \u00a7e"
                    + newSips + "\u00a7b/" + THERMOS_MAX_SIPS);
                return;
            }

            // ПКМ на воду → заполнить грязной водой (с fallback через getTargetBlock)
            boolean tNearWater = (tBlock != null && tBlock.getType() == Material.WATER);
            if (!tNearWater) {
                try {
                    @SuppressWarnings("deprecation")
                    org.bukkit.block.Block tgt = p.getTargetBlock(null, 4);
                    if (tgt != null && tgt.getType() == Material.WATER) tNearWater = true;
                } catch (Exception ex) {}
            }
            if (tNearWater) {
                int sips = getThermosSips(item);
                if (sips >= THERMOS_MAX_SIPS) { msg(p, "\u00a7e\u0422\u0435\u0440\u043c\u043e\u0441 \u0443\u0436\u0435 \u043f\u043e\u043b\u043d\u044b\u0439!"); return; }
                p.getInventory().setItemInMainHand(makeThermos(THERMOS_MAX_SIPS, true));
                msg(p, "\u00a77\u0422\u0435\u0440\u043c\u043e\u0441 \u043d\u0430\u043f\u043e\u043b\u043d\u0435\u043d \u0438\u0437 \u0432\u043e\u0434\u043e\u0451\u043c\u0430. \u00a7c\u0412\u043e\u0434\u0430 \u043d\u0435 \u043a\u0438\u043f\u044f\u0447\u0451\u043d\u0430!");
                return;
            }

            // Shift+ПКМ под дождём в воздухе → +1 глоток чистый
            if (e.getAction() == Action.RIGHT_CLICK_AIR && p.isSneaking()
                    && p.getWorld().hasStorm()
                    && p.getWorld().getEnvironment() == World.Environment.NORMAL
                    && p.getLocation().getBlock().getLightFromSky() >= 12) {
                int sips = getThermosSips(item);
                if (sips >= THERMOS_MAX_SIPS) { msg(p, "\u00a7e\u0422\u0435\u0440\u043c\u043e\u0441 \u0443\u0436\u0435 \u043f\u043e\u043b\u043d\u044b\u0439!"); return; }
                p.getInventory().setItemInMainHand(makeThermos(sips + 1, false));
                msg(p, "\u00a73\u0414\u043e\u0436\u0434\u0435\u0432\u0430\u044f \u0432\u043e\u0434\u0430 +1 \u0433\u043b\u043e\u0442\u043e\u043a (\u0447\u0438\u0441\u0442\u0430\u044f). \u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: \u00a7e" + (sips + 1) + "\u00a73/" + THERMOS_MAX_SIPS);
                return;
            }

            // ВЫПИТЬ глоток — мгновенно со звуком
            int sips = getThermosSips(item);
            if (sips <= 0) {
                msg(p, "\u00a7c\u0422\u0435\u0440\u043c\u043e\u0441 \u043f\u0443\u0441\u0442! \u041d\u0430\u043f\u043e\u043b\u043d\u0438 \u0438\u0437 \u0432\u043e\u0434\u043e\u0451\u043c\u0430 \u0438\u043b\u0438 \u043a\u043e\u0442\u043b\u0430.");
                return;
            }
            UUID uid = p.getUniqueId();
            long now = System.currentTimeMillis();
            if (now - thermosDrinkCd.getOrDefault(uid, 0L) < 1700L) return;
            thermosDrinkCd.put(uid, now);
            boolean dirty = isThermoDirty(item);
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            addThirst(p, 4);
            if (dirty && Math.random() < 0.60) { applyParasite(p); msg(p, "\u00a7c\u0412\u043e\u0434\u0430 \u0432 \u0442\u0435\u0440\u043c\u043e\u0441\u0435 \u043e\u043a\u0430\u0437\u0430\u043b\u0430\u0441\u044c \u0437\u0430\u0440\u0430\u0436\u0451\u043d\u043d\u043e\u0439. (+4)"); }
            p.getInventory().setItemInMainHand(makeThermos(sips - 1, dirty));
            if (sips - 1 == 0) msg(p, "\u00a77\u0422\u0435\u0440\u043c\u043e\u0441 \u043e\u043f\u0443\u0441\u0442\u0435\u043b. \u041d\u0430\u043f\u043e\u043b\u043d\u0438 \u0438\u0437 \u0432\u043e\u0434\u043e\u0451\u043c\u0430 \u0438\u043b\u0438 \u043a\u043e\u0442\u043b\u0430.");
            else msg(p, "\u00a7b\u0413\u043b\u043e\u0442\u043e\u043a. \u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: \u00a7e" + (sips - 1) + "\u00a7b/" + THERMOS_MAX_SIPS);
            return;
        }

        // Shift+ПКМ с грелкой — активация/деактивация
        if (isHeater(item) && p.isSneaking()) {
            e.setCancelled(true);
            UUID hid = p.getUniqueId();
            long now = System.currentTimeMillis();
            // Запрет использования грелок на спавне и в Незере
            if (p.getWorld().getName().equalsIgnoreCase("SpawnWorld")) {
                msg(p, "\u00a7c\u041d\u0430 \u0441\u043f\u0430\u0432\u043d\u0435 \u0433\u0440\u0435\u043b\u043a\u0438 \u043d\u0435\u043b\u044c\u0437\u044f \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c.");
                return;
            }
            if (p.getWorld().getEnvironment() == World.Environment.NETHER) {
                msg(p, "\u00a7c\u0412 \u041d\u0435\u0437\u0435\u0440\u0435 \u0433\u0440\u0435\u043b\u043a\u0430 \u0431\u0435\u0441\u0441\u043c\u044b\u0441\u043b\u0435\u043d\u043d\u0430 \u2014 \u0437\u0434\u0435\u0441\u044c \u0438 \u0442\u0430\u043a \u0436\u0430\u0440\u043a\u043e.");
                return;
            }
            // Защита от двойного срабатывания
            if (now - heaterToggleCd.getOrDefault(hid, 0L) < 400) return;
            heaterToggleCd.put(hid, now);
            int lvl = getHeaterLevel(item);
            // Если в инвентаре уже есть другая активная грелка (не этот предмет) — блок
            for (ItemStack _cs : p.getInventory().getContents()) {
                if (_cs == null || _cs.equals(item)) continue;
                if (isHeater(_cs) && isHeaterActive(_cs)) {
                    long secsLeft = (getHeaterExpiry(_cs) - now) / 1000;
                    long mLeft = secsLeft / 60, sLeft = secsLeft % 60;
                    msg(p, "\u00a7eГрелка уже работает! Осталось: " + mLeft + " мин " + sLeft + " сек");
                    return;
                }
            }
            // Проверяем активна ли грелка по expiry, не по флагу [Использована]
            long exp = getHeaterExpiry(item);
            if (exp > 0 && exp > System.currentTimeMillis()) {
                // Уже активна — показываем сколько осталось
                long secsLeft = (exp - System.currentTimeMillis()) / 1000;
                long mLeft = secsLeft / 60, sLeft = secsLeft % 60;
                msg(p, "\u00a7eГрелка уже работает! Осталось: \u00a7a" + mLeft + " мин " + sLeft + " сек");
                return;
            }
            // Если время истекло — скажем что остыла
            if (exp > 0 && exp <= System.currentTimeMillis()) {
                msg(p, "\u00a7cЭта грелка уже остыла.");
                return;
            }
            // Запрет в жарком биоме
            double biomeT;
            biomeT = getBiomeTemperature(p.getLocation().getBlock());
            if (biomeT >= 1.0) {
                msg(p, "\u00a7cВ жарком биоме грелка бесполезна!");
                return;
            }
            // Активируем: записываем время истечения в NBT предмета
            long expiry = now + (long) heaterDuration(lvl) * 1000L;
            setHeaterExpiry(item, expiry);
            markHeaterUsed(item);
            p.getInventory().setItemInMainHand(item);
            heaterNotifyTimer.put(hid, 0);
            msg(p, "\u00a76Грелка " + lvl + " активирована! \u00a7eТаймер: " + heaterDuration(lvl) / 60 + " мин");
            return;
        }

        // Shift+ПКМ со стеклянной бутылкой — набрать воду (из дождя или водоёма)
        if (item.getType() == Material.GLASS_BOTTLE && p.isSneaking()) {
            boolean isRain = p.getWorld().hasStorm()
                && p.getWorld().getEnvironment() == World.Environment.NORMAL
                && p.getLocation().getBlock().getLightFromSky() >= 12;
            boolean nearWater = (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.WATER);
            if (!nearWater) {
                try {
                    @SuppressWarnings("deprecation")
                    org.bukkit.block.Block tgt = p.getTargetBlock(null, 4);
                    if (tgt != null && tgt.getType() == Material.WATER) nearWater = true;
                } catch (Exception ex) {}
            }
            if (isRain || nearWater) {
                e.setCancelled(true);
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                else p.getInventory().setItemInMainHand(item);
                if (isRain && !nearWater) {
                    p.getInventory().addItem(makeRainWaterBottle());
                    msg(p, "\u00a73Собрали дождевую воду. Пейте ПКМ (+10, чистая).");
                } else {
                    // 25% шанс попасть на чистый источник
                    if (Math.random() < 0.25) {
                        p.getInventory().addItem(makeCleanWaterBottle());
                        msg(p, "\u00a7bПовезло — источник оказался чистым! (+10)");
                    } else {
                        p.getInventory().addItem(makeDirtyWaterBottle());
                        msg(p, "\u00a7cСобрали воду из водоёма. Вскипятите в печи или пейте с риском (+4, 60% заражения).");
                    }
                }
                return;
            }
        }

        // Shift+ПКМ с пустым ведром под дождём — дождевая вода
        if (item.getType() == Material.BUCKET
                && p.isSneaking()
                && p.getWorld().hasStorm()
                && p.getWorld().getEnvironment() == World.Environment.NORMAL
                && p.getLocation().getBlock().getLightFromSky() >= 12) {
            e.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            else p.getInventory().setItemInMainHand(item);
            p.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
            msg(p, "\u00a73\u041d\u0430\u043f\u043e\u043b\u043d\u0438\u043b\u0438 \u0432\u0435\u0434\u0440\u043e \u0434\u043e\u0436\u0434\u0435\u0432\u043e\u0439 \u0432\u043e\u0434\u043e\u0439. \u0412\u044b\u043b\u0435\u0439 \u0432 \u043a\u043e\u0442\u0451\u043b \u0438\u043b\u0438 \u043d\u0430\u043f\u043e\u043b\u043d\u0438 \u0442\u0435\u0440\u043c\u043e\u0441.");
        }
    }

    private void addThirst(Player p, int amount) {
        UUID id = p.getUniqueId();
        thirst.put(id, Math.min(THIRST_MAX, thirst.getOrDefault(id, THIRST_MAX) + amount));
    }

    /** Админам показывает префикс [TAN], обычным игрокам — чистый текст. */
    private void msg(Player p, String text) {
        p.sendMessage(p.hasPermission("tan.admin") ? "\u00a78[TAN] " + text : text);
    }

    /** Удаляет бутылку из руки и возвращает пустую стеклянную бутылку. */
    private void consumeBottle(Player p, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        else p.getInventory().setItemInMainHand(item);
        p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
    }

    private void applyParasite(Player p) {
        parasiteEnd.put(p.getUniqueId(), System.currentTimeMillis() + 25_000L);
        msg(p, "\u00a7cВода оказалась заражённой!");
        p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 0, true, true, true));
    }

    @EventHandler
    public void onRegainHealth(org.bukkit.event.entity.EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getRegainReason() != org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED) return;
        UUID id = p.getUniqueId();
        float temp = temperature.getOrDefault(id, TEMP_NORMAL);
        int thirstVal = thirst.getOrDefault(id, THIRST_MAX);
        // Пока игрок в критической зоне (гипо/гипертермия или нулевая жажда) —
        // блокируем естественную регенерацию, чтобы нанесённый урон не лечился мгновенно.
        if (temp < TEMP_HYPO_DAMAGE || temp > TEMP_HYPER_DAMAGE || thirstVal <= 0) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent e) {
        if (e.isSprinting() && sprintLocked.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    private void tickPlayer(Player p) {
        UUID id = p.getUniqueId();
        // В креативе/спектаторе — скрываем индикаторы и пропускаем механику
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            BossBar bar = bossBars.get(id);
            if (bar != null && bar.isVisible()) bar.setVisible(false);
            return;
        }
        // Возвращаем бар если вернулся в сурвайвал
        BossBar bar0 = bossBars.get(id);
        if (bar0 != null && !bar0.isVisible()) bar0.setVisible(true);
        float temp    = temperature.getOrDefault(id, TEMP_NORMAL);
        float target  = calcTargetTemp(p);
        // В воде температура меняется в 4 раза быстрее (мгновенное погружение)
        float lerp = (p.isInWater() || p.isSwimming()) ? TEMP_LERP * 4f : TEMP_LERP;
        float newTemp = clamp(temp + (target - temp) * lerp, TEMP_MIN, TEMP_MAX);
        temperature.put(id, newTemp);

        int thirstVal = thirst.getOrDefault(id, THIRST_MAX);
        // Паразит — дренаж жажды строго раз в 3 секунды (отдельный таймер)
        if (parasiteEnd.getOrDefault(id, 0L) > System.currentTimeMillis()) {
            int pt = parasiteTimer.getOrDefault(id, 3);
            if (--pt <= 0) {
                thirstVal = Math.max(0, thirstVal - 1);
                thirst.put(id, thirstVal);
                pt = 9;
            }
            parasiteTimer.put(id, pt);
        }
        int drainInterval = calcThirstDrainInterval(p, newTemp);
        int timer = thirstTimer.getOrDefault(id, drainInterval);
        if (--timer <= 0) {
            if (thirstVal > 0) thirstVal--;
            thirst.put(id, thirstVal); timer = drainInterval;
        }
        thirstTimer.put(id, timer);
        // Жёсткая блокировка спринта — не даёт обойти эффект прыжками на месте
        boolean lockSprint = newTemp < TEMP_HYPO_DAMAGE || thirstVal <= 3;
        if (lockSprint) {
            sprintLocked.add(id);
            if (p.isSprinting()) p.setSprinting(false);
        } else {
            sprintLocked.remove(id);
        }
        applyTempEffects(p, newTemp, id);
        applyThirstEffects(p, thirstVal);
        updateBossBar(p, newTemp, thirstVal);
        tickIceMelt(p);
        // Грелки: истекшие — убираем; пороговые уведомления
        long _now2 = System.currentTimeMillis();
        org.bukkit.inventory.PlayerInventory _inv = p.getInventory();
        ItemStack[] _contents = _inv.getContents();
        long _minExpiry = Long.MAX_VALUE;
        boolean _hasActive = false;
        for (int _i = 0; _i < _contents.length; _i++) {
            ItemStack _s = _contents[_i];
            if (!isHeater(_s)) continue;
            long _exp = getHeaterExpiry(_s);
            if (_exp <= 0) continue;
            if (_exp <= _now2) {
                // Истекла — изъять
                int _el = getHeaterLevel(_s);
                String _lvlName = _el == 3 ? "III" : _el == 2 ? "II" : "I";
                _inv.setItem(_i, null);
                heaterNotifyTimer.remove(id);
                msg(p, "\u00a77Грелка " + _lvlName + " остыла — предмет удалён.");
            } else {
                _hasActive = true;
                if (_exp < _minExpiry) _minExpiry = _exp;
            }
        }
        // Пороговые уведомления: бит 0 = 5 мин, бит 1 = 1 мин
        if (_hasActive && _minExpiry != Long.MAX_VALUE) {
            long _secsLeft = (_minExpiry - _now2) / 1000;
            int _nt = heaterNotifyTimer.getOrDefault(id, 0);
            if (_secsLeft <= 300 && (_nt & 1) == 0) {
                heaterNotifyTimer.put(id, _nt | 1);
                long _mL = _secsLeft / 60, _sL = _secsLeft % 60;
                msg(p, "\u00a7e\u26a0 \u00a7eГрелка остывает \u2014 осталось " + _mL + " мин " + _sL + " сек");
            } else if (_secsLeft <= 60 && (_nt & 2) == 0) {
                heaterNotifyTimer.put(id, _nt | 2);
                long _mL = _secsLeft / 60, _sL = _secsLeft % 60;
                msg(p, "\u00a7c\u26a0 \u00a7cГрелка почти остыла \u2014 осталась \u00a7e" + _mL + " мин " + _sL + " сек\u00a7c!");
            }
        }
    }

    private float calcTargetTemp(Player p) {
        // Книга Равновесия проверяется ПЕРВОЙ — работает в любом измерении и биоме
        if (hasBalanceEnchant(p)) return TEMP_NORMAL;
        World.Environment env = p.getWorld().getEnvironment();
        int y = p.getLocation().getBlockY();
        float target;
        if (env == World.Environment.NETHER) {
            // Незер: базовая жара. Лёд, грелка и прочие модификаторы всё равно применяются.
            target = 37f;
        } else if (env == World.Environment.THE_END) {
            // Край: базовый холод. Грелка и лёд работают.
            target = 7f;
        } else if (y < 50) {
            target = (y < 0) ? clamp(20f + (-y) * 0.05f, 20f, 35f) : 15f;
        } else {
            double biomeT;
            biomeT = getBiomeTemperature(p.getLocation().getBlock());
            // Реалистичное масштабирование:
            //   t ≤ 1.0: base = t*16 + 2  (0→2°C, 0.25→6°C, 0.8→14.8°C, 1.0→18°C)
            //   t > 1.0: base = 18 + (t-1)*14  (1.6→26.4°C, 2.0→32°C)
            if (biomeT <= 1.0) {
                target = clamp((float)(biomeT * 16.0 + 2.0), TEMP_MIN, TEMP_MAX);
            } else {
                target = clamp(18f + (float)(biomeT - 1.0) * 14f, TEMP_MIN, TEMP_MAX);
            }

            // Проверяем покрытие 3×3 — нужно минимум 6/9 чтобы считалось укрытием
            int shelter = calcShelterScore(p);
            long time = p.getWorld().getTime();
            float dayNightMod = calcDayNightMod(time, biomeT);
            if (shelter >= 6) {
                // Дом без отопления: биомный климат сохраняется — пустыня остаётся жаркой,
                // тайга холодной. Стены поглощают 70% суточных колебаний (тепловая инерция).
                // scale = 0.67–1.0 в зависимости от полноты укрытия (6/9 – 9/9 блоков).
                float scale = shelter / 9f;
                target += dayNightMod * 0.30f * scale;
                // Минимальная защита: стены не дают опуститься ниже ~−4°C на экране.
                // В реальности кирпич/дерево удерживает несколько градусов даже в мороз.
                if (target < 6f) target = 6f;
                // Дождь снаружи — внутри не ощущается
            } else {
                // На улице: полный суточный цикл
                target += dayNightMod;
                // Дождь охлаждает только снаружи
                if (p.getWorld().hasStorm()) target -= 4f;
            }
        }
        target += scanNearbyBlocks(p);
        target += getArmorMod(p, target);
        target += getIceMod(p);
        // Грелка: находим лучшую активную в инвентаре
        UUID _pid = p.getUniqueId();
        float _bestHeat = 0f;
        for (ItemStack _hs : p.getInventory().getContents()) {
            if (isHeater(_hs) && isHeaterActive(_hs)) {
                float _mod = heaterTempMod(getHeaterLevel(_hs));
                if (_mod > _bestHeat) _bestHeat = _mod;
            }
        }
        if (_bestHeat > 0f) {
            // Кап: грелка не может поднять выше 28 внутр. (34°C на экране) — ниже порога гипертермии
            target = Math.min(target + _bestHeat, TEMP_HYPER_EFFECT - 2f);
        }
        // Кап температуры внутри помещения — только на поверхности обычного мира
        if (y >= 50 && env == World.Environment.NORMAL) {
            int _capShelter = calcShelterScore(p);
            if (_capShelter >= 6) {
                double _capBiomeT;
                _capBiomeT = getBiomeTemperature(p.getLocation().getBlock());
                // тёплые/жаркие биомы (равнины, пустыня): макс 33 внутр. = 42.75°C
                // умеренно-холодные (лес, берёза): макс 26 внутр. = 30.5°C
                // холодные (тайга, тундра): макс 30 внутр. = 37.5°C
                float _indoorCap = (float)_capBiomeT >= 0.8f ? 33f
                                 : (float)_capBiomeT >= 0.5f ? 26f
                                 : 22f;
                target = Math.min(target, _indoorCap);
            }
        }
        // Вода охлаждает ПОСЛЕ брони — эффективно снижает перегрев даже в металле
        if (p.isInWater() || p.isSwimming()) target -= 15f;
        return clamp(target, TEMP_MIN, TEMP_MAX);
    }

    /**
     * Температурный бонус по времени суток — 6 категорий биомов × 4 фазы.
     * Minecraft time: 0=рассвет, 6000=полдень, 12000=закат, 18000=полночь.
     *
     *  Категория   | t        | Примеры                    | Утро | День | Вечер | Ночь
     *  ЛЕДЯНОЙ     | < 0.15   | снег, ледяные шипы, wintry | -2   |  +1  |  -2   |  -6
     *  ХОЛОДНЫЙ    | 0.15-0.55| тайга, горы                | -1   |  +3  |  -1   |  -8
     *  УМЕРЕННЫЙ   | 0.55-0.90| лес, равнины               |  0   |  +4  |  +1   |  -7
     *  ТЁПЛЫЙ      | 0.90-1.30| джунгли, Terralith саванны |  +1  |  +5  |  +2   |  -6
     *  ЖАРКИЙ      | 1.30-1.80| arid_highlands, брасленд   |  +2  |  +8  |  +3   | -10
     *  РАСКАЛЁННЫЙ | > 1.80   | пустыня, ваниль-саванна    |  +3  | +10  |  +4   | -12
     *
     * biomeT — RAW температура (до умножения в °C).
     */
    private static float calcDayNightMod(long time, double biomeT) {
        boolean isDay     = time >= 5000  && time < 9500;
        boolean isEvening = time >= 9500  && time < 13000;
        boolean isNight   = time >= 13000 && time < 23000;
        // утро = всё остальное (23000..24000 + 0..5000)

        if (biomeT >= 1.8) {           // РАСКАЛЁННЫЙ: пустыня t=2.0, ваниль-саванна t=2.0
            if (isDay)     return  10f;
            if (isEvening) return   4f;
            if (isNight)   return -12f;
            return 3f;
        } else if (biomeT >= 1.3) {    // ЖАРКИЙ: arid_highlands t=1.6, brushland t=1.2→тёплый
            if (isDay)     return   8f;
            if (isEvening) return   3f;
            if (isNight)   return -10f;
            return 2f;
        } else if (biomeT >= 0.9) {    // ТЁПЛЫЙ: джунгли t=0.95, Terralith-саванны t=1.0-1.2
            if (isDay)     return   5f;
            if (isEvening) return   2f;
            if (isNight)   return  -6f;
            return 1f;
        } else if (biomeT >= 0.55) {   // УМЕРЕННЫЙ: лес t=0.7, равнины t=0.8, болото
            if (isDay)     return   4f;
            if (isEvening) return   1f;
            if (isNight)   return  -7f;
            return 0f;
        } else if (biomeT >= 0.15) {   // ХОЛОДНЫЙ: тайга t=0.25, горы t=0.3-0.4
            if (isDay)     return   3f;
            if (isEvening) return  -1f;
            if (isNight)   return  -8f;
            return -1f;
        } else {                       // ЛЕДЯНОЙ: снежные t=0.0, wintry t=-0.5
            if (isDay)     return   1f;
            if (isEvening) return  -2f;
            if (isNight)   return  -6f;
            return -2f;
        }
    }

    /**
     * Блоки в радиусе 3 с учётом расстояния до источника.
     * Костры и печи учитываются только если горят (Lightable.isLit()).
     */
    private float scanNearbyBlocks(Player p) {
        Location loc = p.getLocation();
        World world = loc.getWorld();
        if (world == null) return 0f;
        int px = loc.getBlockX(), py = loc.getBlockY(), pz = loc.getBlockZ();
        int radius = 3;
        float totalHeat = 0f;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    org.bukkit.block.Block block = world.getBlockAt(px+dx, py+dy, pz+dz);
                    Material type = block.getType();
                    float sourceHeat = 0f;

                    if (type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE) {
                        sourceHeat = 18f;
                    } else if (type == Material.MAGMA_BLOCK) {
                        sourceHeat = 12f;
                    } else if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
                        if (isLit(block)) sourceHeat = 14f;
                    } else if (type == Material.FURNACE || type == Material.BLAST_FURNACE || type == Material.SMOKER) {
                        if (isLit(block)) sourceHeat = 10f;
                    } else if (type == Material.TORCH || type == Material.WALL_TORCH) {
                        sourceHeat = 5f;
                    } else if (type == Material.LANTERN) {
                        sourceHeat = 4f;
                    } else if (type == Material.SOUL_LANTERN || type == Material.SOUL_TORCH || type == Material.SOUL_WALL_TORCH) {
                        sourceHeat = -1f;
                    } else if (type == Material.BLUE_ICE)    { sourceHeat = -4f;
                    } else if (type == Material.PACKED_ICE)  { sourceHeat = -3f;
                    } else if (type == Material.ICE)         { sourceHeat = -2f;
                    } else if (type == Material.SNOW_BLOCK || type == Material.POWDER_SNOW) { sourceHeat = -1.5f; }

                    if (sourceHeat != 0f) {
                        double dist = loc.distance(block.getLocation().add(0.5, 0.5, 0.5));
                        if (dist <= radius) {
                            float distFactor = (float)(1.0 - dist / (radius + 1));
                            totalHeat += sourceHeat * distFactor;
                        }
                    }
                }
            }
        }
        return clamp(totalHeat, -15f, 20f);
    }

    /**
     * Возвращает true если блок горит.
     * Сначала проверяем Lightable (Paper/Spigot).
     * Если не работает (Mohist/ArcLight Forge-гибрид) — fallback через getLightLevel().
     * Lit furnace emits light 13, lit campfire emits 15.
     */
    /**
     * Проверяет является ли материал элементом крыши.
     * Стекло и люки — да, листья и двери — нет.
     */
    private boolean isRoofBlock(Material mat) {
        if (mat == null || mat == Material.AIR) return false;
        String name = mat.name();
        if (name.contains("LEAVES"))  return false;
        if (name.contains("_DOOR") && !name.contains("TRAP")) return false;
        if (name.contains("GLASS"))    return true;
        if (name.contains("TRAPDOOR")) return true;
        return mat.isSolid();
    }

    /**
     * Оценка укрытия: проверяет 3×3 сетку столбцов над головой игрока.
     * Возвращает 0–9 (сколько столбцов из 9 имеют блок крыши вверху).
     */
    private int calcShelterScore(Player p) {
        World world = p.getWorld();
        if (world == null) return 0;
        int px = p.getEyeLocation().getBlockX();
        int py = p.getEyeLocation().getBlockY();
        int pz = p.getEyeLocation().getBlockZ();
        int covered = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 6; dy++) {
                    if (isRoofBlock(world.getBlockAt(px+dx, py+dy, pz+dz).getType())) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return covered;
    }

    /**
     * Определяет, находится ли игрок под крышей (здание, пещера).
     * Ищет непрозрачный (occluding) блок в 6 блоках над глазами игрока.
     * Работает корректно с дверями, стёклами и проёмами — в отличие от skyLight==0.
     */
    private boolean hasRoofAbove(Player p) {
        World world = p.getWorld();
        if (world == null) return false;
        int px = p.getEyeLocation().getBlockX();
        int py = p.getEyeLocation().getBlockY();
        int pz = p.getEyeLocation().getBlockZ();
        for (int dy = 1; dy <= 6; dy++) {
            if (isRoofBlock(world.getBlockAt(px, py + dy, pz).getType())) return true;
        }
        return false;
    }

    /** Крафт грелок II и III — валидация центрального слота (грелка нужного уровня). */
    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent e) {
        ItemStack[] m = e.getInventory().getMatrix();
        if (m.length < 9) return;

        // --- Книга Равновесия + любая броня → зачарованная броня ---
        ItemStack balBook = null, armorPiece = null;
        int filled = 0;
        for (ItemStack s : m) {
            if (s == null || s.getType() == Material.AIR) continue;
            filled++;
            if (isBalanceBook(s) && balBook == null) balBook = s;
            else if (isArmorPiece(s) && armorPiece == null) armorPiece = s;
        }
        if (balBook != null && armorPiece != null && filled == 2) {
            if (hasBalanceLore(armorPiece)) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            } else {
                ItemStack res = armorPiece.clone();
                ItemMeta rm = res.getItemMeta();
                java.util.List<String> rl = rm.hasLore()
                    ? new java.util.ArrayList<>(rm.getLore()) : new java.util.ArrayList<>();
                rl.add(0, BALANCE_ENCHANT_TAG);
                rm.setLore(rl);
                res.setItemMeta(rm);
                e.getInventory().setResult(res);
            }
            return;
        }

        // --- Апгрейд грелок ---
        ItemStack center = m[4];
        if (!isHeater(center) || isHeaterUsed(center)) { return; }
        int hLvl = getHeaterLevel(center);
        if (hLvl == 1) {
            // O Z O / I H I / I I I  (O=GOLD_INGOT, I=IRON_INGOT) → Грелка II
            if (is(m[0], Material.GOLD_INGOT) && is(m[1], Material.BLAZE_POWDER) && is(m[2], Material.GOLD_INGOT)
             && is(m[3], Material.IRON_INGOT) && is(m[5], Material.IRON_INGOT)
             && is(m[6], Material.IRON_INGOT) && is(m[7], Material.IRON_INGOT) && is(m[8], Material.IRON_INGOT))
                e.getInventory().setResult(makeHeater(2));
            else e.getInventory().setResult(new ItemStack(Material.AIR));
        } else if (hLvl == 2) {
            // D Z D / G H G / G G G  (D=DIAMOND, G=GOLD_INGOT) → Грелка III
            if (is(m[0], Material.DIAMOND) && is(m[1], Material.BLAZE_POWDER) && is(m[2], Material.DIAMOND)
             && is(m[3], Material.GOLD_INGOT) && is(m[5], Material.GOLD_INGOT)
             && is(m[6], Material.GOLD_INGOT) && is(m[7], Material.GOLD_INGOT) && is(m[8], Material.GOLD_INGOT))
                e.getInventory().setResult(makeHeater(3));
            else e.getInventory().setResult(new ItemStack(Material.AIR));
        } else {
            // Грелка III в центре — ничего не даёт
            e.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    /** Возвращает true если предмет является предметом брони. */
    private boolean isArmorPiece(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String n = item.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
            || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    private boolean is(ItemStack item, Material mat) {
        return item != null && item.getType() == mat && !isHeater(item);
    }

    /**
     * Фактическое взятие результата крафта — гарантированная валидация на ArcLight.
     * Проверяет: в центре должна лежать грелка нужного уровня.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getInventory().getResult();

        // --- Валидация: Книга Равновесия + броня ---
        if (isArmorPiece(result) && hasBalanceLore(result)) {
            ItemStack[] mx = e.getInventory().getMatrix();
            ItemStack book = null, armor = null;
            for (ItemStack s : mx) {
                if (s == null || s.getType() == Material.AIR) continue;
                if (isBalanceBook(s) && book == null) book = s;
                else if (isArmorPiece(s) && armor == null) armor = s;
            }
            if (book == null || armor == null || hasBalanceLore(armor)) {
                e.setCancelled(true);
                msg(p, "\u00a7cНеверный крафт.");
            }
            return;
        }

        // --- Валидация: апгрейд грелок ---
        if (!isHeater(result)) return;
        int targetLevel = getHeaterLevel(result);
        if (targetLevel < 2) return; // Грелка I крафтится обычным рецептом
        ItemStack center = e.getInventory().getMatrix()[4];
        int needed = targetLevel - 1;
        if (!isHeater(center) || getHeaterLevel(center) != needed) {
            e.setCancelled(true);
            msg(p, "\u00a7cВ центре должна быть Грелка " + needed + " уровня!");
        }
        // Если всё верно — обычная обработка, ингредиенты спишутся автоматически
    }

    private boolean isLit(org.bukkit.block.Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.Lightable l) {
            return l.isLit();
        }
        // Fallback для Mohist/ArcLight: горящий блок всегда испускает свет
        return block.getLightLevel() > 0;
    }

    private float getArmorMod(Player p, float currentTarget) {
        ItemStack[] armor = { p.getInventory().getHelmet(), p.getInventory().getChestplate(),
                              p.getInventory().getLeggings(), p.getInventory().getBoots() };
        float leatherPieces = 0, heavyPieces = 0;
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) continue;
            String name = piece.getType().name();
            if (name.contains("LEATHER")) leatherPieces++;
            else if (containsAny(name, "IRON","DIAMOND","NETHERITE","CHAINMAIL")) heavyPieces++;
        }
        float mod = 0f;
        if (leatherPieces > 0 && currentTarget < TEMP_NORMAL) mod += leatherPieces * 1.5f;
        if (heavyPieces   > 0 && currentTarget > TEMP_HYPER_EFFECT) mod += heavyPieces * 2f;
        return mod;
    }

    /**
     * Таяние льда в жарком биоме (biomeT >= 1.0).
     * Тает самый слабый тип первым: обычный (180с) → плотный (420с) → синий (720с).
     */
    private void tickIceMelt(Player p) {
        UUID id = p.getUniqueId();
        double biomeT;
        biomeT = getBiomeTemperature(p.getLocation().getBlock());

        if (biomeT < 1.0) {
            iceMeltTimer.remove(id); activeIceType.remove(id);
            return;
        }

        Material meltType = findWeakestIce(p);
        if (meltType == null) {
            iceMeltTimer.remove(id); activeIceType.remove(id);
            return;
        }

        int meltInterval = iceMeltInterval(meltType);

        // Сброс таймера при смене типа льда
        if (meltType != activeIceType.get(id)) {
            iceMeltTimer.put(id, meltInterval);
            activeIceType.put(id, meltType);
            return;
        }

        int timer = iceMeltTimer.getOrDefault(id, meltInterval);
        if (--timer <= 0) {
            consumeOneIce(p, meltType);
            int remaining = countIce(p, meltType);
            if (remaining > 0) {
                msg(p, "\u00a7e" + iceDisplayName(meltType) + " тает... (осталось " + remaining + ")");
            } else {
                msg(p, "\u00a7cВесь " + iceDisplayName(meltType).toLowerCase() + " растаял!");
            }
            timer = meltInterval;
        }
        iceMeltTimer.put(id, timer);
    }

    private Material findWeakestIce(Player p) {
        boolean hasIce = false, hasPacked = false, hasBlue = false;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item == null) continue;
            if (item.getType() == Material.ICE)        hasIce    = true;
            else if (item.getType() == Material.PACKED_ICE) hasPacked = true;
            else if (item.getType() == Material.BLUE_ICE)   hasBlue   = true;
        }
        if (hasIce)    return Material.ICE;
        if (hasPacked) return Material.PACKED_ICE;
        if (hasBlue)   return Material.BLUE_ICE;
        return null;
    }

    private int iceMeltInterval(Material type) {
        return switch (type) {
            case PACKED_ICE -> 420;
            case BLUE_ICE   -> 720;
            default         -> 180;
        };
    }

    private String iceDisplayName(Material type) {
        return switch (type) {
            case PACKED_ICE -> "Плотный лёд";
            case BLUE_ICE   -> "Синий лёд";
            default         -> "Обычный лёд";
        };
    }

    private void consumeOneIce(Player p, Material type) {
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == type) {
                contents[i].setAmount(contents[i].getAmount() - 1);
                if (contents[i].getAmount() <= 0) contents[i] = null;
                p.getInventory().setStorageContents(contents);
                return;
            }
        }
    }

    private int countIce(Player p, Material type) {
        int count = 0;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item != null && item.getType() == type) count += item.getAmount();
        }
        return count;
    }

    /**
     * Модификатор от льда в инвентаре.
     * Жаркий биом (biomeT >= 1.0): охлаждает (польза).
     * Холодный биом (biomeT <= 0.3): усиливает холод (штраф).
     * Берётся только самый мощный тип льда из инвентаря.
     */
    private float getIceMod(Player p) {
        boolean hasBlue = false, hasPacked = false, hasIce = false;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item == null) continue;
            switch (item.getType()) {
                case BLUE_ICE   -> hasBlue   = true;
                case PACKED_ICE -> hasPacked = true;
                case ICE        -> hasIce    = true;
                default -> {}
            }
        }
        if (!hasBlue && !hasPacked && !hasIce) return 0f;

        double biomeT;
        biomeT = getBiomeTemperature(p.getLocation().getBlock());

        if (biomeT >= 1.0) {
            // Жаркий биом — лёд охлаждает (польза)
            if (hasBlue)   return -16f;
            if (hasPacked) return -12f;
            return -9f;
        } else if (biomeT <= 0.3) {
            // Холодный биом — лёд усиливает холод (штраф)
            if (hasBlue)   return -8f;
            if (hasPacked) return -4f;
            return -2f;
        }
        return 0f;
    }

    private int calcThirstDrainInterval(Player p, float temp) {
        if (p.getWorld().getEnvironment() == World.Environment.NETHER) return 30;
        if (temp > 35f) return 30;
        // Биомная проверка ДО температурной — иначе temp>30 перекрывает её
        double biomeT = getBiomeTemperature(p.getLocation().getBlock());
        if (biomeT >= 1.8) return 30;  // пустыня — так же быстро как Незер
        if (biomeT >= 1.0) return 45;  // саванна — чуть медленнее
        if (temp > 30f) return 54;
        if (p.isSprinting() || p.isSwimming()) return 120;
        return 180;
    }

    @SuppressWarnings("deprecation")
    private void applyTempEffects(Player p, float temp, UUID id) {
        if (temp < TEMP_HYPO_DAMAGE) {
            // Гипотермия: замедление + усталость + расход голода + урон 1 hp каждые 2 сек
            addEffect(p, PotionEffectType.SLOW, 1);
            addEffect(p, PotionEffectType.SLOW_DIGGING, 0);
            p.setExhaustion(4f);
            applyTempDamage(p, id, 2, 1.0);
        } else if (temp < TEMP_HYPO_EFFECT) {
            addEffect(p, PotionEffectType.SLOW, 0);
        } else if (temp > TEMP_HYPER_DAMAGE) {
            // Гипертермия: тошнота + слепота + урон 1 hp каждые 2 сек
            addEffect(p, PotionEffectType.CONFUSION, 0);
            addEffect(p, PotionEffectType.BLINDNESS, 0);
            p.setExhaustion(4f);
            applyTempDamage(p, id, 2, 1.0);
        } else if (temp > TEMP_HYPER_EFFECT) {
            addEffect(p, PotionEffectType.WEAKNESS, 0);
        }
    }

    private void applyThirstEffects(Player p, int thirstVal) {
        if (thirstVal <= 0) {
            // Критическое обезвоживание: сильное головокружение + голод + расход голода + урон
            addEffect(p, PotionEffectType.CONFUSION, 1);
            addEffect(p, PotionEffectType.HUNGER, 0);
            p.setExhaustion(4f);
            if (Math.random() < 0.2) p.damage(1.0);
        } else if (thirstVal <= 3) {
            addEffect(p, PotionEffectType.CONFUSION, 0);
            addEffect(p, PotionEffectType.HUNGER, 0);
        } else if (thirstVal <= 6) {
            addEffect(p, PotionEffectType.HUNGER, 0);
        }
    }

    private void applyTempDamage(Player p, UUID id, int intervalSec, double amount) {
        int t = tempDmgTimer.getOrDefault(id, intervalSec);
        if (--t <= 0) { p.damage(amount); t = intervalSec; }
        tempDmgTimer.put(id, t);
    }

    private void addEffect(Player p, PotionEffectType type, int amplifier) {
        p.addPotionEffect(new PotionEffect(type, 40, amplifier, true, false, false));
    }

    private void updateBossBar(Player p, float temp, int thirstVal) {
        BossBar bar = bossBars.get(p.getUniqueId());
        if (bar == null) return;
        bar.setTitle(buildTempBar(temp) + "   " + buildThirstBar(thirstVal));
        if      (temp < TEMP_HYPO_DAMAGE  || temp > TEMP_HYPER_DAMAGE)  bar.setColor(BarColor.RED);
        else if (temp < TEMP_HYPO_EFFECT  || temp > TEMP_HYPER_EFFECT)  bar.setColor(BarColor.YELLOW);
        else                                                              bar.setColor(BarColor.GREEN);
    }

    private static final String ICON_THERMO = "\uEF00";
    private static final String ICON_DROP   = "\uEF01";

    private String buildTempBar(float temp) {
        int displayC = (int)(temp * 1.75f - 15f);
        String color, label;
        if      (temp < 5f)  { color = "\u00a79"; label = "Гипотермия"; }
        else if (temp < 10f) { color = "\u00a7b"; label = "Холодно";    }
        else if (temp < 17f) { color = "\u00a73"; label = "Прохладно";  }
        else if (temp <= 25f){ color = "\u00a7a"; label = "Комфорт";    }
        else if (temp <= 30f){ color = "\u00a7e"; label = "Тепло";      }
        else if (temp <= 35f){ color = "\u00a76"; label = "Жарко";      }
        else                 { color = "\u00a7c"; label = "Тепл.удар";  }
        return color + ICON_THERMO + " " + displayC + "\u00b0C \u00a78[" + color + label + "\u00a78]";
    }

    private String buildThirstBar(int thirstVal) {
        String fillColor = thirstVal <= 3 ? "\u00a7c" : thirstVal <= 8 ? "\u00a7e" : "\u00a7b";
        int filled = thirstVal / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < filled ? fillColor : "\u00a78").append(ICON_DROP);
        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("tan")) return false;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("\u00a7cТолько для игроков."); return true; }
            sendStatus(p, p); return true;
        }
        switch (args[0].toLowerCase()) {
            case "status" -> { Player t = resolveTarget(sender, args, 1); if (t != null) sendStatus(sender, t); }
            case "settemp" -> {
                if (!sender.hasPermission("tan.admin")) { noPerms(sender); return true; }
                if (args.length < 3) { sender.sendMessage("\u00a7c/tan settemp <ник> <0-40>"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { sender.sendMessage("\u00a7cИгрок не найден."); return true; }
                try {
                    float val = Float.parseFloat(args[2]);
                    if (val < 0 || val > 40) { sender.sendMessage("\u00a7cЗначение: 0-40."); return true; }
                    temperature.put(t.getUniqueId(), val);
                    sender.sendMessage("\u00a7aТемп. \u00a7e" + t.getName() + "\u00a7a = \u00a7e" + (int)(val*1.75f-15f) + "\u00b0C");
                } catch (NumberFormatException ex) { sender.sendMessage("\u00a7cНеверное число."); }
            }
            case "setthirst" -> {
                if (!sender.hasPermission("tan.admin")) { noPerms(sender); return true; }
                if (args.length < 3) { sender.sendMessage("\u00a7c/tan setthirst <ник> <0-20>"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { sender.sendMessage("\u00a7cИгрок не найден."); return true; }
                try {
                    int val = Integer.parseInt(args[2]);
                    if (val < 0 || val > 20) { sender.sendMessage("\u00a7cЗначение: 0-20."); return true; }
                    thirst.put(t.getUniqueId(), val);
                    sender.sendMessage("\u00a7aЖажда \u00a7e" + t.getName() + "\u00a7a = \u00a7e" + val + "/20");
                } catch (NumberFormatException ex) { sender.sendMessage("\u00a7cНеверное число."); }
            }
            case "give" -> {
                if (!sender.hasPermission("tan.admin")) { noPerms(sender); return true; }
                if (args.length < 3) { sender.sendMessage("\u00a7c/tan give <ник> cleanwater|rainwater"); return true; }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) { sender.sendMessage("\u00a7cИгрок не найден."); return true; }
                ItemStack item = switch (args[2].toLowerCase()) {
                    case "cleanwater" -> makeCleanWaterBottle();
                    case "rainwater"  -> makeRainWaterBottle();
                    case "heater1"      -> makeHeater(1);
                    case "heater2"      -> makeHeater(2);
                    case "heater3"      -> makeHeater(3);
                    case "balancebook"  -> makeBalanceBook();
                    default -> null;
                };
                if (item == null) { sender.sendMessage("\u00a7cТипы: cleanwater, rainwater, heater1, heater2, heater3, balancebook"); return true; }
                t.getInventory().addItem(item);
                sender.sendMessage("\u00a7aВыдано \u00a7e" + args[2] + " \u00a7aигроку \u00a7e" + t.getName());
            }
            case "reload" -> {
                if (!sender.hasPermission("tan.admin")) { noPerms(sender); return true; }
                for (Player p : Bukkit.getOnlinePlayers()) initPlayer(p.getUniqueId());
                sender.sendMessage("\u00a7a[TAN] Сброс выполнен.");
            }
            case "help", "?" -> sendHelp(sender);
            default -> sender.sendMessage("\u00a7cНеизвестная команда. \u00a7e/tan help");
        }
        return true;
    }

    private void sendStatus(CommandSender viewer, Player target) {
        UUID id = target.getUniqueId();
        float temp      = temperature.getOrDefault(id, TEMP_NORMAL);
        int   thirstVal = thirst.getOrDefault(id, THIRST_MAX);
        org.bukkit.block.Block _blk = target.getLocation().getBlock();
        double biomeT = getBiomeTemperature(_blk);
        String biomeKey = getBiomeResourceKey(_blk);
        String biomeName;
        if (biomeKey != null) {
            biomeName = biomeKey;
        } else {
            org.bukkit.block.Biome biome = _blk.getBiome();
            biomeName = biome != null ? biome.name() : "MODDED";
        }
        viewer.sendMessage("\u00a78==== \u00a76[TAN] \u00a77" + target.getName() + " \u00a78====");
        viewer.sendMessage("\u00a77Темп: " + buildTempBar(temp) + " \u00a78(" + String.format("%.1f",temp) + ")");
        viewer.sendMessage("\u00a77Жажда: " + buildThirstBar(thirstVal) + " \u00a78(" + thirstVal + "/20)");
        viewer.sendMessage("\u00a78Биом: \u00a77" + biomeName + " \u00a78(t="+String.format("%.2f",biomeT)+")");
        viewer.sendMessage("\u00a78Y: \u00a77" + target.getLocation().getBlockY());
        long pEnd = parasiteEnd.getOrDefault(id, 0L);
        if (pEnd > System.currentTimeMillis())
            viewer.sendMessage("\u00a7c!! Паразит: \u00a7e" + ((pEnd-System.currentTimeMillis())/1000L) + " сек");
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("\u00a78==== \u00a76[TAN] \u00a77Команды \u00a78====");
        s.sendMessage("\u00a7e/tan \u00a77-- своё состояние");
        s.sendMessage("\u00a7e/tan status [ник]");
        if (s.hasPermission("tan.admin")) {
            s.sendMessage("\u00a7e/tan settemp <ник> <0-40>");
            s.sendMessage("\u00a7e/tan setthirst <ник> <0-20>");
            s.sendMessage("\u00a7e/tan give <ник> cleanwater|rainwater");
            s.sendMessage("\u00a7e/tan reload");
        }
    }

    private Player resolveTarget(CommandSender sender, String[] args, int idx) {
        if (args.length > idx) {
            if (!sender.hasPermission("tan.admin")) { noPerms(sender); return null; }
            Player t = Bukkit.getPlayer(args[idx]);
            if (t == null) { sender.sendMessage("\u00a7cИгрок \u00a7e" + args[idx] + "\u00a7c не найден."); return null; }
            return t;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage("\u00a7cУкажи имя игрока."); return null;
    }

    private void noPerms(CommandSender s) { s.sendMessage("\u00a7cНедостаточно прав. \u00a7etan.admin"); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private static boolean containsAny(String str, String... keys) {
        for (String k : keys) if (str.contains(k)) return true;
        return false;
    }
}