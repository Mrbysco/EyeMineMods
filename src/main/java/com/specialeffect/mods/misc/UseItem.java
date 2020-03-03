/**
 * Copyright (C) 2016 Kirsty McNaught, SpecialEffect
 * www.specialeffect.org.uk
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.specialeffect.mods.misc;

import java.awt.Color;

import org.lwjgl.glfw.GLFW;

import com.irtimaled.bbor.client.renderers.AbstractRenderer;

import com.specialeffect.mods.ChildMod;
import com.specialeffect.mods.mousehandling.MouseHandler;
import com.specialeffect.utils.CommonStrings;
import com.specialeffect.utils.ModUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class UseItem extends ChildMod {
	public final String MODID = "useitem";

	public void setup(final FMLCommonSetupEvent event) {

		// Register key bindings
		mUseItemOnceKB = new KeyBinding("Use item", GLFW.GLFW_KEY_KP_0, CommonStrings.EYEGAZE_COMMON);
		ClientRegistry.registerKeyBinding(mUseItemOnceKB);

		mUseItemContinuouslyKB = new KeyBinding("Use item continuously", GLFW.GLFW_KEY_KP_1,
				CommonStrings.EYEGAZE_EXTRA);
		ClientRegistry.registerKeyBinding(mUseItemContinuouslyKB);

		mPrevItemKB = new KeyBinding("Select previous item", GLFW.GLFW_KEY_KP_4, CommonStrings.EYEGAZE_EXTRA);
		ClientRegistry.registerKeyBinding(mPrevItemKB);

		mNextItemKB = new KeyBinding("Select next item", GLFW.GLFW_KEY_KP_5, CommonStrings.EYEGAZE_EXTRA);
		ClientRegistry.registerKeyBinding(mNextItemKB);

	}
	
	private class TargetBlock {

		final BlockPos pos;
		final Direction direction;
		
		public TargetBlock(BlockRayTraceResult res) {
			this.pos = res.getPos();
			this.direction = res.getFace();
		}

		private UseItem getOuterType() {
			return UseItem.this;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((direction == null) ? 0 : direction.hashCode());
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TargetBlock other = (TargetBlock) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (direction != other.direction)
				return false;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			return true;
		}
	}
	

	private static KeyBinding mUseItemOnceKB;
	private static KeyBinding mUseItemContinuouslyKB;
	private static KeyBinding mPrevItemKB;
	private static KeyBinding mNextItemKB;
	
	// State for 'continuously use'
	private boolean mUsingItem = false;
	private int usingTimer = 0;
	private int usingCooldown = 10; //FIXME: put in user config
	private Vec3d lastLook = new Vec3d(1.0,0.0,0.0);
	
	private long lastTime = 0;
	private long currDwellTime = 0;
	private int dwellTimeInit = 200; // ms
	private int dwellTimeComplete = 1000; // ms
	
	private TargetBlock currTarget;
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			long time = System.currentTimeMillis();
			long dt = time - this.lastTime;
			this.lastTime = time;
			if (mUsingItem) {
				this.currDwellTime += dt;
				System.out.println(this.currDwellTime);
				
				// Set mouse in correct state - shouldn't build unless there's an
				// accompanying mouse movement.	
				if (MouseHandler.hasPendingEvent() && this.currDwellTime > this.dwellTimeComplete) {						
					final KeyBinding useItemKeyBinding = Minecraft.getInstance().gameSettings.keyBindUseItem;
					KeyBinding.onTick(useItemKeyBinding.getKey());
					this.currDwellTime = 0;				
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onBlockOutlineRender(DrawBlockHighlightEvent e)
	{
		if (mUsingItem && this.currDwellTime > this.dwellTimeInit) {
			double dAlpha = 255.0*(this.currDwellTime - this.dwellTimeInit)/this.dwellTimeComplete;
			int iAlpha = (int)dAlpha;
//			System.out.println("alpha: "+iAlpha);
			RayTraceResult raytraceResult = e.getTarget();			
			if(e.getSubID() == 0 && raytraceResult.getType() == RayTraceResult.Type.BLOCK)
			{
				BlockRayTraceResult rayTraceBlock = ModUtils.getMouseOverBlock();

	            if (rayTraceBlock != null) {
	            	
	            	TargetBlock target = new TargetBlock(rayTraceBlock);
	            	
	            	// Is this the block we're currently dwelling on? 
	            	// TODO: persist dwells on >1 block, in case you're on the edge?
	            	if (!target.equals(this.currTarget)) {
	            		this.currDwellTime = 0;
	            		this.currTarget = target;
	            		return;
	            	}
					
					Color color = new Color(0.75f, 0.25f, 0.0f);
					AbstractRenderer.renderBlockFace(rayTraceBlock.getPos(), target.direction, color, iAlpha);
	            }
			}
		}
	}
	
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if (ModUtils.hasActiveGui()) { return; }
		
		final KeyBinding useItemKeyBinding = Minecraft.getInstance().gameSettings.keyBindUseItem;
		PlayerEntity player = Minecraft.getInstance().player;
		
		if (mUseItemContinuouslyKB.isPressed()) {
			mUsingItem = !mUsingItem;
//			boolean useItemNewState = !useItemKeyBinding.isKeyDown();
//			KeyBinding.setKeyBindState(useItemKeyBinding.getKey(), useItemNewState);
			final String message = "Using item: " + (mUsingItem ? "ON" : "OFF");
	        player.sendMessage(new StringTextComponent(message));
	        
	        // Tune throttling according to what's in your hand
	        if (mUsingItem) {
	        	ItemStack itemStack = player.inventory.getCurrentItem();
		        Item item = player.inventory.getCurrentItem().getItem();
		        
		        this.currDwellTime = 0;
				if (player.inventory.getCurrentItem().getItem() instanceof BlockItem) {
					System.out.println("block item!");
					this.dwellTimeComplete = 1000;
					this.dwellTimeInit = 200;
				}
				else {
					System.out.println("not a block ...!");
					this.dwellTimeComplete = 300;
					this.dwellTimeInit = 100;
				}
	        }

//	        if (player.inventory.currentItem)
		} else if (mUseItemOnceKB.isPressed()) {
			KeyBinding.onTick(useItemKeyBinding.getKey());
		} else if (mPrevItemKB.isPressed()) {
			player.inventory.changeCurrentItem(1);
		} else if (mNextItemKB.isPressed()) {
			player.inventory.changeCurrentItem(-1);
		}
	}
	
	@SubscribeEvent
	public void onRenderGameOverlayEvent(final RenderGameOverlayEvent.Post event) {
		if(event.isCancelable() || event.getType() != ElementType.EXPERIENCE)
		{      
			return;
		}
		
		// If use-item is on, show a warning message
		if (mUsingItem) {
			Minecraft mc = Minecraft.getInstance();
			int w = mc.mainWindow.getScaledWidth();
			int h = mc.mainWindow.getScaledHeight();
						
			String msg = "USING";
			int msgWidth = mc.fontRenderer.getStringWidth(msg);
		    
		    mc.fontRenderer.drawStringWithShadow(msg, w/2 - msgWidth/2, h/2 - 20, 0xffFFFFFF);		    
		    
		}
		
	}
}
