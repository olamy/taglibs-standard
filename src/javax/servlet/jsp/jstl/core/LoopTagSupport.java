/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

package javax.servlet.jsp.jstl.core;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * <p>JSTL 1.0 allows developers to write custom iteration tags by
 * implementing the LoopTag interface.  (This is not to be confused with
 * javax.servlet.jsp.tagext.IterationTag as defined in JSP 1.2.) 
 * LoopTag establishes a mechanism for iteration tags to be recognized
 * and for type-safe communication with custom subtags.
 * </p>
 * 
 * <p>Since most iteration tags will behave identically with respect to
 * actual iterative behavior, however, JSTL 1.0 provides this
 * base support class to facilitate implementation.  Many iteration tags
 * will extend this and merely implement the hasNext() and next() methods
 * to provide contents for the handler to iterate over.</p>
 *
 * <p>In particular, this base class provides support for:</p>
 * 
 * <ul>
 *  <li> iteration control, based on protected next() and hasNext() methods
 *  <li> subsetting (begin, end, step functionality, including validation
 *       of subset parameters for sensibility)
 *  <li> item retrieval (getCurrent())
 *  <li> status retrieval (LoopTagStatus)
 *  <li> exposing attributes (set by 'var' and 'status' attributes)
 * </ul>
 *
 * <p>In providing support for these tasks, LoopTagSupport contains
 * certain control variables that act to modify the iteration.  Accessors
 * are provided for these control variables when the variables represent
 * information needed or wanted at translation time (e.g., var, status).  For
 * other variables, accessors cannot be provided here since subclasses
 * may differ on their implementations of how those accessors are received.
 * For instance, one subclass might accept a String and convert it into
 * an object of a specific type by using an expression evaluator; others
 * might accept objects directly.  Still others might not want to expose
 * such information to outside control.</p>
 *
 * @author Shawn Bayern
 */

