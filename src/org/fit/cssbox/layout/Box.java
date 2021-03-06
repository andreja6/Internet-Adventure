/*
 * Box.java
 * Copyright (c) 2005-2007 Radek Burget
 *
 * CSSBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * CSSBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created on 11. z��2005, 18:36
 */

package org.fit.cssbox.layout;

import java.net.URL;
import java.awt.*;

import org.w3c.dom.*;

/**
 * A visual formatting box. It can be of two types: an inline box
 * or a block box.
 *
 * @author  radek
 */
abstract public class Box
{
    public static final short DRAW_ALL = 0; //drawing stages
    public static final short DRAW_NONFLOAT = 1;
    public static final short DRAW_FLOAT = 2;
    public static final short DRAW_BOTH = 0; //drawing modes
    public static final short DRAW_FG = 1;
    public static final short DRAW_BG = 2;
    
    /** Is this a box for the root element? */
    protected boolean rootelem;
    
    /** Is this box a block? */
    protected boolean isblock;
    
    /** Is this box empty? (doesn't contain any visible non-empty block */
    protected boolean isempty;
    
    /** Is this element displayed? (it has not display: none) */
    protected boolean displayed; 

    /** Is this element visible? (it has not visibility: hidden) */
    protected boolean visible;
        
    /** The DOM node that forms this box. It is either an inline element
     * (e.g. <em>) or a text node (anonymous box) */
    protected Node node;
    
    /** The order of the node in the code (first node is 0) */
    protected int order;
    
    /** Box position on the screen relatively to the containing content box.
     * Coordinates of the whole box including margin. */
    protected Rectangle bounds;
    
    /** Absolute box position (on the whole page) */
    protected Rectangle absbounds;
    
    /** The viewport */
    protected Viewport viewport;
    
    /** Parent box */
    protected ElementBox parent;
    
    /** Containing block */
    protected BlockBox cblock;
    
    /** Clipping box. The box is not visible if it is not inside of the clipping box.
     * Normally, the clipping box is the viewport or the nearest parent with
     * overflow set to hidden */
    protected BlockBox clipblock;
    
    /** Maximal total width for the layout (obtained from the owner box) */
    protected int availwidth;
    
    /** Graphics context */
    protected Graphics2D g;
    
    /** Rendering context (em size etc.) */
    protected VisualContext ctx;
    
    /** Base URL */
    protected URL base;
    
    /** True if this box is a result of splitting */
    protected boolean splitted;
    
    /** Remaining part of the box after splitting */
    protected Box rest;
    
    //==============================================================
    
    /**
     * Create a new instance of a box
     * @param n the DOM node that forms this box
     * @param g current graphics context
     * @param ctx current visual context
     */
    public Box(Node n, Graphics2D g, VisualContext ctx)
    {
        this.g = g;
        this.ctx = ctx;
        node = n;
        rootelem = false;
        isblock = false;
        isempty = true;

        bounds = new Rectangle();
        absbounds = new Rectangle();
        displayed = true;
        visible = true;
        splitted = false;
        rest = null;
    }

    /**
     * Copy all the values from another box
     * @param src source box
     */
    public void copyValues(Box src)
    {
        rootelem = src.rootelem;
        isblock = src.isblock;
        order = src.order;
        isempty = src.isempty;
        availwidth = src.availwidth;
        viewport = src.viewport;
        parent = src.parent;
        cblock = src.cblock;
        clipblock = src.clipblock;

        bounds = new Rectangle(src.bounds);
        absbounds = new Rectangle(src.absbounds);
        displayed = src.displayed;
        visible = src.visible;
        splitted = src.splitted;
        rest = src.rest;
    }
    
    /**
     * Provides the internal object initialization, after the box tree has been completed. 
     * Should be used fpr subtree content organization, etc. Called from {@link #initSubtree()} during
     * the tree initialization. 
     */
    protected void initBox()
    {
    }
    
    /**
     * Provides the initialization of the box and the corresponding subtree (if any).
     * Loads the basic CSS properties and computes the sizes.
     */
    public void initSubtree()
    {
        initBox(); //default implementation - just initialize this box
    }
    
