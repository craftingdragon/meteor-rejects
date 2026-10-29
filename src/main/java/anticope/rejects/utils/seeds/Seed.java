package anticope.rejects.utils.seeds;

import com.seedfinding.mccore.version.MCVersion;
import net.minecraft.SharedConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class Seed {
    public final Long seed;
    public final MCVersion version;
    public final String minecraftVersion;

    public Seed(Long seed, MCVersion version) {
        this(seed, version == null ? SharedConstants.getCurrentVersion().name() : version.name);
    }

    public Seed(Long seed, String minecraftVersion) {
        this.seed = seed;
        this.minecraftVersion = minecraftVersion == null || minecraftVersion.isBlank()
            ? SharedConstants.getCurrentVersion().name() : minecraftVersion;
        MCVersion parsed = MCVersion.fromString(this.minecraftVersion);
        this.version = parsed == null ? MCVersion.latest() : parsed;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("seed", LongTag.valueOf(seed));
        tag.put("version", StringTag.valueOf(minecraftVersion));
        return tag;
    }

    public static Seed fromTag(CompoundTag tag) {
        return new Seed(
            tag.getLong("seed").orElse(0L),
            tag.getString("version").orElse(SharedConstants.getCurrentVersion().name())
        );
    }

    public Component toText() {
        MutableComponent text = Component.literal(String.format("[%s%s%s] (%s)",
            ChatFormatting.GREEN,
            seed.toString(),
            ChatFormatting.WHITE,
            minecraftVersion
        ));
        text.setStyle(text.getStyle()
            .withClickEvent(new ClickEvent.CopyToClipboard(
                seed.toString()
            ))
            .withHoverEvent(new HoverEvent.ShowText(
                Component.literal("Copy to clipboard")
            ))
        );
        return text;
    }
}
