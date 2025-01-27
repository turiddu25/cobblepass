package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.google.gson.JsonObject;
import ca.landonjw.gooeylibs2.api.UIManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.cobblemon.mdks.cobblepass.data.Reward;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.BattlePassTier;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.cobblemon.mdks.cobblepass.util.Utils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ViewCommand extends Subcommand {
    public ViewCommand() {
        super("§9Usage: §3/battlepass view");
    }

    @Override
    public CommandNode<CommandSourceStack> build() {
        return Commands.literal("view")
            .executes(this::run)
            .build();
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendSystemMessage(
                Component.literal(Constants.ERROR_PREFIX + "This command must be run by a player!")
            );
            return 1;
        }

        ServerPlayer player = context.getSource().getPlayer();
        showBattlePassInfo(player);
        return 1;
    }

    private static String formatTimeRemaining(long milliseconds) {
        long seconds = milliseconds / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        
        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }

    private static List<Component> getRewardLore(BattlePassTier tier, boolean isPremium) {
        List<Component> lore = new ArrayList<>();
        if (isPremium) {
            lore.add(Component.literal("§6Premium Reward"));
        } else {
            lore.add(Component.literal("§aFree Reward"));
        }

        Reward reward = isPremium ? tier.getPremiumReward() : tier.getFreeReward();
        if (reward != null) {
            JsonObject data = reward.getData();
            switch (reward.getType()) {
                case ITEM:
                    if (data != null) {
                        String itemId = data.get("id").getAsString();
                        int count = data.has("Count") ? data.get("Count").getAsInt() : 1;
                        // Extract item name from ID (e.g., "minecraft:stone" -> "Stone")
                        String[] parts = itemId.split(":");
                        String itemName = parts[parts.length - 1];
                        itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
                        // Add mod name to lore if not minecraft
                        if (!parts[0].equals("minecraft")) {
                            String modName = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
                            lore.add(Component.literal("§8" + modName + " Item"));
                        }
                        lore.add(Component.literal("§7" + count + "x " + itemName));
                    } else {
                        lore.add(Component.literal("§7Item"));
                    }
                    break;
                case POKEMON:
                    lore.add(Component.literal("§7Pokemon"));
                    if (data != null) {
                        if (data.has("species")) {
                            lore.add(Component.literal(data.get("species").getAsString()));
                        }
                        if (data.has("level")) {
                            lore.add(Component.literal("§7Level: §f" + data.get("level").getAsInt()));
                        }
                        if (data.has("shiny") && data.get("shiny").getAsBoolean()) {
                            lore.add(Component.literal("§6✦ Shiny"));
                        }
                    }
                    break;
                case COMMAND:
                    if (data != null) {
                        if (data.has("display_name")) {
                            // Use custom display name if provided
                            lore.add(Component.literal("§7" + data.get("display_name").getAsString()));
                        } else if (data.has("id")) {
                            // Fall back to item ID if no display name
                            String itemId = data.get("id").getAsString();
                            String[] parts = itemId.split(":");
                            String itemName = parts[parts.length - 1];
                            itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
                            lore.add(Component.literal("§7" + itemName));
                        }
                    }
                    break;
            }
        }
        return lore;
    }

    private static List<Component> getPremiumRewardLore(BattlePassTier tier) {
        return getRewardLore(tier, true);
    }

    public static void showBattlePassInfo(ServerPlayer player) {
        PlayerBattlePass pass = CobblePass.battlePass.getPlayerPass(player);
        
        // Create info button showing level, XP and time remaining
        int currentXP = pass.getXP();
        int xpForNext = (int)(CobblePass.config.getXpPerLevel() * Math.pow(Constants.XP_MULTIPLIER, pass.getLevel() - 1));
        List<Component> infoLore = new ArrayList<>(Arrays.asList(
            Component.literal(String.format("§3Level: §f%d", pass.getLevel())),
            Component.literal(String.format("§3XP: §f%d§7/§f%d", currentXP, xpForNext))
        ));
        
        if (CobblePass.config.isSeasonActive()) {
            long timeLeft = CobblePass.config.getSeasonEndTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                infoLore.add(Component.literal("§3Time Remaining: §b" + formatTimeRemaining(timeLeft)));
            }
        }

        GooeyButton infoButton = GooeyButton.builder()
            .display(new ItemStack(Items.EXPERIENCE_BOTTLE))
            .with(DataComponents.CUSTOM_NAME, Component.literal("§bBattle Pass Progress"))
            .with(DataComponents.LORE, new ItemLore(infoLore))
            .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
            .build();

        // Create premium status button
        ItemStack premiumDisplay = pass.isPremium() ? new ItemStack(Items.GOLDEN_APPLE) : new ItemStack(Items.APPLE);
        List<Component> premiumLore = new ArrayList<>();
        if (pass.isPremium()) {
            if (CobblePass.config.isSeasonActive()) {
                premiumLore.add(Component.literal("§3Season " + CobblePass.config.getCurrentSeason()));
            } else {
                premiumLore.add(Component.literal("§cNo active season"));
            }
        } else {
            premiumLore.add(Component.literal("§cInactive"));
            premiumLore.add(Component.literal("§7Click to upgrade!"));
        }
        
        GooeyButton premiumButton = GooeyButton.builder()
            .display(premiumDisplay)
            .with(DataComponents.CUSTOM_NAME, Component.literal("§6Premium Status"))
            .with(DataComponents.LORE, new ItemLore(premiumLore))
            .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
            .onClick(action -> {
                if (!pass.isPremium()) {
                    player.sendSystemMessage(Component.literal("§7Use §f/battlepass premium §7to unlock premium rewards"));
                }
            })
            .build();

        // Create empty background button
        Button background = new PlaceholderButton();

        // Create base template
        ChestTemplate baseTemplate = ChestTemplate.builder(4)
            .fill(background) // Fill entire GUI with background
            .set(0, 4, infoButton)
            .set(0, 8, premiumButton)
            .build();

        // Create tier pairs (reward + status) for all tiers
        List<Button> rewardButtons = new ArrayList<>();
        List<Button> statusButtons = new ArrayList<>();
        Map<Integer, BattlePassTier> tiers = CobblePass.battlePass.getTiers();
        int totalTiers = tiers.size();
        
        for (int i = 1; i <= totalTiers; i++) {
            final int level = i;
            BattlePassTier tier = tiers.get(level);
            if (tier == null) continue;
            
            // Get display item for the tier
            boolean isPremiumTier = tier.hasPremiumReward();
            // Get display item for the tier
            ItemStack displayItem;
            if (isPremiumTier) {
                displayItem = tier.getPremiumRewardItem(player.level().registryAccess());
            } else {
                displayItem = tier.getFreeRewardItem(player.level().registryAccess());
            }
            
            // Use stone as fallback if no display item
            if (displayItem == null || displayItem.isEmpty()) {
                displayItem = new ItemStack(Items.STONE);
            }

            // Create reward button with tier-specific item
            Button rewardButton = GooeyButton.builder()
                .display(displayItem)
                .with(DataComponents.CUSTOM_NAME, Component.literal("§fLevel " + level + " Reward"))
                .with(DataComponents.LORE, new ItemLore(getRewardLore(tier, isPremiumTier)))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .onClick(action -> {
                    if (level > pass.getLevel()) {
                        player.sendSystemMessage(Component.literal(String.format(Constants.MSG_LEVEL_NOT_REACHED, level)));
                        return;
                    }

                    // Claim rewards
                        if (CobblePass.battlePass.claimReward(player, level, isPremiumTier)) {
                            player.sendSystemMessage(Component.literal(String.format(Constants.MSG_REWARD_CLAIM, level)));
                            showBattlePassInfo(player); // Refresh UI
                        }
                })
                .build();

            // Create status indicator with level number and status
            ItemStack statusGlass;
            List<Component> statusLore = new ArrayList<>();
            
            if (isPremiumTier && !pass.isPremium()) {
                statusGlass = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§cPremium Only"));
            } else if (level > pass.getLevel()) {
                statusGlass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§7Not Reached"));
            } else if ((isPremiumTier && pass.hasClaimedPremiumReward(level)) || 
                      (!isPremiumTier && pass.hasClaimedFreeReward(level))) {
                statusGlass = new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§6Claimed"));
            } else {
                statusGlass = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§aAvailable"));
            }

            Button statusButton = GooeyButton.builder()
                .display(statusGlass)
                .with(DataComponents.CUSTOM_NAME, Component.literal("§3Level " + level))
                .with(DataComponents.LORE, new ItemLore(statusLore))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .build();

            // Add premium check for premium tiers
            if (isPremiumTier) {
                // Use diamond as display item for premium rewards if no item available
                ItemStack premiumRewardDisplay = displayItem;
                if (premiumRewardDisplay == null || premiumRewardDisplay.isEmpty()) {
                    premiumRewardDisplay = new ItemStack(Items.DIAMOND);
                }
                
                rewardButton = GooeyButton.builder()
                    .display(premiumRewardDisplay)
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§6Level " + level + " Reward"))
                    .with(DataComponents.LORE, new ItemLore(getPremiumRewardLore(tier)))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .onClick(action -> {
                        if (!pass.isPremium()) {
                            player.sendSystemMessage(Component.literal(Constants.MSG_NOT_PREMIUM));
                            return;
                        }
                        // Claim premium reward
                        if (CobblePass.battlePass.claimReward(player, level, true)) {
                            player.sendSystemMessage(Component.literal("§aRewards claimed for level " + level + "!"));
                            showBattlePassInfo(player); // Refresh UI
                        }
                    })
                    .build();
            }

            rewardButtons.add(rewardButton);
            statusButtons.add(statusButton);
        }

        // Create pages with proper button placement
        List<LinkedPage> pages = new ArrayList<>();
        int buttonsPerPage = 9;
        int totalPages = (int) Math.ceil((double) totalTiers / buttonsPerPage);

        for (int pageNum = 0; pageNum < totalPages; pageNum++) {
            ChestTemplate template = ChestTemplate.builder(4)
                .fill(background)
                .set(0, 4, infoButton)
                .set(0, 8, premiumButton)
                .build();

            int startIdx = pageNum * buttonsPerPage;
            int endIdx = Math.min(startIdx + buttonsPerPage, totalTiers);

            // Place reward buttons in row 2
            for (int i = startIdx; i < endIdx; i++) {
                template.set(1, i - startIdx, rewardButtons.get(i));
            }

            // Place status buttons in row 3 (directly below rewards)
            for (int i = startIdx; i < endIdx; i++) {
                template.set(2, i - startIdx, statusButtons.get(i));
            }

            LinkedPage page = LinkedPage.builder()
                .template(template)
                .title("§3Battle Pass")
                .build();

            pages.add(page);
        }

        // Link pages together
        for (int i = 0; i < pages.size(); i++) {
            LinkedPage current = pages.get(i);
            if (i > 0) {
                current.setPrevious(pages.get(i - 1));
            }
            if (i < pages.size() - 1) {
                current.setNext(pages.get(i + 1));
            }

            // Add navigation buttons
            ChestTemplate template = (ChestTemplate) current.getTemplate();
            if (current.getPrevious() != null) {
                Button prevBtn = LinkedPageButton.builder()
                    .display(new ItemStack(Items.ARROW))
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§f← Previous Page"))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .linkType(LinkType.Previous)
                    .build();
                template.set(3, 0, prevBtn);
            }
            if (current.getNext() != null) {
                Button nextBtn = LinkedPageButton.builder()
                    .display(new ItemStack(Items.ARROW))
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§fNext Page →"))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .linkType(LinkType.Next)
                    .build();
                template.set(3, 8, nextBtn);
            }
        }

        // Open the first page
        UIManager.openUIForcefully(player, pages.get(0));
    }
}