    /**
     * Initializes a box in order to be a proper child box of the specified parent. Copies
     * all the necessary information from the parent.
     * @param parent the parent box
     */
    public void adoptParent(ElementBox parent)
    {
        if (parent instanceof BlockBox)
            setContainingBlock((BlockBox) parent);
        else
            setContainingBlock(parent.getContainingBlock());
        setParent(parent);
        setViewport(parent.getViewport());
        setClipBlock(parent.getClipBlock());
    }
    
    //========================================================================
        
    /**
     * Returns the DOM node that forms this box.
     * @return the DOM node
     */
    public Node getNode()
    {
        return node;
    }
    
    /**
     * Gets the order of the node in the document.
	 * @return the order
	 */
	public int getOrder()
	{
		return order;
	}

	/**
     * Sets the order of the node in the document.
	 * @param order the order to set
	 */
	public void setOrder(int order)
	{
		this.order = order;
	}

    /**
     * Returns the graphics context that is used for rendering.
     * @return the graphics context
     */
    public Graphics2D getGraphics()
    {
        return g;
    }

    /**
     * @return the visual context of this box
     */
    public VisualContext getVisualContext()
    {
        return ctx;
    }
    
    /**
     * Checks if the box corresponds to the root element
     * @return true, if the box corresponds to the root element
     */
    public boolean isRootElement()
    {
        return rootelem;
    }

    /**
     * Makes this box a root element box.
     */
    public void makeRoot()
    {
        this.rootelem = true;
    }

    /** 
     * Checks if this is a block box.
     * @return false if this is an inline box and it contains inline
     * boxes only, true otherwise.
     */
    public boolean isBlock()
    {
        return isblock;
    }
    
    /** 
     * @return <code>true</code>, if this element contains no visible non-empty elements
     */
    public boolean isEmpty()
    {
        return isempty;
    }
    
    /**
     * @return <code>true</code>, if this element has the 'display' property different from 'none'
     */
    public boolean isDisplayed()
    {
        return displayed;
    }

    /**
     * Checks if this box is visible, i.e. it has not visibility:hidden and it is at least partially
     * contained in the clipping region.
     * @return <code>true</code> if the element is visible
     */
    public boolean isVisible()
    {
        return visible && clipblock.absbounds.intersects(absbounds) && clipblock.isVisible();
    }
    
    /**
     * Checks if the box has visibility property set to visible.
     * @return <code>true</code>, if this element has the 'visibility' property set to 'visible'
     */
    public boolean isDeclaredVisible()
    {
        return visible;
    }
    
    /**
     * @return <code> true if this is a replaced box
     */
    public boolean isReplaced()
    {
    	return false;
    }
    
    /**
     * @return all the text contained in this box and its subboxes
     */
    abstract public String getText();
    
    /**
     * @return <code>true</code> if the box only contains whitespaces
     */
    abstract public boolean isWhitespace();

    /**
     * Checks whether the whitespaces should be collapsed within in the box according to its style.
     * @return <code>true</code> if the whitespace sequences should be collapsed.
     */
    abstract public boolean collapsesSpaces();
    
    /**
     * @return <code>true</code> if the box can be split in two or more boxes
     * on different lines
     */
    abstract public boolean canSplitInside();
    
    /**
     * @return <code>true</code> if there can be a linebreak before this element
     * on different lines
     */
    abstract public boolean canSplitBefore();
    
    /**
     * @return <code>true</code> if there can be a linebreak after this element
     * on different lines
     */
    abstract public boolean canSplitAfter();
    
    /**
     * Checks whether the box content starts with a whitespace character
     * @return <code>true</code> if the box content is not empty and it starts with a whitespace character
     */
    abstract public boolean startsWithWhitespace();
    
    /**
     * Checks whether the box content ends with a whitespace character
     * @return <code>true</code> if the box content is not empty and it starts with a whitespace character
     */
    abstract public boolean endsWithWhitespace();
    
    /**
     * Switches ignoring the initial whitespace during the box layout on/off
     * @param b when set to <code>true</code> the initial whitespace characters will be ignored
     */
    abstract public void setIgnoreInitialWhitespace(boolean b);
    
