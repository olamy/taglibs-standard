/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.taglibs.standard.tag.common.core;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

import javax.el.ELException;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.el.ExpressionFactory;

import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.taglibs.standard.resources.Resources;

/**
 * <p>Support for handlers of the &lt;set&gt; tag.</p>
 *
 * <p>The protected <code>value</code> and <code>valueSpecified</code>
 * attributes must be set in sync. That is, if you set the value then 
 * you should set <code>valueSpecified</code> to <code>true<code>, if you unset the value, then 
 * you should set <code>valueSpecified</code> to <code>false<code>. </p>
 *
 * @author Shawn Bayern
 */
public class SetSupport extends BodyTagSupport {

    //*********************************************************************
    // Internal state

    protected Object value;             // tag attribute
    protected boolean valueSpecified;   // status
    protected Object target;            // tag attribute
    protected String property;          // tag attribute
    private String var;                 // tag attribute
    private String scope;               // tag attribute

    //*********************************************************************
    // Construction and initialization

    /**
     * Constructs a new handler.  As with TagSupport, subclasses should
     * not provide other constructors and are expected to call the
     * superclass constructor.
     */
    public SetSupport() {
        super();
        init();
    }

    // resets local state
    private void init() {
        value = target = property = var = scope = null;
        valueSpecified = false;
    }

    // Releases any resources we may have (or inherit)
    public void release() {
        init();
        super.release();
    }


    //*********************************************************************
    // Tag logic

    public int doEndTag() throws JspException {

        // what we'll store in scope:var
        Object result = getResult();

        // decide what to do with the result
        if (var != null) {
            exportToVariable(result);
        } else if (target == null) {
            // can happen if target evaluates to null
            throw new JspTagException(Resources.getMessage("SET_INVALID_TARGET"));
        } else if (target instanceof Map) {
            exportToMapProperty(result);
        } else {
            exportToBeanProperty(result);
        }

        return EVAL_PAGE;
    }

    Object getResult() {
        if (valueSpecified) {
            return value;
        } else if (bodyContent == null) {
            return "";
        } else {
            String content = bodyContent.getString();
            if (content == null) {
                return "";
            } else {
                return content.trim();
            }
        }
    }

    /**
     * Export the result into a scoped variable.
     *
     * @param result the value to export
     * @throws JspTagException if there was a problem exporting the result
     */
    void exportToVariable(Object result) throws JspTagException {
        /*
        * Store the result, letting an IllegalArgumentException
        * propagate back if the scope is invalid (e.g., if an attempt
        * is made to store something in the session without any
        * HttpSession existing).
        */
        int scopeValue = Util.getScope(scope);
        ELContext myELContext = pageContext.getELContext();
        VariableMapper vm = myELContext.getVariableMapper();
        if (result != null) {
            // if the result is a ValueExpression we just export to the mapper
            if (result instanceof ValueExpression) {
                if (scopeValue != PageContext.PAGE_SCOPE) {
                    throw new JspTagException(Resources.getMessage("SET_BAD_DEFERRED_SCOPE", scope));
                }
                vm.setVariable(var, (ValueExpression)result);
            } else {
                // make sure to remove it from the VariableMapper if we will be setting into page scope
                if (scopeValue == PageContext.PAGE_SCOPE && vm.resolveVariable(var) != null) {
                    vm.setVariable(var, null);
                }
                pageContext.setAttribute(var, result, scopeValue);
            }
        } else {
            //make sure to remove it from the Var mapper
            if (vm.resolveVariable(var)!=null) {
                vm.setVariable(var, null);
            }
            if (scope != null) {
                pageContext.removeAttribute(var, Util.getScope(scope));
            } else {
                pageContext.removeAttribute(var);
            }
        }
    }

    /**
     * Export the result into a Map.
     *
     * @param result the value to export
     */
    void exportToMapProperty(Object result) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) target;
        if (result == null) {
            map.remove(property);
        } else {
            map.put(property, result);
        }
    }

    /**
     * Export the result into a bean property.
     *
     * @param result the value to export
     * @throws JspTagException if there was a problem exporting the result
     */
    void exportToBeanProperty(Object result) throws JspTagException {
        PropertyDescriptor[] descriptors;
        try {
            descriptors = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();
        } catch (IntrospectionException ex) {
            throw new JspTagException(ex);
        }

        for (PropertyDescriptor pd : descriptors) {
            if (pd.getName().equals(property)) {
                Method m = pd.getWriteMethod();
                if (m == null) {
                    throw new JspTagException(Resources.getMessage("SET_NO_SETTER_METHOD", property));
                }
                try {
                    m.invoke(target, convertToExpectedType(result, m));
                } catch (ELException ex) {
                    throw new JspTagException(ex);
                } catch (IllegalAccessException ex) {
                    throw new JspTagException(ex);
                } catch (InvocationTargetException ex) {
                    throw new JspTagException(ex);
                }
                return;
            }
        }
        throw new JspTagException(Resources.getMessage("SET_INVALID_PROPERTY", property));
    }

    /**
     * Convert an object to an expected type of the method parameter according to the conversion
     * rules of the Expression Language.
     *
     * @param value the value to convert
     * @param m the setter method
     * @return value converted to an instance of the expected type; will be null if value was null
     * @throws javax.el.ELException if there was a problem coercing the value
     */
    private Object convertToExpectedType(final Object value, Method m) throws ELException {
        if (value == null) {
            return null;
        }
        Class<?> expectedType = m.getParameterTypes()[0];
        JspFactory jspFactory = JspFactory.getDefaultFactory();
        ExpressionFactory expressionFactory = jspFactory.getJspApplicationContext(pageContext.getServletContext()).getExpressionFactory();
        return expressionFactory.coerceToType(value, expectedType);
    }

    //*********************************************************************
    // Accessor methods

    /**
     * Name of the exported scoped variable to hold the value specified in the action.
     * The type of the scoped variable is whatever type the value expression evaluates to.
     *
     * @param var name of the exported scoped variable
     */
    public void setVar(String var) {
        this.var = var;
    }

    /**
     * Scope for var.
     * Values are verified by TLV.
     *
     * @param scope the variable scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }
}
