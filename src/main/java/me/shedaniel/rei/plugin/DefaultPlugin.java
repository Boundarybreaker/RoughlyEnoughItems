/*
 * Roughly Enough Items by Danielshe.
 * Licensed under the MIT License.
 */

package me.shedaniel.rei.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.*;
import me.shedaniel.rei.client.ScreenHelper;
import me.shedaniel.rei.gui.RecipeViewingScreen;
import me.shedaniel.rei.gui.VillagerRecipeViewingScreen;
import me.shedaniel.rei.listeners.ContainerScreenHooks;
import me.shedaniel.rei.listeners.RecipeBookGuiHooks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.recipe.book.ClientRecipeBook;
import net.minecraft.container.CraftingContainer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.awt.*;
import java.util.List;
import java.util.*;

public class DefaultPlugin implements REIPluginEntry {
    
    public static final Identifier CRAFTING = new Identifier("minecraft", "plugins/crafting");
    public static final Identifier SMELTING = new Identifier("minecraft", "plugins/smelting");
    public static final Identifier SMOKING = new Identifier("minecraft", "plugins/smoking");
    public static final Identifier BLASTING = new Identifier("minecraft", "plugins/blasting");
    public static final Identifier CAMPFIRE = new Identifier("minecraft", "plugins/campfire");
    public static final Identifier STONE_CUTTING = new Identifier("minecraft", "plugins/stone_cutting");
    public static final Identifier BREWING = new Identifier("minecraft", "plugins/brewing");
    public static final Identifier PLUGIN = new Identifier("roughlyenoughitems", "default_plugin");
    public static final Identifier COMPOSTING = new Identifier("minecraft", "plugins/composting");
    private static final Identifier DISPLAY_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/display.png");
    private static final Identifier DISPLAY_TEXTURE_DARK = new Identifier("roughlyenoughitems", "textures/gui/display_dark.png");
    private static final List<DefaultBrewingDisplay> BREWING_DISPLAYS = Lists.newArrayList();
    
    public static Identifier getDisplayTexture() {
        return ScreenHelper.isDarkModeEnabled() ? DISPLAY_TEXTURE_DARK : DISPLAY_TEXTURE;
    }
    
    public static void registerBrewingDisplay(DefaultBrewingDisplay display) {
        BREWING_DISPLAYS.add(display);
    }
    
    @Override
    public Identifier getPluginIdentifier() {
        return PLUGIN;
    }
    
    @Override
    public void onFirstLoad(PluginDisabler pluginDisabler) {
        if (!RoughlyEnoughItemsCore.getConfigManager().getConfig().loadDefaultPlugin) {
            pluginDisabler.disablePluginFunction(PLUGIN, PluginFunction.REGISTER_ITEMS);
            pluginDisabler.disablePluginFunction(PLUGIN, PluginFunction.REGISTER_CATEGORIES);
            pluginDisabler.disablePluginFunction(PLUGIN, PluginFunction.REGISTER_RECIPE_DISPLAYS);
            pluginDisabler.disablePluginFunction(PLUGIN, PluginFunction.REGISTER_OTHERS);
        }
    }
    
