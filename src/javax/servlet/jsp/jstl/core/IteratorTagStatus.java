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

/**
 * <p>Provides an interface for objects representing the current status of
 * an iteration.  JSTL 1.0 provides a mechanism for IteratorTags to
 * return information about the current index of the iteration and
 * convenience methods to determine whether or not the current round is
 * either the first or last in the iteration.  It also lets authors
 * use the status object to obtain information about the iteration range,
 * step, label, and current object.</p>
 *
 * <p>Environments that require more status can extend this interface.</p>
 *
 * @author Shawn Bayern
 */

public interface IteratorTagStatus {

    /**
     * Retrieves the current item in the iteration.  Behaves
     * idempotently; calling getCurrent() repeatedly should return the same
     * Object until the iteration is advanced.  (Specifically, calling
     * getCurrent() does <b>not</b> advance the iteration.)
     *
     * @return the current item as an object
     */
    public Object getCurrent();

    /**
     * Retrieves the index of the current round of the iteration.  If
     * iteration is being performed over a subset of an underlying
     * array, java.lang.Collection, or other type, the index returned
     * is absolute with respect to the underlying collection.  Indices
     * are 0-based.
     *
     * @return the 0-based index of the current round of the iteration
     */
    public int getIndex();

    /**
     * <p>Retrieves the "count" of the current round of the iteration.  The
     * count is a relative, 1-based sequence number identifying the
     * current "round" of iteration (in context with all rounds the
     * current iteration will perform).</p>
     *
     * <p>As an example, an iteration with begin = 5, end = 15, and step =
     * 5 produces the counts 1, 2, and 3 in that order.</p>
     *
     * @return the 1-based count of the current round of the iteration
     */
    public int getCount();

    /**
     * Returns information about whether the current round of the
     * iteration is the first one.  This current round may be the 'first'
     * even when getIndex() != 0, for 'index' refers to the absolute
     * index of the current 'item' in the context of its underlying
     * collection.  It is always that case that a true result from
     * isFirst() implies getCount() == 1.
     * 
     * @return <tt>true</tt> if the current round is the first in the
     * iteration, <tt>false</tt> otherwise.
     */
    public boolean isFirst();

    /**
     * Returns information about whether the current round of the
     * iteration is the last one.  As with isFirst(), subsetting is
     * taken into account.  isLast() doesn't necessarily refer to the
     * status of the underlying Iterator; it refers to whether or not
     * the current round will be the final round of iteration for the
     * tag associated with this IteratorTagStatus.
     * 
     * @return <tt>true</tt> if the current round is the last in the
     * iteration, <tt>false</tt> otherwise.
     */
    public boolean isLast();

    /**
     * Returns information about whether the 'begin' attribute was
     * specified.
     *
     * @return <tt>true</tt> if 'begin' was specified, <tt>false</tt>
     * otherwise.
     */
    public boolean isBeginSpecified();

    /**
     * Returns information about whether the 'end' attribute was
     * specified.
     *
     * @return <tt>true</tt> if 'end' was specified, <tt>false</tt>
     * otherwise.
     */
    public boolean isEndSpecified();

    /**
     * Returns information about whether the 'step' attribute was
     * specified.
     *
     * @return <tt>true</tt> if 'step' was specified, <tt>false</tt>
     * otherwise.
     */
    public boolean isStepSpecified();

    /**
     * Returns the value of the 'begin' attribute for the associated tag.  
     * If this attribute was not specified, the result is undefined.
     * (isBeginSpecified() should be called prior to this method to ensure
     * that the result is sensible.)
     *
     * @return the 'begin' value for the associated tag, or an undefined
     * value if this attribute was not specified.
     */
    public int getBegin();

    /**
     * Returns the value of the 'end' attribute for the associated tag.  
     * If this attribute was not specified, the result is undefined.
     * (isEndSpecified() should be called prior to this method to ensure
     * that the result is sensible.)
     *
     * @return the 'end' value for the associated tag, or an undefined
     * value if this attribute was not specified.
     */
    public int getEnd();

    /**
     * Returns the value of the 'step' attribute for the associated tag.  
     * If this attribute was not specified, the result is undefined.
     * (isStepSpecified() should be called prior to this method to ensure
     * that the result is sensible.)
     *
     * @return the 'step' value for the associated tag, or an undefined
     * value if this attribute was not specified.
     */
    public int getStep();

/*-- No labels in EA2
    **
     * Returns the label of the associated tag.  This label identifies
     * this tag (against other IteratorTags in a nested chain) primarily
     * for the benefit of subtags.
     *
     * @return the label of the associated tag
     *
    public String getLabel();
*/
}
