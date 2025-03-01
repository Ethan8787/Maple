package ethan.maple;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

public final class Maple extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(net.md_5.bungee.api.ChatColor.RED + "Only players can use this command.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = this.getConfig();
        if (config.getBoolean("claimed." + player.getUniqueId(), false)) {
            return;
        }
        giveCompensationPackage(player);
        config.set("claimed." + player.getUniqueId(), true);
        saveConfig();
        player.sendMessage(ChatColor.AQUA + "補償 " + ChatColor.WHITE + "玩家 " + ChatColor.GOLD + player.getDisplayName() + ChatColor.WHITE + " 已領取補償裡包" + ChatColor.GRAY + "(請檢查背包)");
    }

    private void giveCompensationPackage(Player player) {
        ItemStack shulkerBox = new ItemStack(Material.LIGHT_BLUE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName("§6楓葉補償禮包");
        BlockState state = meta.getBlockState();
        if (state instanceof ShulkerBox) {
            ShulkerBox box = (ShulkerBox) state;
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
            meta.addStoredEnchant(enchantment, level, true); // 使用 addStoredEnchant 而非 addEnchant
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
