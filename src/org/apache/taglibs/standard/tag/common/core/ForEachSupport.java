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

package org.apache.taglibs.standard.tag.common.core;

import java.util.*;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.sql.ResultSet;
import javax.servlet.jsp.jstl.core.IteratorTagSupport;
import org.apache.taglibs.standard.resources.Resources;

/**
 * <p>Support for tag handlers for &lt;forEach&gt;, the core iteration
 * tag in JSTL 1.0.  This class extends IteratorTagSupport and provides
 * ForEach-specific functionality.  The rtexprvalue library and the
 * expression-evaluating library each have handlers that extend this
 * class.</p>
 *
 * <p>Localized here is the logic for handling the veritable smorgasbord
 * of types supported by &lt;forEach&gt;, including arrays,
 * Collections, and others.  To see how the actual iteration is controlled,
 * review the javax.servlet.jsp.jstl.core.IteratorTagSupport class instead.
 * </p>
 *
 * @see javax.servlet.jsp.jstl.core.IteratorTagSupport
 * @author Shawn Bayern
 */

public abstract class ForEachSupport extends IteratorTagSupport {

    //*********************************************************************
    // Implementation overview

    /*
     * This particular handler is essentially a large switching mechanism
     * to support the various types that the <forEach> tag handles.  The
     * class is organized around the private ForEachIterator interface,
     * which serves as the basis for relaying information to the iteration
     * implementation we inherit from IteratorTagSupport.
     *
     * We expect to receive our 'items' from one of our subclasses
     * (presumably from the rtexprvalue or expression-evaluating libraries).
     * If 'items' is missing, we construct an Integer[] array representing
     * iteration indices, in line with the spec draft.  From doStartTag(),
     * we analyze and 'digest' the data we're passed.  Then, we simply
     * relay items as necessary to the iteration implementation that
     * we inherit from IteratorTagSupport.
     */


    //*********************************************************************
    // Internal, supporting classes and interfaces

    /*
     * Acts as a focal point for converting the various types we support.
     * It would have been ideal to use Iterator here except for one problem:
     * Iterator.hasNext() and Iterator.next() can't throw the JspTagException
     * we want to throw.  So instead, we'll encapsulate the hasNext() and
     * next() methods we want to provide inside this local class.
     * (Other implementations are more than welcome to implement hasNext()
     * and next() explicitly, not in terms of a back-end supporting class.
     * For the forEach tag handler, however, this class acts as a convenient
     * organizational mechanism, for we support so many different classes.
     * This encapsulation makes it easier to localize implementations
     * in support of particular types -- e.g., changing the implementation
     * of primitive-array iteration to wrap primitives only on request,
     * instead of in advance, would involve changing only those methods that
     * handle primitive arrays.
     */
    protected static interface ForEachIterator {
        public boolean hasNext() throws JspTagException;
        public Object next() throws JspTagException;
    }

    /*
     * Simple implementation of ForEachIterator that adapts from
     * an Iterator.  This is appropriate for cases where hasNext() and
     * next() don't need to throw JspTagException.  Such cases are common.core.
     */
    protected class SimpleForEachIterator implements ForEachIterator {
        private Iterator i;
        public SimpleForEachIterator(Iterator i) {
            this.i = i;
        }
        public boolean hasNext() {
            return i.hasNext();
        }
        public Object next() {
            return i.next();
        }
    }


    //*********************************************************************
    // ForEach-specifc state (protected)

    protected ForEachIterator items;              // our 'digested' items
    protected Object rawItems;                    // our 'raw' items


    //*********************************************************************
    // Iteration control methods (based on processed 'items' object)

    protected boolean hasNext() throws JspTagException {
        return items.hasNext();
    }

    protected Object next() throws JspTagException {
        return items.next();
    }


    //*********************************************************************
    // Tag logic and lifecycle management

    // Releases any resources we may have (or inherit)
    public void release() {
        super.release();
        items = null;
        rawItems = null;
    }