public abstract class LoopTagSupport
    extends TagSupport
    implements LoopTag, IterationTag, TryCatchFinally
{
    //*********************************************************************
    // 'Protected' state 

    /*
     * JavaBean-style properties and other state slaved to them.  These
     * properties can be set directly by accessors; they will not be
     * modified by the LoopTagSupport implementation -- and should
     * not be modified by subclasses outside accessors unless those
     * subclasses are perfectly aware of what they're doing.
     * (An example where such non-accessor modification might be sensible
     * is in the doStartTag() method of an EL-aware subclass.)
     */

    /*-- No labels in EA2
    ** 'label' attribute *
    protected String label;
    --*/

    /** Starting index ('begin' attribute) */
    protected int begin;

    /**
     * Ending index ('end' attribute).  -1 internally indicates 'no end
     * specified', although accessors for the core JSTL tags do not
     * allow this value to be supplied directly by the user.
     */
    protected int end;

    /** Iteration step ('step' attribute) */
    protected int step;

    /** Boolean flag indicating whether 'begin' was specified. */
    protected boolean beginSpecified;

    /** Boolean flag indicating whether 'end' was specified. */
    protected boolean endSpecified;

    /** Boolean flag indicating whether 'step' was specified. */
    protected boolean stepSpecified;

    /** Attribute-exposing control */
    protected String itemId, statusId /*, itemType */;


    //*********************************************************************
    // 'Private' state (implementation details)

    /*
     * State exclusively internal to the default, reference implementation.
     * (While this state is kept private to ensure consistency, 'status'
     * and 'item' happen to have one-for-one, read-only, accesor methods
     * as part of the LoopTag interface.)
     *
     * 'last' is kept separately for two reasons:  (a) to avoid
     * running a computation every time it's requested, and (b) to
     * let LoopTagStatus.isLast() avoid throwing any exceptions,
     * which would complicate subtag and scripting-variable use.
     *
     * Our 'internal index' begins at 0 and increases by 'step' each
     * round; this is arbitrary, but it seemed a simple way of keeping
     * track of the information we need.  To avoid computing
     * getIteratorStatus().getCount() by dividing index / step, we keep
     * a separate 'count' and increment it by 1 each round (as a minor
     * performance improvement).
     */
    private LoopTagStatus status;           // our LoopTagStatus
    private Object item;                        // the current item
    private int index;                          // the current internal index
    private int count;                          // the iteration count
    private boolean last;                       // current round == last one?


    //*********************************************************************
    // Constructor

    /**
     * Constructs a new LoopTagSupport.  As with TagSupport, subclasses
     * should not provide other constructors and are expected to call
     * the superclass constructor
     */
    public LoopTagSupport() {
        super();
        init();
    }


    //*********************************************************************
    // Abstract methods

    /**
     * <p>Returns the next object over which the tag should iterate.  This
     * method must be provided by concrete subclasses of LoopTagSupport
     * to inform the base logic about what objects it should iterate over.</p>
     *
     * <p>It is expected that this method will generally be backed by an
     * Iterator, but this will not always be the case.  In particular, if
     * retrieving the next object raises the possibility of an exception
     * being thrown, this method allows that exception to propagate back
     * to the JSP container as a JspTagException; a standalone Iterator
     * would not be able to do this.  (This explains why LoopTagSupport
     * does not simply call for an Iterator from its subtags.)</p>
     * 
     * @return the java.lang.Object to use in the next round of iteration
     * @exception NoSuchElementException
     *            if next() is called but no new elements are available
     * @exception javax.servlet.jsp.JspTagException
     *            for other, unexpected exceptions
     */
    protected abstract Object next() throws JspTagException;

    /**
     * <p>Returns information concerning the availability of more items
     * over which to iterate.  This method must be provided by concrete
     * subclasses of LoopTagSupport to assist the iterative logic
     * provided by the supporting base class.</p>
     *  
     * <p>See <a href="#next()">next</a> for more information about the
     * purpose and expectations behind this tag.</p>
     *
     * @return <tt>true</tt> if there is at least one more item to iterate
     *         over, <tt>false</tt> otherwise
     * @exception javax.servlet.jspTagException
     * @see #next
     */
    protected abstract boolean hasNext() throws JspTagException;

    /**
     * <p>Prepares for a single tag invocation.  Specifically, allows
     * subclasses to prepare for calls to hasNext() and next(). 
     * Subclasses can assume that prepare() will be called once for
     * each invocation of doStartTag() in the superclass.</p>
     *
     * @exception javax.servlet.jspTagException
     */
    protected abstract void prepare() throws JspTagException;


    //*********************************************************************
    // Lifecycle management and implementation of iterative behavior

    // Releases any resources we may have (or inherit)
    public void release() {
        super.release();
        init();
    }

    /* Begins iterating by processing the first item. */
    public int doStartTag() throws JspException {

        // make sure 'begin' isn't greater than 'end'
        if (end != -1 && begin > end)
            return SKIP_BODY;                   // nothing to do

        // we're beginning a new iteration, so reset our counts (etc.)
        index = 0;
        count = 1;
        last = false;

        // let the subclass conduct any necessary preparation
        prepare();

        // throw away the first 'begin' items (if they exist)
        discardIgnoreSubset(begin);

        // get the item we're interested in
        if (hasNext())
            // index is 0-based, so we don't update it for the first item
            item = next();
        else
            return SKIP_BODY;

        /*
         * now discard anything we have to "step" over.
         * (we do this in advance to support LoopTagStatus.isLast())
         */
        discard(step - 1);

        // prepare to include our body...
        exposeVariables();
        calibrateLast();
        return EVAL_BODY_INCLUDE;
    }

    /*
     * Continues the iteration when appropriate -- that is, if we (a) have
     * more items and (b) don't run over our 'end' (given our 'step').
     */
    public int doAfterBody() throws JspException {

        // re-sync the index, given our prior behind-the-scenes 'step'
        index += step - 1;

        // increment the count by 1 for each round
        count++;

        // everything's been prepared for us, so just get the next item
        if (hasNext() && !atEnd()) {
            index++;
            item = next();
        } else
            return SKIP_BODY;

        /*
         * now discard anything we have to "step" over.
         * (we do this in advance to support LoopTagStatus.isLast())
         */
        discard(step - 1);

        // prepare to re-iterate...
        exposeVariables();
        calibrateLast();
        return EVAL_BODY_AGAIN;
    }

    /*
     * Removes attributes that our tag set; these attributes are intended
     * to support scripting variables with NESTED scope, so we don't want
     * to pollute attribute space by leaving them lying around.
     */
    public void doFinally() {
	/*
	 * This always gets called, which introduces a minor danger:
	 * we might destroy attributes we *didn't* set if an exception
	 * stops us before we set those attributes.  However, since our
	 * setting of those attributes is destructive anyway (with respect
	 * to attributes previously stored under the names we're directed
	 * to use), this doesn't seem like a problem.
         */
	unExposeVariables();
    }

    /*
     * Be transparent with respect to exceptions: rethrow anything we get.
     */
    public void doCatch(Throwable t) throws Throwable {
	throw t;
    }

    //*********************************************************************
    // Accessor methods

    /*
     * Overview:  The getXXX() methods we provide implement the Tag
     * contract.  setXXX() accessors are provided only for those
     * properties (attributes) that must be known at translation time,
     * on the premise that these accessors will vary less than the
     * others in terms of their interface with the page author.
     */

    /*
     * (Purposely inherit JavaDoc and semantics from LoopTag.
     * Subclasses can override this if necessary, but such a need is
     * expected to be rare.)
     */
    public Object getCurrent() {
        return item;
    }

    /*
     * (Purposely inherit JavaDoc and semantics from LoopTag.
     * Subclasses can override this method for more fine-grained control
     * over LoopTagStatus, but an effort has been made to simplify
     * implementation of subclasses that are happy with reasonable default
     * behavior.)
     */
    public LoopTagStatus getIteratorStatus() {

        // local implementation with reasonable default behavior
        class Status implements LoopTagStatus {

            /*
             * All our methods are straightforward.  We inherit
             * our JavaDoc from LoopTagSupport; see that class
             * for more information.
             */

            public Object getCurrent() {
                /*
                 * Access the item through getCurrent() instead of just
                 * returning the item our containing class stores.  This
                 * should allow a subclass of LoopTagSupport to override
                 * getCurrent() without having to rewrite getIteratorStatus() too.
                 */
                return (LoopTagSupport.this.getCurrent());
            }
            public int getIndex() {
                return (index + begin);       // our 'index' isn't getIndex()
            }
            public int getCount() {
                return (count);
            }
            public boolean isFirst() {
                return (index == 0);          // our 'index' isn't getIndex()
            }
            public boolean isLast() {
                return (last);                // use cached value
            }
            public boolean isBeginSpecified() {
                return (beginSpecified);
            }
            public boolean isEndSpecified() {
                return (endSpecified);
            }
            public boolean isStepSpecified() {
                return (stepSpecified);
            }
            public int getBegin() {
                return (begin);
            }
            public int getEnd() {
                return (end);
            }
            public int getStep() {
                return (step);
            }
	    /*-- No labels in EA2
            public String getLabel() {
                return (label);
            }
	    --*/
        }

        /*
         * We just need one per invocation...  Actually, for the current
         * implementation, we just need one per instance, but I'd rather
         * not keep the reference around once release() has been called.
         */
        if (status == null)
            status = new Status();

        return status;
    }

    /*
     * We only support setter methods for attributes that need to be
     * offered as Strings or other literals; other attributes will be
     * handled directly by implementing classes, since there might be
     * both rtexprvalue- and EL-based varieties, which will have
     * different signatures.  (We can't pollute child classes by having
     * base implementations of those setters here; child classes that
     * have attributes with different signatures would end up having
     * two incompatible setters, which is illegal for a JavaBean.
     */

    /*-- No labels in EA2
    // for tag attribute
    public void setLabel(String label) {
        this.label = label;
    }
    --*/
 
    // for tag attribute
    public void setVar(String id) {
        this.itemId = id;
    }

    /* NO LONGER NEEDED
    // for tag attribute
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    */

    // for tag attribute
    public void setStatus(String statusId) {
        this.statusId = statusId;
    }

    //*********************************************************************
    // Public static (utility) methods

    /*-- No labels in EA2
    **
     * Locates the nearest ancestor LoopTag with the given label,
     * starting at the Tag given as the 'base'.  If label is null, simply
     * locates the nearest LoopTag ancestor to 'base'.
     *
     * @param  base   the Tag at which to start the search (that is, the
     *                Tag whose ancestors to search)
     * @param  label  the label to search for, or 'null' if any LoopTag
     *                is suitable
     *
     * @return the LoopTag found, or 'null' if no matching LoopTag
     * was found
     *
    public static LoopTag findIteratorAncestorWithLabel(
            Tag base, String label) {

        // find the first LoopTag ancestor
        LoopTag it =
            (LoopTag) findAncestorWithClass(base, LoopTag.class);

        // if we want a specific label, search for it up the tree
        while (it != null && label != null
                && !label.equals(it.getIteratorStatus().getLabel())) {
            it = (LoopTag) findAncestorWithClass(it, LoopTag.class);
        }

        // return what we've got (which might be null)
        return it;
    }
    --*/

    //*********************************************************************
    // Protected utility methods

    /* 
     * These methods validate attributes common to iteration tags.
     * Call them if your own subclassing implementation modifies them
     * -- e.g., if you set them through an expression language.
     */

    /*
     * Ensures the "begin" property is sensible, throwing an exception
     * expected to propagate up if it isn't
     */
    protected void validateBegin() throws JspTagException {
        if (begin < 0)
            throw new JspTagException("'begin' < 0");
    }

    /*
     * Ensures the "end" property is sensible, throwing an exception
     * expected to propagate up if it isn't
     */
    protected void validateEnd() throws JspTagException {
        if (end < 0)
            throw new JspTagException("'end' < 0");
    }

    /*
     * Ensures the "step" property is sensible, throwing an exception
     * expected to propagate up if it isn't
     */
    protected void validateStep() throws JspTagException {
        if (step < 1)
            throw new JspTagException("'step' <= 0");
    }


    //*********************************************************************
    // Private utility methods

    // (re)initializes state (during release() or construction)
    private void init() {
        // defaults for internal bookkeeping
        index = 0;              // internal index always starts at 0
        count = 1;              // internal count always starts at 1
        status = null;          // we clear status on release()
        item = null;            // item will be retrieved for each round
        last = false;           // last must be set explicitly
        beginSpecified = false; // not specified until it's specified :-)
        endSpecified = false;   // (as above)
        stepSpecified = false;  // (as above)

        // defaults for interface with page author
        begin = 0;              // when not specified, 'begin' is 0 by spec.
        end = -1;               // when not specified, 'end' is not used
        step = 1;               // when not specified, 'step' is 1
        itemId = null;          // when not specified, no variable exported
        // itemType = null;        // when not specified, no variable exported
        statusId = null;        // when not specified, no variable exported
    }

    // sets 'last' appropriately
    private void calibrateLast() throws JspTagException {
        /*
         * the current round is the last one if (a) there are no remaining
         * elements, or (b) the next one is beyond the 'end'.
         */
        last = !hasNext() || atEnd() ||
            (end != -1 && (begin + index + step > end));
    }

    /*
     * Exposes attributes (formerly scripting variables, but no longer!)
     * if appropriate.  Note that we don't really care, here, whether they're
     * scripting variables or not.
     */
    private void exposeVariables() throws JspTagException {

        /*
         * We need to support null items returned from next(); we
         * do this simply by passing such non-items through to the
         * scripting variable as 'null' (which we ensure by calling
         * removeAttribute()).
         *
         * Also, just to be defensive, we handle the case of a null
         * 'status' object as well.
         *
         * We call getCurrent() and getIteratorStatus() (instead of just using
         * 'item' and 'status') to bridge to subclasses correctly.
         * A subclass can override getCurrent() or getIteratorStatus() but still
         * depend on our doStartTag() and doAfterBody(), which call this
         * method (exposeVariables()), to expose 'item' and 'status'
         * correctly.
         */

        if (itemId != null) {
            if (getCurrent() == null)
                pageContext.removeAttribute(itemId);
            else
                pageContext.setAttribute(itemId, getCurrent());
        }
        if (statusId != null) {
            if (getIteratorStatus() == null)
                pageContext.removeAttribute(statusId);
            else
                pageContext.setAttribute(statusId, getIteratorStatus());
        }

    }

    // removes page attributes if appropriate
    private void unExposeVariables() {
	if (itemId != null)
	    pageContext.removeAttribute(itemId, PageContext.PAGE_SCOPE);
	if (statusId != null)
	    pageContext.removeAttribute(statusId, PageContext.PAGE_SCOPE);
    }

    /*
     * Cycles through and discards up to 'n' items from the iteration.
     * We only know "up to 'n'", not "exactly n," since we stop cycling
     * if hasNext() returns false or if we hit the 'end' of the iteration.
     * Note: this does not update the iteration index, since this method
     * is intended as a behind-the-scenes operation.  The index must be
     * updated separately.  (I don't really like this, but it's the simplest
     * way to support isLast() without storing two separate inconsistent
     * indices.  We need to (a) make sure hasNext() refers to the next
     * item we actually *want* and (b) make sure the index refers to the
     * item associated with the *current* round, not the next one.
     * C'est la vie.)
     */
    private void discard(int n) throws JspTagException {
        /*
         * copy index so we can restore it, but we need to update it
         * as we work so that atEnd() works
         */
        int oldIndex = index;
        while (n-- > 0 && !atEnd() && hasNext()) {
            index++;
            next();
        }
        index = oldIndex;
    }

    /*
     * Discards items ignoring subsetting rules.  Useful for discarding
     * items from the beginning (i.e., to implement 'begin') where we
     * don't want factor in the 'begin' value already.
     */
    private void discardIgnoreSubset(int n) throws JspTagException {
	while (n-- > 0 && hasNext())
	    next();
    }

    /*
     * Returns true if the iteration has past the 'end' index (with
     * respect to subsetting), false otherwise.  ('end' must be set
     * for atEnd() to return true; if 'end' is not set, atEnd()
     * always returns false.)
     */
    private boolean atEnd() {
        return ((end != -1) && (begin + index >= end));
    }
}
