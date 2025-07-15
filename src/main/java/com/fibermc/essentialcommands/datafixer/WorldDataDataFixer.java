package com.fibermc.essentialcommands.datafixer;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Maps;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Dynamic;

public final class WorldDataDataFixer {
    private WorldDataDataFixer() {}

    public static final DSL.TypeReference TYPE = () -> "world_data";

    // V0 Schema - Original format where spawn field might contain empty objects
    public static class V0 extends Schema {
        public V0(int versionKey, Schema parent) {
            super(versionKey, parent);
        }

        @Override
        public void registerTypes(
            Schema schema,
            Map<String, Supplier<TypeTemplate>> entityTypes,
            Map<String, Supplier<TypeTemplate>> blockEntityTypes
        ) {
            this.registerType(true, TYPE, this::v0);
        }

        @Override
        public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
            return Maps.newHashMap();
        }

        @Override
        public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
            return Maps.newHashMap();
        }

        private TypeTemplate v0() {
            // V0: Simple remainder type that accepts any structure
            // We'll handle the validation in the datafixer
            return DSL.remainder();
        }
    }

    // V1 Schema - Clean format with proper optional spawn field
    public static class V1 extends Schema {
        public V1(int versionKey, Schema parent) {
            super(versionKey, parent);
        }

        @Override
        public void registerTypes(
            Schema schema,
            Map<String, Supplier<TypeTemplate>> entityTypes,
            Map<String, Supplier<TypeTemplate>> blockEntityTypes
        ) {
            schema.registerType(true, TYPE, this::v1);
        }

        @Override
        public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
            return Maps.newHashMap();
        }

        @Override
        public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
            return Maps.newHashMap();
        }

        private TypeTemplate v1() {
            // V1: Clean format - still flexible with remainder,
            // but the DataFix will ensure empty spawn objects are removed
            return DSL.remainder();
        }
    }

    public static class RemoveEmptyObjectSpawnFix extends DataFix {
        public RemoveEmptyObjectSpawnFix(Schema outputSchema) {
            super(outputSchema, false);
        }

        @Override
        public TypeRewriteRule makeRule() {
            Type<?> inputType = getInputSchema().getType(TYPE);

            return fixTypeEverywhereTyped("RemoveEmptyObjectSpawnFix", inputType, typed -> {
                return typed.update(DSL.remainderFinder(), this::removeEmptyObjectSpawnFields);
            });
        }

        private Dynamic<?> removeEmptyObjectSpawnFields(Dynamic<?> dynamic) {
            var hasSpawnValueWithEmptyObject = dynamic.get("spawn")
                .get()
                .mapOrElse( // if we've got a spawn
                    spawn -> spawn.getMapValues().map(Map::isEmpty).getOrThrow(), // and that spawn object is empty
                    (_keyMissingError) -> false // (false if no spawn key in WorldData)
                );

            if (hasSpawnValueWithEmptyObject) {
                return dynamic.remove("spawn");
            }

            return dynamic;
        }
    }

    public static DataFixerBuilder createDataFixer() {
        DataFixerBuilder builder = new DataFixerBuilder(1);

        builder.addSchema(0, V0::new);
        Schema v1Schema = builder.addSchema(1, V1::new);
        builder.addFixer(new RemoveEmptyObjectSpawnFix(v1Schema));

        return builder;
    }

}
