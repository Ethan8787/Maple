package ethan.maple;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Maple extends JavaPlugin implements Listener, CommandExecutor {
    private final HashSet<UUID> sneakingPlayers = new HashSet<>();
    private final HashMap<UUID, Location> playerLocations = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        sneakingPlayers.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = this.getConfig();

        if (config.getBoolean("claimed." + player.getUniqueId(), false)) {
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "你的背包已滿，無法領取補償物品！");
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                giveCompensationPackage(player);
            }
        }, 40);

        config.set("claimed." + player.getUniqueId(), true);
        saveConfig();

        player.sendMessage(ChatColor.AQUA + "補償 " + ChatColor.WHITE + "玩家 " + ChatColor.GOLD + player.getDisplayName() + ChatColor.WHITE + " 已領取補償裡包" + ChatColor.GRAY + "(請檢查背包)");
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("claimedlist")) {
            FileConfiguration config = this.getConfig();
            Set<String> claimedPlayers = config.getConfigurationSection("claimed") != null ?
                    Objects.requireNonNull(config.getConfigurationSection("claimed")).getKeys(false) : new HashSet<>();

            if (claimedPlayers.isEmpty()) {
                sender.sendMessage(ChatColor.AQUA + "補償 " + ChatColor.YELLOW + "沒有玩家領取過補償禮包！");
                return true;
            }

            sender.sendMessage(ChatColor.AQUA + "補償 " + ChatColor.WHITE + "已領取玩家: ");
            for (String uuid : claimedPlayers) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                ChatColor statusColor = offlinePlayer.isOnline() ? ChatColor.GREEN : ChatColor.GRAY;
                String msg = statusColor + " • ";
                String players = ChatColor.WHITE + offlinePlayer.getName();
                sender.sendMessage(msg + players);
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (sneakingPlayers.contains(uuid)) {
            event.setCancelled(true);
            openCustomGUI(player);
        }
    }

    private void openCustomGUI(Player player) {
        Inventory menu = Bukkit.createInventory(null, InventoryType.CHEST, "選單");

        ItemStack enderChest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta ecMeta = enderChest.getItemMeta();
        assert ecMeta != null;
        ecMeta.setDisplayName("§7打開終界箱");
        enderChest.setItemMeta(ecMeta);
        menu.setItem(10, enderChest);

        ItemStack shop = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta shopMeta = shop.getItemMeta();
        assert shopMeta != null;
        shopMeta.setDisplayName("§a商城");
        shop.setItemMeta(shopMeta);
        menu.setItem(11, shop);

        ItemStack cmdTutorial = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta cmdMeta = cmdTutorial.getItemMeta();
        assert cmdMeta != null;
        cmdMeta.setDisplayName("§c指令教學");
        cmdTutorial.setItemMeta(cmdMeta);
        menu.setItem(12, cmdTutorial);

        ItemStack helper = new ItemStack(Material.CHEST);
        ItemMeta helperMeta = helper.getItemMeta();
        assert helperMeta != null;
        helperMeta.setDisplayName("§e輔助");
        helper.setItemMeta(helperMeta);
        menu.setItem(13, helper);

        ItemStack tool = new ItemStack(Material.IRON_AXE);
        ItemMeta toolMeta = tool.getItemMeta();
        assert toolMeta != null;
        toolMeta.setDisplayName("§d小工具");
        toolMeta.addEnchant(Enchantment.MENDING, 1, true);
        toolMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tool.setItemMeta(toolMeta);
        menu.setItem(14, tool);

        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerInfo.getItemMeta();
        assert skullMeta != null;
        skullMeta.setDisplayName("§b玩家資訊");
        skullMeta.setOwnerProfile(player.getPlayerProfile());
        skullMeta.setLore(List.of("§a(圖標用每個人自己的頭顱)"));
        playerInfo.setItemMeta(skullMeta);
        menu.setItem(15, playerInfo);

        ItemStack portal = new ItemStack(Material.END_PORTAL_FRAME);
        ItemMeta portalMeta = portal.getItemMeta();
        assert portalMeta != null;
        portalMeta.setDisplayName("§6傳送點選單");
        portal.setItemMeta(portalMeta);
        menu.setItem(16, portal);

        player.openInventory(menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!event.getView().getTitle().equals("選單")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName());

        switch (displayName) {
            case "打開終界箱":
                player.openInventory(player.getEnderChest());
                break;

            case "商城":
                break;

            case "指令教學":
                player.sendMessage("");
                break;

            case "輔助":
                break;

            case "小工具":
                break;

            case "玩家資訊":
                break;

            case "傳送點選單":
                break;
            default:
                break;
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerLocation(event.getPlayer());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            sneakingPlayers.add(player.getUniqueId());
        } else {
            sneakingPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (sneakingPlayers.contains(player.getUniqueId())) {
            playerLocations.put(player.getUniqueId(), player.getLocation());
            setSpectator(player);
            event.setCancelled(true);

            player.setMetadata("dropItem", new FixedMetadataValue(this, true));
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.removeMetadata("dropItem", this);
            }, 1L);
        }
    }

    public Location getPlayerLocation(Player player) {
        return playerLocations.get(player.getUniqueId());
    }

    public void removePlayerLocation(Player player) {
        playerLocations.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("dropItem")) {
            return;
        }

        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        if (sneakingPlayers.contains(player.getUniqueId())) {
            player.teleport(getPlayerLocation(player));
            setSpectator(player);
            event.setCancelled(true);
        }
    }

    private void setSpectator(Player player) {
        if (!(player.getGameMode() == GameMode.SURVIVAL)) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ChatColor.WHITE + "靈魂出竅" + ChatColor.GRAY +  " ▶ " + ChatColor.RED + "Off");
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.WHITE + "靈魂出竅" + ChatColor.GRAY +  " ▶ " + ChatColor.GREEN + "On");
        }
    }

    private void giveCompensationPackage(Player player) {
        ItemStack shulkerBox = new ItemStack(Material.LIGHT_BLUE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName("§6楓葉補償禮包");
        BlockState state = meta.getBlockState();
        if (state instanceof ShulkerBox box) {
            Inventory inv = box.getInventory();

            inv.setItem(0, createEnchantedBook(Enchantment.SILK_TOUCH, 1));
            inv.setItem(1, new ItemStack(Material.NETHERITE_INGOT, 1));
            inv.setItem(2, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
            inv.setItem(3, createEnchantedArmor(Material.DIAMOND_BOOTS));
            inv.setItem(4, new ItemStack(Material.GOLDEN_APPLE, 32));
            inv.setItem(5, createEnchantedArmor(Material.DIAMOND_LEGGINGS));
            inv.setItem(6, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
            inv.setItem(7, new ItemStack(Material.NETHERITE_INGOT, 1));
            inv.setItem(8, createEnchantedBook(Enchantment.SHARPNESS, 5));
            inv.setItem(9, createEnchantedBook(Enchantment.UNBREAKING, 3));
            inv.setItem(10, new ItemStack(Material.SPRUCE_LOG, 64));
            inv.setItem(11, new ItemStack(Material.TRIAL_KEY, 3));
            inv.setItem(12, new ItemStack(Material.COOKED_BEEF, 64));
            inv.setItem(13, new ItemStack(Material.HEAVY_CORE, 1));
            inv.setItem(14, new ItemStack(Material.COOKED_BEEF, 64));
            inv.setItem(15, new ItemStack(Material.OMINOUS_TRIAL_KEY, 3));
            inv.setItem(16, new ItemStack(Material.SPRUCE_LOG, 64));
            inv.setItem(17, createEnchantedBook(Enchantment.UNBREAKING, 3));
            inv.setItem(18, createEnchantedBook(Enchantment.PROTECTION, 4));
            inv.setItem(19, new ItemStack(Material.NETHERITE_INGOT, 1));
            inv.setItem(20, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
            inv.setItem(21, createEnchantedArmor(Material.DIAMOND_CHESTPLATE));
            inv.setItem(22, new ItemStack(Material.GOLDEN_APPLE, 32));
            inv.setItem(23, createEnchantedArmor(Material.DIAMOND_HELMET));
            inv.setItem(24, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
            inv.setItem(25, new ItemStack(Material.NETHERITE_INGOT, 1));
            inv.setItem(26, createEnchantedBook(Enchantment.FORTUNE, 3));
            box.update();
            meta.setBlockState(box);
        }
        shulkerBox.setItemMeta(meta);
        player.getInventory().addItem(shulkerBox);
    }

    private static ItemStack createEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(enchantment, level, true);
            book.setItemMeta(meta);
        }
        return book;
    }

    private static ItemStack createEnchantedArmor(Material material) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            armor.setItemMeta(meta);
        }
        return armor;
    }
}