    /*
     * Produces and stores an appropriate ForEachIterator and performs
     * some custom checks, then defers to the parent's implementation.
     */
    public int doStartTag() throws JspException {

        // produce the right sort of ForEachIterator
        if (rawItems != null) {
            // extract an iterator over the 'items' we've got
            items = supportedTypeForEachIterator(rawItems);
        } else {
            // no 'items', so use 'begin' and 'end'
            items = beginEndForEachIterator();
        }

        // step must be 1 when ResultSet is passed in
        if (rawItems instanceof ResultSet && step != 1)
            throw new JspTagException(
		Resources.getMessage("FOREACH_STEP_NO_RESULTSET"));

        // now we're ready...
        return super.doStartTag();
    }


    //*********************************************************************
    // Private generation methods for the ForEachIterators we produce

    /* Extracts a ForEachIterator given an object of a supported type. */
    protected ForEachIterator supportedTypeForEachIterator(Object o)
            throws JspTagException {

        /*
         * This is, of necessity, just a big, simple chain, matching in
         * order.  Since we are passed on Object because of all the
         * various types we support, we cannot rely on the language's
         * mechanism for resolving overloaded methods.  (Method overloading
         * resolves via early binding, so the type of the 'o' reference,
         * not the type of the eventual value that 'o' references, is
         * all that's available.)
         *
         * Currently, we 'match' on the object we have through an
         * if/else chain that picks the first interface (or class match)
         * found for an Object.
         */

        ForEachIterator items;

        if (o instanceof Object[])
            items = toForEachIterator((Object[]) o);
        else if (o instanceof boolean[])
            items = toForEachIterator((boolean[]) o);
        else if (o instanceof byte[])
            items = toForEachIterator((byte[]) o);
        else if (o instanceof char[])
            items = toForEachIterator((char[]) o);
        else if (o instanceof short[])
            items = toForEachIterator((short[]) o);
        else if (o instanceof int[])
            items = toForEachIterator((int[]) o);
        else if (o instanceof long[])
            items = toForEachIterator((long[]) o);
        else if (o instanceof float[])
            items = toForEachIterator((float[]) o);
        else if (o instanceof double[])
            items = toForEachIterator((double[]) o);
        else if (o instanceof Collection)
            items = toForEachIterator((Collection) o);
        else if (o instanceof Iterator)
            items = toForEachIterator((Iterator) o);
        else if (o instanceof Enumeration)
            items = toForEachIterator((Enumeration) o);
        else if (o instanceof Map)
            items = toForEachIterator((Map) o);
        else if (o instanceof ResultSet)
            items = toForEachIterator((ResultSet) o);
        else if (o instanceof String)
            items = toForEachIterator((String) o);
        else
            items = toForEachIterator(o);

        return (items);
    }

    /*
     * Creates a ForEachIterator of Integers from 'begin' to 'end'
     * in support of cases where our tag handler isn't passed an
     * explicit collection over which to iterate.
     */
    private ForEachIterator beginEndForEachIterator() {
        /*
         * To plug into existing support, we need to keep 'begin', 'end',
         * and 'step' as they are.  So we'll simply create an Integer[]
         * from 0 to 'end', inclusive, and let the existing implementation
         * handle the subsetting and stepping operations.  (Other than
         * localizing the cost of creating this Integer[] to the start
         * of the operation instead of spreading it out over the lifetime
         * of the iteration, this implementation isn't worse than one that
         * created new Integers() as needed from next().  Such an adapter
         * to ForEachIterator could easily be written but, like I said,
         * wouldn't provide much benefit.)
         */
        Integer[] ia = new Integer[end + 1];
        for (int i = 0; i <= end; i++)
            ia[i] = new Integer(i);
        return new SimpleForEachIterator(Arrays.asList(ia).iterator());
    }


    //*********************************************************************
    // Private conversion methods to handle the various types we support

