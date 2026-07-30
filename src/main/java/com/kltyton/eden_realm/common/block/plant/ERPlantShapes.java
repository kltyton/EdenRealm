package com.kltyton.eden_realm.common.block.plant;

public final class ERPlantShapes {
    public static final DoublePlantShape GOLDEN_SPIKE_GRASS = new DoublePlantShape(13.0, 16.0, 8.0, 9.0);
    public static final DoublePlantShape PURPLE_GLOW_CATTAIL = new DoublePlantShape(14.0, 16.0, 10.0, 14.0);
    public static final DoublePlantShape GRAY_SPIKE_REED = new DoublePlantShape(13.0, 16.0, 10.0, 12.0);
    public static final DoublePlantShape WATER_SCALLION = new DoublePlantShape(12.0, 16.0, 9.0, 7.0);
    public static final DoublePlantShape UMBRELLA_HYGROPHILA = new DoublePlantShape(9.0, 16.0, 9.0, 12.0);

    private ERPlantShapes() {
    }

    public record DoublePlantShape(
            double lowerWidth,
            double lowerHeight,
            double upperWidth,
            double upperHeight) {
        public double totalHeight() {
            return lowerHeight + upperHeight;
        }
    }
}
