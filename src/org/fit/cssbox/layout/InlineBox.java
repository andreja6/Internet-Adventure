/*
 * InlineBox.java
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
 * Created on 5. �nor 2006, 13:38
 */

package org.fit.cssbox.layout;

import java.awt.*;

import org.w3c.dom.*;
import cz.vutbr.web.css.*;

/**
 * An inline element box.
 *
 * @author  radek
 */
public class InlineBox extends ElementBox implements Inline
{
    /** vertical box alignment specified by the style */
    private CSSProperty.VerticalAlign valign;
    
    /** parent LineBox assigned during layout */
    private LineBox linebox;
    
    /** line box describing the children layout */
    private LineBox curline;
    
    /** half-lead after layout */
    private int halflead;
    
    //========================================================================
    
    /** Creates a new instance of InlineBox */
    public InlineBox(Element n, Graphics2D g, VisualContext ctx) 
    {
        super(n, g, ctx);
        halflead = 0;
    }
    
    public void copyValues(InlineBox src)
    {
        super.copyValues(src);
        valign = src.valign;
    }
    
    @Override
    public InlineBox copyBox()
    {
        InlineBox ret = new InlineBox(el, g, ctx);
        ret.copyValues(this);
        return ret;
    }
    
    //========================================================================
    
    @Override
    public String toString()
    {
        return "<" + el.getTagName() + " id=\"" + el.getAttribute("id") + 
               "\" class=\""  + el.getAttribute("class") + "\">";
    }
    
    @Override
    public void setStyle(NodeData s)
    {
        super.setStyle(s);
        loadInlineStyle();
    }
    
    public CSSProperty.VerticalAlign getVerticalAlign()
    {
        return valign;
    }
    
    /**
     * Assigns the line box assigned to this inline box and all the inline sub-boxes.
     * @param linebox The assigned linebox.
     */
    public void setLineBox(LineBox linebox)
    {
        this.linebox = linebox;
        for (int i = startChild; i < endChild; i++)
        {
            Box sub = getSubBox(i);
            if (sub instanceof InlineBox)
                ((InlineBox) sub).setLineBox(linebox);
        }
    }
    
    /**
     * Returns the line box used for positioning this element.
     */
    public LineBox getLineBox()
    {
        return linebox;
    }
    
    //========================================================================
    
    public int getBaselineOffset()
    {
    	if (curline == null)
    		return 0;
    	else
    		return curline.getBaselineOffset();
    }
    
    public int getBelowBaseline()
    {
    	if (curline == null)
    		return 0;
    	else
    		return curline.getBelowBaseline();
    }
    
    public int getTotalLineHeight()
    {
    	if (curline == null)
    		return 0;
    	else
    		return curline.getTotalLineHeight();
    }
    
    public int getMaxLineHeight()
    {
        if (curline == null)
            return lineHeight;
        else
            return Math.max(lineHeight, curline.getMaxLineHeight());
    }
    
    /**
     * Obtains the offset of the content edge from the line box top
     * @return the difference between the content edge and the top of the line box in pixels. Positive numbers mean the content box is inside the line box.  
     */
    public int getLineboxOffset()
    {
        if (curline == null)
            return 0;
        else
            return  curline.getBaselineOffset() - ctx.getBaselineOffset() - halflead;
    }
    /**
     * Returns the half-lead value used for positioning the nested boxes within this inline box
     * @return half-lead value in pixels
     */
    public int getHalfLead()
    {
        return halflead;
    }
    
    //========================================================================
    
	@Override
    public boolean isInFlow()
	{
		return true;
	}
	
	@Override
    public boolean containsFlow()
	{
		return !isempty;
	}
    