    // catch-all method whose invocation currently signals a 'matching error'
    protected ForEachIterator toForEachIterator(Object o)
            throws JspTagException {
        throw new JspTagException(Resources.getMessage("FOREACH_BAD_ITEMS"));
    }

    // returns an iterator over an Object array (via List)
    protected ForEachIterator toForEachIterator(Object[] a) {
        return new SimpleForEachIterator(Arrays.asList(a).iterator());
    }

    // returns an iterator over a boolean[] array, wrapping items in Boolean
    protected ForEachIterator toForEachIterator(boolean[] a) {
        Boolean[] wrapped = new Boolean[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Boolean(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a byte[] array, wrapping items in Byte
    protected ForEachIterator toForEachIterator(byte[] a) {
        Byte[] wrapped = new Byte[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Byte(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a char[] array, wrapping items in Character
    protected ForEachIterator toForEachIterator(char[] a) {
        Character[] wrapped = new Character[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Character(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a short[] array, wrapping items in Short
    protected ForEachIterator toForEachIterator(short[] a) {
        Short[] wrapped = new Short[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Short(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over an int[] array, wrapping items in Integer
    protected ForEachIterator toForEachIterator(int[] a) {
        Integer[] wrapped = new Integer[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Integer(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a long[] array, wrapping items in Long
    protected ForEachIterator toForEachIterator(long[] a) {
        Long[] wrapped = new Long[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Long(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a float[] array, wrapping items in Float
    protected ForEachIterator toForEachIterator(float[] a) {
        Float[] wrapped = new Float[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Float(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // returns an iterator over a double[] array, wrapping items in Double
    protected ForEachIterator toForEachIterator(double[] a) {
        Double[] wrapped = new Double[a.length];
        for (int i = 0; i < a.length; i++)
            wrapped[i] = new Double(a[i]);
        return new SimpleForEachIterator(Arrays.asList(wrapped).iterator());
    }

    // retrieves an iterator from a Collection
    protected ForEachIterator toForEachIterator(Collection c) {
        return new SimpleForEachIterator(c.iterator());
    }

    // simply passes an Iterator through...
    protected ForEachIterator toForEachIterator(Iterator i) {
        return new SimpleForEachIterator(i);
    }

    // converts an Enumeration to an Iterator via a local adapter
    protected ForEachIterator toForEachIterator(Enumeration e) {

        // local adapter
        class EnumerationAdapter implements ForEachIterator {
            private Enumeration e;
            public EnumerationAdapter(Enumeration e) {
                this.e = e;
            }
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            public Object next() {
                return e.nextElement();
            }
        }

        return new EnumerationAdapter(e);
    }

    // retrieves an iterator over the Map.Entry items in a Map
    protected ForEachIterator toForEachIterator(Map m) {
        return new SimpleForEachIterator(m.entrySet().iterator());
    }

    // thinly wraps a ResultSet in an appropriate Iterator
    protected ForEachIterator toForEachIterator(ResultSet rs)
            throws JspTagException {

        // local adapter
        class ResultSetAdapter implements ForEachIterator {
            private ResultSet rs;
            public ResultSetAdapter(ResultSet rs) {
                this.rs = rs;
            }
            public boolean hasNext() throws JspTagException {
                try {
                    return !(rs.isLast());      // dependent on JDBC 2.0
                } catch (java.sql.SQLException ex) {
                    throw new JspTagException(ex.getMessage());
                }
            }
            public Object next() throws JspTagException {
                try {
                    rs.next();
                    return rs;
                } catch (java.sql.SQLException ex) {
                    throw new JspTagException(ex.getMessage());
                }
            }
        }

        return new ResultSetAdapter(rs);
    }

    // tokenizes a String as a CSV and returns an iterator over it
    protected ForEachIterator toForEachIterator(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        return toForEachIterator(st);           // convert from Enumeration
    }

}