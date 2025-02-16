package com.rewindmc.retroemi;

import com.google.common.collect.Lists;
import dev.emi.emi.input.EmiInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;

public class REMIScreen extends GuiScreen implements ParentElement {

	protected final Minecraft client = Minecraft.getMinecraft();
	private int mouseDown = -1;
	private int lastMouseX, lastMouseY;

	private boolean closing = false;

	protected final Text title;
	private final List<Element> children = Lists.newArrayList();
	private final List<Drawable> drawables = Lists.newArrayList();

	public REMIScreen(Text title) {
		this.title = title;
	}

	@Override
	public void initGui() {
		closing = false;
		mouseDown = -1;
		super.initGui();
		children.clear();
		drawables.clear();
		init();
	}

	@Override
	public final void drawScreen(int var1, int var2, float var3) {
		glEnable(GL_RESCALE_NORMAL);
		render(DrawContext.INSTANCE, var1, var2, var3);
		super.drawScreen(var1, var2, var3);
		if (mouseDown != -1) {
			mouseDragged(var1, var2, mouseDown, var1 - lastMouseX, var2 - lastMouseY);
		}
		lastMouseX = var1;
		lastMouseY = var2;
		glDisable(GL_RESCALE_NORMAL);
	}

	@Override
	public void handleKeyboardInput() {
		super.handleKeyboardInput();
		int k = Keyboard.getEventKey();
		char c = Keyboard.getEventCharacter();
		if (!Keyboard.getEventKeyState() || k == 0 && Character.isDefined(c)) {
			keyReleased(Keyboard.getEventKey(), 0, EmiInput.getCurrentModifiers());
		}
	}

	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		if (Mouse.getEventDWheel() != 0) {
			mouseScrolled(lastMouseX, lastMouseY, Mouse.getEventDWheel() / 120D);
		}
	}

	@Override
	public final void keyTyped(char var1, int var2) {
		if (var2 == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
			close();
			return;
		}
		int mod = EmiInput.getCurrentModifiers();
		keyPressed(var2, 0, mod);
		if (var1 != 0 && !Character.isISOControl(var1)) {
			charTyped(var1, mod);
		}
	}

	@Override
	public final void mouseClicked(int var1, int var2, int var3) {
		mouseClicked((double) var1, (double) var2, var3);
		lastMouseX = var1;
		lastMouseY = var2;
		mouseDown = var3;
	}

	@Override
	public final void mouseMovedOrUp(int var1, int var2, int var3) {
		mouseReleased((double) var1, (double) var2, var3);
		if (var3 == mouseDown) {
			mouseDown = -1;
		}
	}

	@Override
	public final boolean doesGuiPauseGame() {
		return shouldPause();
	}

	@Override
	public final void onGuiClosed() {
		mouseDown = -1;
		if (closing) {
			return;
		}
		closing = true;
		close();
	}


	protected boolean shouldCloseOnEsc() {
		return true;
	}

	protected void init() {

	}

	protected boolean shouldPause() {
		return super.doesGuiPauseGame();
	}

	protected void render(DrawContext raw, int mouseX, int mouseY, float delta) {
		for (Drawable drawable : this.drawables) {
			drawable.render(raw, mouseX, mouseY, delta);
		}
	}

	protected void renderBackgroundTexture(DrawContext ctx) {
		drawBackground(0);
	}

	protected void renderBackground(DrawContext ctx) {
		drawDefaultBackground();
	}

	protected void close() {
		super.onGuiClosed();
		client.displayGuiScreen(null);
	}


	@Nullable
	private Element focused;
	private boolean dragging;

	@Override
	public final boolean isDragging() {
		return this.dragging;
	}

	@Override
	public final void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	@Override
	@Nullable
	public Element getFocused() {
		return this.focused;
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		if (this.focused != null) {
			this.focused.setFocused(false);
		}
		if (focused != null) {
			focused.setFocused(true);
		}
		this.focused = focused;
	}


	@Override
	public List<? extends Element> children() {
		return children;
	}


	protected <T extends Element & Drawable> T addDrawableChild(T drawableElement) {
		this.drawables.add(drawableElement);
		return this.addSelectableChild(drawableElement);
	}

	protected <T extends Drawable> T addDrawable(T drawable) {
		this.drawables.add(drawable);
		return drawable;
	}

	protected <T extends Element> T addSelectableChild(T child) {
		this.children.add(child);
		return child;
	}

	protected void remove(Element child) {
		if (child instanceof Drawable) {
			this.drawables.remove((Drawable) child);
		}
		this.children.remove(child);
	}
}
