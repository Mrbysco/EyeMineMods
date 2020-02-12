/**
 * Copyright (C) 2016 Kirsty McNaught, SpecialEffect
 * www.specialeffect.org.uk
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.specialeffect.gui;

import com.specialeffect.utils.ModUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class IconOverlay
{
	private ResourceLocation mResource;
	
	// Current state
	private boolean mVisible = false;	
	
	// Position/appearance (see setters)
	// Position/size are relative to screen	
	private float mCentreX = 0.5f;
	private float mCentreY = 0.5f;
	private float mHeight = 1.0f;
	private float mAspectRatio = 1.0f;
	private float mAlpha = 1.0f;
	private int fadeTime = 20;
	private int fadeCountdown = 0;

	public IconOverlay(Minecraft mc, String resourcePath)
	{
		mResource = new ResourceLocation(resourcePath);				
	}
	
	public void setPosition(float centreX, float centreY, float height, float aspectRatio) {
		mCentreX = centreX;
		mCentreY = centreY;
		mAspectRatio = aspectRatio;
		mHeight = height;
	}

	public void setVisible(boolean visible) {
		if (visible != mVisible) {
			fadeCountdown = fadeTime;
		}
		
		mVisible = visible;		
	}
	
	public void setAlpha(float alpha) {
		mAlpha = alpha;
	}
	
	// A helper function to draw a texture scaled to fit.
	private void drawTexture(int screenHeight, int screenWidth, float fade)
	{
		// calculate position
		int height = (int)(screenWidth*mHeight);
		int width = (int)(height*mAspectRatio);
		int centreX = (int)(mCentreX*screenWidth);
		int centreY = (int)(mCentreY*screenHeight);
		
		// render the texture 
		// TODO:white? black? drop shadow? 
		Minecraft.getInstance().getTextureManager().bindTexture(mResource);					
		ModUtils.drawTexQuad(centreX - width/2, centreY - height/2, 
							 width, height, mAlpha*fade);
		

	}
	
	// This event is called by GuiIngameForge during each frame by
	// GuiIngameForge.pre() and GuiIngameForce.post().
	@SubscribeEvent
	public void onRenderGameOverlayEvent(final RenderGameOverlayEvent.Post event) {

		// We draw after the ExperienceBar has drawn.  The event raised by GuiIngameForge.pre()
		// will return true from isCancelable.  If you call event.setCanceled(true) in
		// that case, the portion of rendering which this event represents will be canceled.
		// We want to draw *after* the experience bar is drawn, so we make sure isCancelable() returns
		// false and that the eventType represents the ExperienceBar event.
		if(event.isCancelable() || event.getType() != ElementType.EXPERIENCE)
		{      
			return;
		}
		
		if (mVisible || fadeCountdown > 0) {		
			// Update fading ticks
			float fade = 1.0f;
			if (fadeCountdown > 0) {
				fadeCountdown--;
				fade = fadeCountdown/(float)fadeTime;
				if (mVisible) {
					fade = 1.0f - fade;	
				}
			}				
			
			// Don't show if the debug screen is open
			if (Minecraft.getInstance().gameSettings.showDebugInfo) {
				return;
			}
		
			int w = event.getWindow().getScaledWidth();
			int h = event.getWindow().getScaledHeight();
			this.drawTexture(h, w, fade);
		}
	}

}