    @Override
    public void registerItems(ItemRegistry itemRegistry) {
        Registry.ITEM.stream().forEach(item -> {
            itemRegistry.registerItemStack(item.getStackForRender());
            try {
                itemRegistry.registerItemStack(itemRegistry.getAllStacksFromItem(item));
            } catch (Exception e) {
            }
        });
        Registry.ENCHANTMENT.forEach(enchantment -> {
            for(int i = enchantment.getMinimumLevel(); i <= enchantment.getMaximumLevel(); i++) {
                Map<Enchantment, Integer> map = new HashMap<>();
                map.put(enchantment, i);
                ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.set(map, itemStack);
                itemRegistry.registerItemStack(Items.ENCHANTED_BOOK, itemStack);
            }
        });
    }
    
    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
        recipeHelper.registerCategory(new DefaultCraftingCategory());
        recipeHelper.registerCategory(new DefaultSmeltingCategory());
        recipeHelper.registerCategory(new DefaultSmokingCategory());
        recipeHelper.registerCategory(new DefaultBlastingCategory());
        recipeHelper.registerCategory(new DefaultCampfireCategory());
        recipeHelper.registerCategory(new DefaultStoneCuttingCategory());
        recipeHelper.registerCategory(new DefaultBrewingCategory());
        recipeHelper.registerCategory(new DefaultCompostingCategory());
    }
    
    @Override
    public void registerRecipeDisplays(RecipeHelper recipeHelper) {
        recipeHelper.registerRecipes(CRAFTING, ShapelessRecipe.class, DefaultShapelessDisplay::new);
        recipeHelper.registerRecipes(CRAFTING, ShapedRecipe.class, DefaultShapedDisplay::new);
        recipeHelper.registerRecipes(SMELTING, SmeltingRecipe.class, DefaultSmeltingDisplay::new);
        recipeHelper.registerRecipes(SMOKING, SmokingRecipe.class, DefaultSmokingDisplay::new);
        recipeHelper.registerRecipes(BLASTING, BlastingRecipe.class, DefaultBlastingDisplay::new);
        recipeHelper.registerRecipes(CAMPFIRE, CampfireCookingRecipe.class, DefaultCampfireDisplay::new);
        recipeHelper.registerRecipes(STONE_CUTTING, StonecuttingRecipe.class, DefaultStoneCuttingDisplay::new);
        BREWING_DISPLAYS.stream().forEachOrdered(display -> recipeHelper.registerDisplay(BREWING, display));
        List<ItemStack> arrowStack = Collections.singletonList(Items.ARROW.getStackForRender());
        RoughlyEnoughItemsCore.getItemRegisterer().getItemList().stream().filter(stack -> stack.getItem().equals(Items.LINGERING_POTION)).forEach(stack -> {
            List<List<ItemStack>> input = new ArrayList<>();
            for(int i = 0; i < 4; i++)
                input.add(arrowStack);
            input.add(Collections.singletonList(stack));
            for(int i = 0; i < 4; i++)
                input.add(arrowStack);
            ItemStack outputStack = new ItemStack(Items.TIPPED_ARROW, 8);
            PotionUtil.setPotion(outputStack, PotionUtil.getPotion(stack));
            PotionUtil.setCustomPotionEffects(outputStack, PotionUtil.getCustomPotionEffects(stack));
            List<ItemStack> output = Collections.singletonList(outputStack);
            recipeHelper.registerDisplay(CRAFTING, new DefaultCustomDisplay(input, output));
        });
        Map<ItemConvertible, Float> map = Maps.newLinkedHashMap();
        if (ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.isEmpty())
            ComposterBlock.registerDefaultCompostableItems();
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.keySet().forEach(itemConvertible -> {
            float chance = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.get(itemConvertible);
            if (chance > 0)
                map.put(itemConvertible, chance);
        });
        List<ItemConvertible> stacks = new LinkedList<>(map.keySet());
        stacks.sort((first, second) -> {
            return (int) ((map.get(first) - map.get(second)) * 100);
        });
        for(int i = 0; i < stacks.size(); i += MathHelper.clamp(48, 1, stacks.size() - i)) {
            List<ItemConvertible> thisStacks = Lists.newArrayList();
            for(int j = i; j < i + 48; j++)
                if (j < stacks.size())
                    thisStacks.add(stacks.get(j));
            recipeHelper.registerDisplay(COMPOSTING, new DefaultCompostingDisplay(MathHelper.floor(i / 48f), thisStacks, map, Lists.newArrayList(map.keySet()), new ItemStack[]{new ItemStack(Items.BONE_MEAL)}));
        }
    }
    
    @Override
    public void registerBounds(DisplayHelper displayHelper) {
        displayHelper.getBaseBoundsHandler().registerExclusionZones(AbstractContainerScreen.class, isOnRightSide -> {
            if (isOnRightSide || !MinecraftClient.getInstance().player.getRecipeBook().isGuiOpen() || !(MinecraftClient.getInstance().currentScreen instanceof RecipeBookProvider) || !(ScreenHelper.getLastContainerScreen().getContainer() instanceof CraftingContainer))
                return Collections.emptyList();
            ContainerScreenHooks screenHooks = ScreenHelper.getLastContainerScreenHooks();
            List<Rectangle> l = Lists.newArrayList(new Rectangle(screenHooks.rei_getContainerLeft() - 4 - 145, screenHooks.rei_getContainerTop(), 4 + 145 + 30, screenHooks.rei_getContainerHeight()));
            int size = ClientRecipeBook.getGroupsForContainer((CraftingContainer) ScreenHelper.getLastContainerScreen().getContainer()).size();
            if (size > 0)
                l.add(new Rectangle(screenHooks.rei_getContainerLeft() - 4 - 145 - 30, screenHooks.rei_getContainerTop(), 30, (size - 1) * 27));
            return l;
        });
        displayHelper.registerBoundsHandler(new DisplayHelper.DisplayBoundsHandler<AbstractContainerScreen>() {
            @Override
            public Class getBaseSupportedClass() {
                return AbstractContainerScreen.class;
            }
            
            @Override
            public Rectangle getLeftBounds(AbstractContainerScreen screen) {
                return new Rectangle(2, 0, ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft() - 4, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public Rectangle getRightBounds(AbstractContainerScreen screen) {
                int startX = ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft() + ScreenHelper.getLastContainerScreenHooks().rei_getContainerWidth() + 2;
                return new Rectangle(startX, 0, MinecraftClient.getInstance().window.getScaledWidth() - startX - 2, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public float getPriority() {
                return -1.0f;
            }
        });
        displayHelper.registerBoundsHandler(new DisplayHelper.DisplayBoundsHandler<RecipeViewingScreen>() {
            @Override
            public Class getBaseSupportedClass() {
                return RecipeViewingScreen.class;
            }
            
            @Override
            public Rectangle getLeftBounds(RecipeViewingScreen screen) {
                return new Rectangle(2, 0, ((RecipeViewingScreen) screen).getBounds().x - 4, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public Rectangle getRightBounds(RecipeViewingScreen screen) {
                int startX = ((RecipeViewingScreen) screen).getBounds().x + ((RecipeViewingScreen) screen).getBounds().width + 2;
                return new Rectangle(startX, 0, MinecraftClient.getInstance().window.getScaledWidth() - startX - 2, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public float getPriority() {
                return -1.0f;
            }
        });
        displayHelper.registerBoundsHandler(new DisplayHelper.DisplayBoundsHandler<VillagerRecipeViewingScreen>() {
            @Override
            public Class getBaseSupportedClass() {
                return VillagerRecipeViewingScreen.class;
            }
            
            @Override
            public Rectangle getLeftBounds(VillagerRecipeViewingScreen screen) {
                return new Rectangle(2, 0, ((VillagerRecipeViewingScreen) screen).bounds.x - 4, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public Rectangle getRightBounds(VillagerRecipeViewingScreen screen) {
                int startX = ((VillagerRecipeViewingScreen) screen).bounds.x + ((VillagerRecipeViewingScreen) screen).bounds.width + 2;
                return new Rectangle(startX, 0, MinecraftClient.getInstance().window.getScaledWidth() - startX - 2, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public float getPriority() {
                return -1.0f;
            }
        });
        displayHelper.registerBoundsHandler(new DisplayHelper.DisplayBoundsHandler<CreativeInventoryScreen>() {
            @Override
            public Class getBaseSupportedClass() {
                return CreativeInventoryScreen.class;
            }
            
            @Override
            public Rectangle getLeftBounds(CreativeInventoryScreen screen) {
                return new Rectangle(2, 0, ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft() - 2, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public Rectangle getRightBounds(CreativeInventoryScreen screen) {
                int startX = ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft() + ScreenHelper.getLastContainerScreenHooks().rei_getContainerWidth();
                return new Rectangle(startX, 0, MinecraftClient.getInstance().window.getScaledWidth() - startX - 2, MinecraftClient.getInstance().window.getScaledHeight());
            }
            
            @Override
            public Rectangle getItemListArea(Rectangle rectangle) {
                return new Rectangle(rectangle.x + 1, rectangle.y + 24, rectangle.width - 2, rectangle.height - (RoughlyEnoughItemsCore.getConfigManager().getConfig().sideSearchField ? 27 + 22 : 27));
            }
            
            @Override
            public float getPriority() {
                return -0.9f;
            }
        });
    }
    
    @Override
    public void registerOthers(RecipeHelper recipeHelper) {
        recipeHelper.registerWorkingStations(CRAFTING, new ItemStack(Items.CRAFTING_TABLE));
        recipeHelper.registerWorkingStations(SMELTING, new ItemStack(Items.FURNACE));
        recipeHelper.registerWorkingStations(SMOKING, new ItemStack(Items.SMOKER));
        recipeHelper.registerWorkingStations(BLASTING, new ItemStack(Items.BLAST_FURNACE));
        recipeHelper.registerWorkingStations(CAMPFIRE, new ItemStack(Items.CAMPFIRE));
        recipeHelper.registerWorkingStations(BREWING, new ItemStack(Items.BREWING_STAND));
        recipeHelper.registerWorkingStations(STONE_CUTTING, new ItemStack(Items.STONECUTTER));
        recipeHelper.registerWorkingStations(COMPOSTING, new ItemStack(Items.COMPOSTER));
        recipeHelper.registerSpeedCraftButtonArea(COMPOSTING, bounds -> null);
        recipeHelper.registerRecipeVisibilityHandler(new DisplayVisibilityHandler() {
            @Override
            public DisplayVisibility handleDisplay(RecipeCategory category, RecipeDisplay display) {
                return DisplayVisibility.ALWAYS_VISIBLE;
            }
            
            @Override
            public float getPriority() {
                return -1f;
            }
        });
        recipeHelper.registerSpeedCraftButtonArea(DefaultPlugin.CAMPFIRE, bounds -> new Rectangle((int) bounds.getMaxX() - 16, bounds.y + 6, 10, 10));
        recipeHelper.registerSpeedCraftFunctional(DefaultPlugin.CRAFTING, new SpeedCraftFunctional<DefaultCraftingDisplay>() {
            @Override
            public Class[] getFunctioningFor() {
                return new Class[]{InventoryScreen.class, CraftingTableScreen.class};
            }
            
            @Override
            public boolean performAutoCraft(Screen screen, DefaultCraftingDisplay recipe) {
                if (!recipe.getRecipe().isPresent())
                    return false;
                if (screen.getClass().isAssignableFrom(CraftingTableScreen.class))
                    ((RecipeBookGuiHooks) (((CraftingTableScreen) screen).getRecipeBookGui())).rei_getGhostSlots().reset();
                else if (screen.getClass().isAssignableFrom(InventoryScreen.class))
                    ((RecipeBookGuiHooks) (((InventoryScreen) screen).getRecipeBookGui())).rei_getGhostSlots().reset();
                else
                    return false;
                MinecraftClient.getInstance().interactionManager.clickRecipe(MinecraftClient.getInstance().player.container.syncId, (Recipe<?>) recipe.getRecipe().get(), Screen.hasShiftDown());
                return true;
            }
            
            @Override
            public boolean acceptRecipe(Screen screen, DefaultCraftingDisplay recipe) {
                return screen instanceof CraftingTableScreen || (screen instanceof InventoryScreen && recipe.getHeight() < 3 && recipe.getWidth() < 3);
            }
        });
        recipeHelper.registerSpeedCraftFunctional(DefaultPlugin.SMELTING, new SpeedCraftFunctional<DefaultSmeltingDisplay>() {
            @Override
            public Class[] getFunctioningFor() {
                return new Class[]{FurnaceScreen.class};
            }
            
            @Override
            public boolean performAutoCraft(Screen screen, DefaultSmeltingDisplay recipe) {
                if (!recipe.getRecipe().isPresent())
                    return false;
                if (screen instanceof FurnaceScreen)
                    ((RecipeBookGuiHooks) (((FurnaceScreen) screen).getRecipeBookGui())).rei_getGhostSlots().reset();
                else
                    return false;
                MinecraftClient.getInstance().interactionManager.clickRecipe(MinecraftClient.getInstance().player.container.syncId, (Recipe<?>) recipe.getRecipe().get(), Screen.hasShiftDown());
                return true;
            }
            
            @Override
            public boolean acceptRecipe(Screen screen, DefaultSmeltingDisplay recipe) {
                return screen instanceof FurnaceScreen;
            }
        });
        recipeHelper.registerSpeedCraftFunctional(DefaultPlugin.SMOKING, new SpeedCraftFunctional<DefaultSmokingDisplay>() {
            @Override
            public Class[] getFunctioningFor() {
                return new Class[]{SmokerScreen.class};
            }
            
            @Override
            public boolean performAutoCraft(Screen screen, DefaultSmokingDisplay recipe) {
                if (!recipe.getRecipe().isPresent())
                    return false;
                if (screen instanceof SmokerScreen)
                    ((RecipeBookGuiHooks) (((SmokerScreen) screen).getRecipeBookGui())).rei_getGhostSlots().reset();
                else
                    return false;
                MinecraftClient.getInstance().interactionManager.clickRecipe(MinecraftClient.getInstance().player.container.syncId, (Recipe<?>) recipe.getRecipe().get(), Screen.hasShiftDown());
                return true;
            }
            
            @Override
            public boolean acceptRecipe(Screen screen, DefaultSmokingDisplay recipe) {
                return screen instanceof SmokerScreen;
            }
        });
        recipeHelper.registerSpeedCraftFunctional(DefaultPlugin.BLASTING, new SpeedCraftFunctional<DefaultBlastingDisplay>() {
            @Override
            public Class[] getFunctioningFor() {
                return new Class[]{BlastFurnaceScreen.class};
            }
            
            @Override
            public boolean acceptRecipe(Screen screen, DefaultBlastingDisplay recipe) {
                return screen instanceof BlastFurnaceScreen;
            }
            
            @Override
            public boolean performAutoCraft(Screen screen, DefaultBlastingDisplay recipe) {
                if (!recipe.getRecipe().isPresent())
                    return false;
                if (screen instanceof BlastFurnaceScreen)
                    ((RecipeBookGuiHooks) (((BlastFurnaceScreen) screen).getRecipeBookGui())).rei_getGhostSlots().reset();
                else
                    return false;
                MinecraftClient.getInstance().interactionManager.clickRecipe(MinecraftClient.getInstance().player.container.syncId, (Recipe<?>) recipe.getRecipe().get(), Screen.hasShiftDown());
                return true;
            }
        });
    }
    
    @Override
    public int getPriority() {
        return -1;
    }
    
}
