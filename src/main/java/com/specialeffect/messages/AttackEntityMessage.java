/**
 * Copyright (C) 2016 Kirsty McNaught, SpecialEffect
 * www.specialeffect.org.uk
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.specialeffect.messages;

import javax.xml.ws.handler.MessageContext;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;

public class AttackEntityMessage implements IMessage {
    
    private int entityId = -1;

    public AttackEntityMessage() { }

    public AttackEntityMessage(Entity entity) {
    	this.entityId = entity.getEntityId();
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = ByteBufUtils.readVarInt(buf, 5);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, entityId, 5);
    }

    public static class Handler implements IMessageHandler<AttackEntityMessage, IMessage> {        
    	@Override
        public IMessage onMessage(final AttackEntityMessage message,final MessageContext ctx) {
            IThreadListener mainThread = (WorldServer) ctx.getServerHandler().playerEntity.world; // or Minecraft.getInstance() on the client
            mainThread.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    PlayerEntity player = ctx.getServerHandler().playerEntity;
                    if (null != player)
                    {
	                    Entity targetEntity = player.world.
	                            getEntityByID(message.entityId);
	                    if (null != targetEntity) {
	                    	player.attackTargetEntityWithCurrentItem(targetEntity);
	                    }
                    }
                }
            });
            return null; // no response in this case
        }
    }
}