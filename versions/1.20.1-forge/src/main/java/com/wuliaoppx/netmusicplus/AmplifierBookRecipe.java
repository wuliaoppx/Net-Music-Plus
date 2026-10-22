package com.wuliaoppx.netmusicplus;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Recipe serializer for the three amplifier books.
 * <p>
 * On 1.21 the books could be declared straight in JSON: the result carried a {@code minecraft:stored_enchantments}
 * data component. 1.20.1 recipes take a plain item id and reject any extra data tag, so the enchantment has to be
 * attached in code. The JSON therefore keeps the vanilla {@code ingredients}/{@code category} fields, adds
 * {@code amplifier_level}, and this serializer hands the parsing to vanilla before swapping in a result stack that
 * already holds the enchantment.
 * <p>
 * The stack is a normal {@link ShapelessRecipe}, so the recipe still shows up in the recipe book and is synced to
 * clients by vanilla's serializer (which writes the result {@link net.minecraft.world.item.ItemStack} including its
 * NBT).
 */
public final class AmplifierBookRecipe implements RecipeSerializer<ShapelessRecipe> {
    /** Level of the amplifier enchantment the crafted book should carry. */
    public static final String LEVEL_KEY = "amplifier_level";
    /** Placeholder result in the JSON; replaced by an enchanted book built in {@link #fromJson}. */
    public static final String RESULT_ITEM = "minecraft:enchanted_book";

    private static final ShapelessRecipe.Serializer VANILLA = new ShapelessRecipe.Serializer();

    public static final AmplifierBookRecipe INSTANCE = new AmplifierBookRecipe();

    private AmplifierBookRecipe() {
    }

    @Override
    public ShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
        int level = Math.max(1, Math.min(GsonHelper.getAsInt(json, LEVEL_KEY, 1), AmplifierData.MAX_TIER));

        // Let vanilla parse ingredients, group and category; only the result needs to be replaced.
        JsonObject vanillaJson = json.deepCopy();
        JsonObject result = new JsonObject();
        result.addProperty("item", RESULT_ITEM);
        result.addProperty("count", 1);
        vanillaJson.add("result", result);

        ShapelessRecipe base = VANILLA.fromJson(id, vanillaJson);
        return new ShapelessRecipe(id, base.getGroup(), base.category(),
                AmplifierData.book(NetMusicPlus.AMPLIFIER.get(), level), base.getIngredients());
    }

    @Override
    public ShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        return VANILLA.fromNetwork(id, buf);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, ShapelessRecipe recipe) {
        VANILLA.toNetwork(buf, recipe);
    }
}