    /** Compute the width and height of this element. Layout the sub-elements.
     * @param availw Maximal width available to the child elements
     * @param force Use the area even if the used width is greater than maxwidth
     * @param linestart Indicates whether the element is placed at the line start
     * @return True if the box has been succesfully placed
     */
    @Override
    public boolean doLayout(int availw, boolean force, boolean linestart)
    {
        //Skip if not displayed
        if (!displayed)
        {
            content.setSize(0, 0);
            bounds.setSize(0, 0);
            return true;
        }

        setAvailableWidth(availw);
        
        curline = new LineBox(this, startChild, 0);
        int wlimit = getAvailableContentWidth();
        int x = 0; //current x
        boolean ret = true;
        rest = null;

        int lastbreak = startChild; //last possible position of a line break
        boolean lastwhite = false; //last box ends with a whitespace
        
        for (int i = startChild; i < endChild; i++)
        {
            Box subbox = getSubBox(i);
            if (subbox.canSplitBefore())
            	lastbreak = i;
            //when forcing, force the first child only and the children before
            //the first possible break
            boolean f = force && (i == startChild || lastbreak == startChild);
            if (lastwhite) subbox.setIgnoreInitialWhitespace(true);
            boolean fit = subbox.doLayout(wlimit - x, f, linestart && (i == startChild));
            if (fit) //something has been placed
            {
                if (subbox instanceof Inline)
                {
                    subbox.setPosition(x,  0); //the y position will be updated later
                    x += subbox.getWidth();
                    curline.considerBox((Inline) subbox);
                }
                else
                	System.err.println("Warning: doLayout(): subbox is not inline: " + subbox);
                if (subbox.getRest() != null) //is there anything remaining?
                {
                    InlineBox rbox = copyBox();
                    rbox.splitted = true;
                    rbox.setStartChild(i); //next starts with me...
                    rbox.nested.setElementAt(subbox.getRest(), i); //..but only with the rest
                    rbox.adoptChildren();
                    setEndChild(i+1); //...and this box stops with this element
                    rest = rbox;
                    break;
                }
            }
            else //nothing from the child has been placed
            {
                if (lastbreak == startChild) //no children have been placed, give up
                {
                    ret = false; 
                    break; 
                }
                else //some children have been placed, contintue the next time
                {
                    InlineBox rbox = copyBox();
                    rbox.splitted = true;
                    rbox.setStartChild(lastbreak); //next time start from the last break
                    rbox.adoptChildren();
                    setEndChild(lastbreak); //this box stops here
                    rest = rbox;
                    break;
                }
            }
            
            lastwhite = subbox.collapsesSpaces() && subbox.endsWithWhitespace(); 
            if (subbox.canSplitAfter())
            	lastbreak = i+1;
        }
        
        
        //compute the vertical positions of the boxes
        //updateLineMetrics();
        content.width = x;
        content.height = (int) Math.round(ctx.getFontHeight() * 1.2); //based on browser behaviour observations
        halflead = (content.height - curline.getTotalLineHeight()) / 2;
        alignBoxes();
        setSize(totalWidth(), totalHeight());
        
        return ret;
    }
    
    @Override
    public void absolutePositions()
    {
        if (isDisplayed())
        {
            //x coordinate is taken from the content edge
            absbounds.x = getParent().getAbsoluteContentX() + bounds.x;
            //y coordinate -- depends on the vertical alignment
            if (valign == CSSProperty.VerticalAlign.TOP)
            {
                absbounds.y = linebox.getAbsoluteY() + (linebox.getLead() / 2) - getContentOffsetY();
            }
            else if (valign == CSSProperty.VerticalAlign.BOTTOM)
            {
                absbounds.y = linebox.getAbsoluteY() + linebox.getTotalLineHeight() - getContentHeight() - getContentOffsetY();
            }
            else //other positions -- set during the layout. Relative to the parent content edge.
            {
                absbounds.y = getParent().getAbsoluteContentY() + bounds.y;
            }

            //update the width and height according to overflow of the parent
            absbounds.width = bounds.width;
            absbounds.height = bounds.height;
            
            //repeat for all valid subboxes
            for (int i = startChild; i < endChild; i++)
                getSubBox(i).absolutePositions();
        }
    }

    @Override
    public int getMinimalWidth()
    {
        //return the maximum of the nested minimal widths that are separated
        int ret = 0;
        for (int i = startChild; i < endChild; i++)
        {
            int w = getSubBox(i).getMinimalWidth();
            if (w > ret) ret = w;
        }
        //increase by margin, padding, border
        ret += margin.left + padding.left + border.left +
               margin.right + padding.right + border.right;
        return ret;
    }
    
    @Override
    public int getMaximalWidth()
    {
        //return the sum of all the elements inside
        int ret = 0;
        for (int i = startChild; i < endChild; i++)
            ret += getSubBox(i).getMaximalWidth();
        //increase by margin, padding, border
        ret += margin.left + padding.left + border.left +
               margin.right + padding.right + border.right;
        return ret;
    }
    
    /**
     * Returns the height of the box or the highest subbox.
     */
    public int getMaximalHeight()
    {
        int ret = getHeight();
        for (int i = startChild; i < endChild; i++)
        {
            Box sub = getSubBox(i);
            int h = 0;
            if (sub instanceof InlineBox)
                h = ((InlineBox) sub).getMaximalHeight();
            else
                h = sub.getHeight();
            
            if (h > ret) ret = h;
        }
        return ret;
    }
    
    @Override
    public boolean canSplitInside()
    {
        for (int i = startChild; i < endChild; i++)
            if (getSubBox(i).canSplitInside())
                return true;
        return false;
    }
    
    @Override
    public boolean canSplitBefore()
    {
        return (endChild > startChild) && getSubBox(startChild).canSplitBefore();
    }
    
    @Override
    public boolean canSplitAfter()
    {
        return (endChild > startChild) && getSubBox(endChild-1).canSplitAfter();
    }
    
    @Override
    public boolean startsWithWhitespace()
    {
        return (endChild > startChild) && getSubBox(startChild).startsWithWhitespace();
    }
    
