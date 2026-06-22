package com.kltyton.eden_realm.data.worldgen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jspecify.annotations.NonNull;

public final class ERWorldgenProvider implements DataProvider {
    public ERWorldgenProvider(PackOutput output) {
    }

    @Override
    public @NonNull CompletableFuture<?> run(@NonNull CachedOutput cache) {
        return CompletableFuture.allOf();
    }

    @Override
    public @NonNull String getName() {
        return "Eden Realm Worldgen Placeholders";
    }
}
