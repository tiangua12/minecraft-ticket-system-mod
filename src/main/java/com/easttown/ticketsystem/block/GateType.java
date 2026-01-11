package com.easttown.ticketsystem.block;

import net.minecraft.util.StringRepresentable;

public enum GateType implements StringRepresentable {
    IN("in"),       // 仅入站
    OUT("out"),      // 仅出站
    BIDIRECTIONAL("bidirectional");  // 双向
    
    private final String name;
    
    GateType(String name) {
        this.name = name;
    }
    
    @Override
    public String getSerializedName() {
        return name;
    }
}