    @Override
    public boolean endsWithWhitespace()
    {
        return (endChild > startChild) && getSubBox(endChild - 1).endsWithWhitespace();
    }
    
    @Override
    public void setIgnoreInitialWhitespace(boolean b)
    {
        if (endChild > startChild)
            getSubBox(startChild).setIgnoreInitialWhitespace(b);
    }
    
    /** Draw the specified stage (DRAW_*) */
    @Override
    public void draw(Graphics2D g, int turn, int mode)
    {
        ctx.updateGraphics(g);
        if (displayed)
        {
            Shape oldclip = g.getClip();
            g.setClip(clipblock.getAbsoluteContentBounds());
            if (turn == DRAW_ALL || turn == DRAW_NONFLOAT)
            {
                if (mode == DRAW_BOTH || mode == DRAW_BG) drawBackground(g);
            }
            
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                for (int i = startChild; i < endChild; i++)
                    getSubBox(i).draw(g, turn, mode);
            }
            g.setClip(oldclip);
        }
    }
    
    /**
     * @return the maximal line height of the contained sub-boxes
     */
    /*public int getMaxLineHeight()
    {
        return maxLineHeight;
    }*/
    
    @Override
    public int totalHeight()
    {
        //for inline boxes, the top and bottom margins don't apply
        return border.top + padding.top + content.height + padding.bottom + border.bottom;
    }
    
    //=======================================================================
    
    /**
     * Loads the basic style properties related to the inline elements.
     */
    protected void loadInlineStyle()
    {
        valign = style.getProperty("vertical-align");
        if (valign == null) valign = CSSProperty.VerticalAlign.BASELINE;
    }
    
    @Override
    protected void loadSizes()
    {
        CSSDecoder dec = new CSSDecoder(ctx);
        
        if (cblock == null)
            System.err.println(this + " has no cblock");
        
        //containing box sizes
        int contw = cblock.getContentWidth();
        
        //top and bottom margins take no effect for inline boxes
        // http://www.w3.org/TR/CSS21/box.html#propdef-margin-top
        margin = new LengthSet();
        margin.right = dec.getLength(getLengthValue("margin-right"), style.getProperty("margin-right") == CSSProperty.Margin.AUTO, 0, 0, contw);
        margin.left = dec.getLength(getLengthValue("margin-left"), style.getProperty("margin-left") == CSSProperty.Margin.AUTO, 0, 0, contw);
        emargin = new LengthSet(margin);

        loadBorders(dec, contw);
        
        padding = new LengthSet();
        padding.top = dec.getLength(getLengthValue("padding-top"), false, null, null, contw);
        padding.right = dec.getLength(getLengthValue("padding-right"), false, null, null, contw);
        padding.bottom = dec.getLength(getLengthValue("padding-bottom"), false, null, null, contw);
        padding.left = dec.getLength(getLengthValue("padding-left"), false, null, null, contw);
        
        content = new Dimension(0, 0);
    }
    
    @Override
    public void updateSizes()
    {
    	//no update needed - inline box size depends on the contents only
    }
   
    @Override
    public boolean hasFixedWidth()
    {
    	return false; //depends on the contents
    }
    
    @Override
    public boolean hasFixedHeight()
    {
    	return false; //depends on the contents
    }
    
    @Override
    public void computeEfficientMargins()
    {
        emargin.top = margin.top; //no collapsing is applied to inline boxes
        emargin.bottom = margin.bottom;
    }

    @Override
	public boolean marginsAdjoin()
	{
    	if (padding.top > 0 || padding.bottom > 0 ||
    		border.top > 0 || border.bottom > 0)
    	{
    		//margins are separated by padding or border
    		return false;
    	}
    	else
    	{
    		//margins can be separated by contents
	        for (int i = startChild; i < endChild; i++)
	        {
	        	Box box = getSubBox(i);
	        	if (box instanceof ElementBox) //all child boxes must have adjoining margins
	        	{
	        		if (!((ElementBox) box).marginsAdjoin())
	        			return false;
	        	}
	        	else
	        	{
	        		if (!box.isWhitespace()) //text boxes must be whitespace
	        			return false;
	        	}
	        }
	        return true;
    	}
	}
    
    //=====================================================================================================

    /**
     * Vertically aligns the contained boxes according to their vertical-align properties.
     */
    private void alignBoxes()
    {
        for (int i = startChild; i < endChild; i++)
        {
            Box sub = getSubBox(i);
            if (!sub.isblock)
            {
                //position relative to the line box
                int dif = curline.alignBox((Inline) sub);
                //recompute to the content box
                dif = dif - getLineboxOffset();
                //recompute to the bounding box
                if (sub instanceof InlineBox)
                    dif = dif - ((InlineBox) sub).getContentOffsetY();
                
                if (dif != 0)
                    sub.moveDown(dif);
            }
        }
    }
    
}