    /**
     * Set the box position.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void setPosition(int x, int y)
    {
        bounds.setLocation(x, y);
    }

    /**
     * Move the box to the right.
     * @param ofs distance in pixels - positive numbers move to the right, negative to the left
     */
    public void moveRight(int ofs)
    {
        bounds.x += ofs;
    }
    
    /**
     * Move the box down.
     * @param ofs distance in pixels - positive numbers move down, negative up
     */
    public void moveDown(int ofs)
    {
        bounds.y += ofs;
    }
    
    /**
     * Set the box total width and height.
     * @param w total box width including margins and borders
     * @param h total height
     */
    public void setSize(int w, int h)
    {
        bounds.setSize(w, h);
    }
    
    /**
     * Returns the real width of the box computed during the layout.
     * @return total width including margins and borders.
     */ 
    public int getWidth()
    {
        return bounds.width;
    }
    
    /**
     * Returns the real height of the box computed during the layout.
     * @return total height including margins and borders
     */ 
    public int getHeight()
    {
        return bounds.height;
    }
    
    /**
     * Returns maximal box bounds including all borders and margins.
     * @return Box bounds
     */
    public Rectangle getBounds()
    {
        return bounds;
    }
    
    /**
     * Returns the absolute box position on the page
     * @return Absolute box bounds
     */
    public Rectangle getAbsoluteBounds()
    {
    	return absbounds;
    }
    

    /**
     * Adjusts the absolute bounds width and height in order to fit into the clip.
     * If the box doesn't fit at all, it is marked as invisible.
     */
    public void clipAbsoluteBounds(Rectangle clip)
    {
        Rectangle inter = absbounds.intersection(clip);
        if (inter.width == 0 && inter.height == 0)
            displayed = false;
        else
            absbounds = inter;
    }
    
    /**
     * Computes the absolute bounds after clipping by the appropriate clipping box. 
     * @return the clipped absolute bounds
     */
    public Rectangle getClippedBounds()
    {
        return absbounds.intersection(clipblock.absbounds);
    }
    
    /**
     * @return maximal width that was available for the box placement during the layout processing
     */
    public int getAvailableWidth()
    {
        return availwidth;
    }
    
    /**
     * Set the maximal width available to the box
     * @param availw the maximal available width
     */
    public void setAvailableWidth(int availw)
    {
        availwidth = availw;
    }
    
    /**
     * @return the containing block of this box according to the 
     * <a href="http://www.w3.org/TR/CSS21/visudet.html#containing-block-details">CSS specification</a>
     */
    public BlockBox getContainingBlock()
    {
        return cblock;
    }
    
    /**
     * Set the containing block. During the layout, the box position will be
     * computed inside of the containing block.
     * @param box the containing box
     */
    public void setContainingBlock(BlockBox box)
    {
        cblock = box;
    }
    
    /**
     * Determines the clipping box. The box is not visible if it is not inside of the clipping box. 
     * Normally, the clipping box is the viewport or the nearest parent with overflow set to hidden. 
     * @return the clipping block
     */
    public BlockBox getClipBlock()
    {
        return clipblock;
    }

    /**
     * Sets the clipping block.
     * @param clipblock the clipblock to set
     */
    public void setClipBlock(BlockBox clipblock)
    {
        this.clipblock = clipblock;
    }
    
    /**
     * @return the expected width of the box according to the CSS property values
     */ 
    abstract public int totalWidth();
    
    /**
     * @return the expected height of the box according to the CSS property values
     */ 
    abstract public int totalHeight();
    
    /**
     * @return maximal available width of the content during the layout
     */
    abstract public int getAvailableContentWidth();
    
    /**
     * @return the X coordinate of the content box top left corner
     */
    abstract public int getContentX();
    
    /**
     * @return the absolute X coordinate of the content box top left corner
     */
    abstract public int getAbsoluteContentX();
    
    /**
     * @return the Y coordinate of the content box top left corner
     */
    abstract public int getContentY();

    /**
     * @return the Y coordinate of the content box top left corner
     */
    abstract public int getAbsoluteContentY();
    
    /**
     * @return the width of the content without any margins and borders
     */
    abstract public int getContentWidth();
    
    /**
     * @return the height of the content without any margins and borders
     */
    abstract public int getContentHeight();
    
