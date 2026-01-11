package com.easttown.ticketsystem.util;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class GateUtil {
    public static UUID getTicketId(CompoundTag ticketTag) {
        if (ticketTag.contains("TicketId")) {
            return UUID.fromString(ticketTag.getString("TicketId"));
        }
        return null;
    }
}
