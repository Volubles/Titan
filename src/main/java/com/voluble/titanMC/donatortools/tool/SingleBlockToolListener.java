package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.config.DonatorToolsSettings;
import com.voluble.titanMC.donatortools.drop.BlockDropService;
import com.voluble.titanMC.donatortools.drop.VanillaLoot;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import com.voluble.titanMC.donatortools.tool.bountiful.BountifulDropStrategy;
import com.voluble.titanMC.donatortools.tool.compressed.CompressedDropStrategy;
import com.voluble.titanMC.donatortools.tool.smelting.SmeltingDropStrategy;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SingleBlockToolListener implements Listener {

	private final DonatorToolRegistry tools;
	private final DonatorToolsSettings configuration;
	private final BlockDropService drops;
	private final Map<DonatorToolType, SingleBlockDropStrategy> strategies;
	private final Map<UUID, PendingBreak> pending = new HashMap<>();

	public SingleBlockToolListener(
		DonatorToolRegistry tools,
		DonatorToolsSettings configuration,
		BlockDropService drops,
		VanillaLoot vanillaLoot
	) {
		this.tools = Objects.requireNonNull(tools, "tools");
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.drops = Objects.requireNonNull(drops, "drops");
		Objects.requireNonNull(vanillaLoot, "vanillaLoot");
		EnumMap<DonatorToolType, SingleBlockDropStrategy> built =
			new EnumMap<>(DonatorToolType.class);
		built.put(DonatorToolType.SMELTING, new SmeltingDropStrategy());
		built.put(DonatorToolType.COMPRESSED, new CompressedDropStrategy());
		built.put(DonatorToolType.BOUNTIFUL, new BountifulDropStrategy(configuration, vanillaLoot));
		this.strategies = Map.copyOf(built);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void rememberTool(BlockBreakEvent event) {
		Player player = event.getPlayer();
		pending.remove(player.getUniqueId());
		if (player.getGameMode() == GameMode.CREATIVE) return;
		if (!configuration.current().allows(event.getBlock().getType())) return;
		ItemStack tool = player.getInventory().getItemInMainHand();
		DonatorToolType type = tools.identify(tool).orElse(null);
		if (type == null || type == DonatorToolType.EXPLOSIVE) return;
		pending.put(player.getUniqueId(), new PendingBreak(
			event.getBlock().getWorld().getUID(),
			event.getBlock().getX(),
			event.getBlock().getY(),
			event.getBlock().getZ(),
			type,
			tool.clone()
		));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockDrops(BlockDropItemEvent event) {
		Player player = event.getPlayer();
		PendingBreak prepared = pending.remove(player.getUniqueId());
		if (prepared == null || !prepared.matches(event)) return;
		SingleBlockDropStrategy strategy = strategies.get(prepared.type());
		if (strategy == null) return;
		SingleBlockDropContext context = new SingleBlockDropContext(
			event.getBlock(),
			event.getBlockState(),
			player,
			prepared.tool(),
			event.getItems().stream().map(Item::getItemStack).map(ItemStack::clone).toList()
		);
		SingleBlockDropResolution resolution = strategy.resolve(context).orElse(null);
		if (resolution == null) return;
		if (!drops.transform(event, ignored -> resolution.drops())) return;
		if (resolution.enchantedEffect()) spawnEffect(event.getBlock());
	}

	@EventHandler
	public void forgetPlayer(PlayerQuitEvent event) {
		pending.remove(event.getPlayer().getUniqueId());
	}

	private static void spawnEffect(Block block) {
		block.getWorld().spawnParticle(
			Particle.ENCHANT,
			block.getLocation().add(0.5, 0.5, 0.5),
			20,
			0.35,
			0.35,
			0.35,
			0.05
		);
	}

	private record PendingBreak(
		UUID worldId,
		int x,
		int y,
		int z,
		DonatorToolType type,
		ItemStack tool
	) {
		private boolean matches(BlockDropItemEvent event) {
			Block block = event.getBlock();
			return block.getWorld().getUID().equals(worldId)
				&& block.getX() == x
				&& block.getY() == y
				&& block.getZ() == z;
		}
	}
}
