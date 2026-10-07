package com.kltyton.eden_realm.data.model.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kltyton.eden_realm.ERConstants;

final class ERCustomBlockModels {
    private ERCustomBlockModels() {
    }

    static JsonObject emissiveOre(String name) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture(name));
        textures.addProperty("base", texture(name));
        textures.addProperty("glow", texture(name + "_g"));
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (String layer : new String[]{"base", "glow"}) {
            JsonObject cube = element();
            JsonObject faces = new JsonObject();
            for (String direction : new String[]{"down", "up", "north", "south", "west", "east"}) {
                faces.add(direction, face("#" + layer, direction, false));
            }
            cube.add("faces", faces);
            if (layer.equals("glow")) {
                cube.addProperty("shade", false);
                cube.addProperty("light_emission", 15);
                JsonObject faceData = new JsonObject();
                faceData.addProperty("ambient_occlusion", false);
                cube.add("neoforge_data", faceData);
            }
            elements.add(cube);
        }
        model.add("elements", elements);
        return model;
    }

    static JsonObject grass(String baseTexture, String topTexture, String decorationTexture) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture(baseTexture));
        textures.addProperty("bottom", texture(baseTexture));
        textures.addProperty("top", texture(topTexture));
        textures.addProperty("side", texture(baseTexture));
        textures.addProperty("overlay", texture("eden_grass_block_side_overlay"));
        if (decorationTexture != null) {
            textures.addProperty("decoration", texture(decorationTexture));
        }
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(grassBaseElement());
        elements.add(grassSideOverlayElement());
        if (decorationTexture != null) {
            elements.add(grassDecorationElement());
        }
        model.add("elements", elements);
        return model;
    }

    static JsonObject duckweed() {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture("duckweed"));
        textures.addProperty("texture", texture("duckweed"));
        model.add("textures", textures);

        JsonObject element = new JsonObject();
        element.add("from", coordinates(0, 0.25, 0));
        element.add("to", coordinates(16, 0.25, 16));
        JsonObject faces = new JsonObject();
        JsonObject down = face("#texture", null, false);
        down.add("uv", coordinates(0, 16, 16, 0));
        faces.add("down", down);
        faces.add("up", face("#texture", null, false));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject grassBaseElement() {
        JsonObject element = element();
        JsonObject faces = new JsonObject();
        faces.add("down", face("#bottom", "down", false));
        faces.add("up", face("#top", "up", true));
        faces.add("north", face("#side", "north", false));
        faces.add("south", face("#side", "south", false));
        faces.add("west", face("#side", "west", false));
        faces.add("east", face("#side", "east", false));
        element.add("faces", faces);
        return element;
    }

    private static JsonObject grassSideOverlayElement() {
        JsonObject element = element();
        JsonObject faces = new JsonObject();
        faces.add("north", face("#overlay", "north", true));
        faces.add("south", face("#overlay", "south", true));
        faces.add("west", face("#overlay", "west", true));
        faces.add("east", face("#overlay", "east", true));
        element.add("faces", faces);
        return element;
    }

    private static JsonObject grassDecorationElement() {
        JsonObject element = new JsonObject();
        element.add("from", coordinates(0, 16.075, 0));
        element.add("to", coordinates(16, 16.075, 16));
        JsonObject faces = new JsonObject();
        faces.add("up", face("#decoration", null, false));
        JsonObject down = face("#decoration", null, false);
        down.add("uv", coordinates(0, 16, 16, 0));
        faces.add("down", down);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject element() {
        JsonObject element = new JsonObject();
        element.add("from", coordinates(0, 0, 0));
        element.add("to", coordinates(16, 16, 16));
        return element;
    }

    private static JsonObject face(String texture, String cullFace, boolean tinted) {
        JsonObject face = new JsonObject();
        face.add("uv", coordinates(0, 0, 16, 16));
        face.addProperty("texture", texture);
        if (cullFace != null) {
            face.addProperty("cullface", cullFace);
        }
        if (tinted) {
            face.addProperty("tintindex", 0);
        }
        return face;
    }

    private static JsonArray coordinates(Number... values) {
        JsonArray array = new JsonArray();
        for (Number value : values) {
            array.add(value);
        }
        return array;
    }

    private static String texture(String texture) {
        return ERConstants.id("block/" + texture).toString();
    }
}