    /**
     * @return the bounds of the content box
     */
    public Rectangle getContentBounds()
    {
        return new Rectangle(getContentX(), getContentY(), getContentWidth(), getContentHeight());
    }
    
    /**
     * @return the absolute bounds of the content box
     */
    public Rectangle getAbsoluteContentBounds()
    {
        return new Rectangle(getAbsoluteContentX(), getAbsoluteContentY(), getContentWidth(), getContentHeight());
    }
    
    /**
     * Determines the minimal width in which the element can fit.
     * @return the minimal width
     */
    abstract public int getMinimalWidth();
    
    /**
     * 
     * Determines the maximal width of the element according to its contents.
     * @return the maximal width
     */
    abstract public int getMaximalWidth();
    
    /**
     * Determines the minimal bounds of the really displayed content.
     * @return the minimal bounds
     */
    abstract public Rectangle getMinimalAbsoluteBounds();
    
    /**
     * @return true, if the box is in-flow
     */
    abstract public boolean isInFlow();

    /**
     * @return <code>true</code> if the width of the box is either explicitely
     * set or it can be computed from the parent box
     */
    abstract public boolean hasFixedWidth();
    
    /**
     * @return <code>true</code> if the height of the box is either explicitely
     * set or it can be computed from the parent box
     */
    abstract public boolean hasFixedHeight();
    
    /**
     * @return true, if the box contains any in-flow boxes
     */
    abstract public boolean containsFlow();
    
    /**
     * @return true, if the element displays at least something (some content,
     * or borders) 
     */
    abstract public boolean affectsDisplay();

    /**
	 * @return the viewport
	 */
	public Viewport getViewport()
	{
		return viewport;
	}

	/**
	 * @param viewport the viewport to set
	 */
	public void setViewport(Viewport viewport)
	{
		this.viewport = viewport;
	}

	/**
	 * @return Returns the parent.
	 */
	public ElementBox getParent()
	{
		return parent;
	}

	/**
	 * @param parent The parent to set.
	 */
	public void setParent(ElementBox parent)
	{
		this.parent = parent;
	}
	
	/**
     * @return the base URL
     */
    public URL getBase()
    {
        return base;
    }

    /**
     * @param base the base URL to set
     */
    public void setBase(URL base)
    {
        this.base = base;
    }

    /**
	 * When the box doesn't fit to the line, it can be split in two boxes.
	 * The first one remains at its place and the rest must be placed elsewhere.
	 * The splitting algorithm depends on the box type.
	 * @return the rest of the box
	 */
    protected Box getRest()
    {
        return rest;
    }
    
    //========================================================================
    
    /** 
     * Compute the width and height of this element. Layout the sub-elements.
     * @param availw Maximal width available to the child elements
     * @param force Use the area even if the used width is greater than maxwidth
     * @param linestart Indicates whether the element is placed at the line start
     * @return True if the box has been succesfully placed
     */
    abstract public boolean doLayout(int availw, boolean force, boolean linestart);
    

    /** 
     * Calculate absolute positions of all the subboxes.
     */
    abstract public void absolutePositions();
    
    
    //=======================================================================
    
    /** 
     * Draw the box and all the subboxes on the default
     * graphics context passed to the box constructor. 
     */
    public void draw()
    {
        draw(g);
    }
    
    /**
     * Draw the box and all the subboxes.
     * @param g graphics context to draw on
     */
    public void draw(Graphics2D g)
    {
        if (isVisible())
        {
            draw(g, DRAW_NONFLOAT, DRAW_BOTH);
            draw(g, DRAW_FLOAT, DRAW_BOTH);
            draw(g, DRAW_NONFLOAT, DRAW_FG);
        }
    }
    
    /**
     * Draw the specified stage (DRAW_*)
     * @param g graphics context to draw on
     * @param turn drawing stage - DRAW_ALL, DRAW_FLOAT or DRAW_NONFLOAT
     * @param mode what to draw - DRAW_FG, DRAW_BG or DRAW_BOTH 
     */
    abstract public void draw(Graphics2D g, int turn, int mode);

    /**
     * Draw the bounds of the box (for visualisation).
     */
    abstract public void drawExtent(Graphics2D g);

}
