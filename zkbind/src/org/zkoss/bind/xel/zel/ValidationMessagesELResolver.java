/* ValidationMessagesELResolver.java

	Purpose:
		
	Description:
		
	History:
		Mary 29, 2012 3:18:34 PM, Created by dennis

Copyright (C) 2012 Potix Corporation. All Rights Reserved.
*/

package org.zkoss.bind.xel.zel;

import java.beans.FeatureDescriptor;
import java.io.Serializable;
import java.util.Iterator;

import org.zkoss.bind.sys.ValidationMessages;
import org.zkoss.zel.ELContext;
import org.zkoss.zel.ELException;
import org.zkoss.zel.ELResolver;
import org.zkoss.zel.PropertyNotFoundException;
import org.zkoss.zel.PropertyNotWritableException;
import org.zkoss.zk.ui.Component;

/**
 * ELResolver for {@link ValidationMessages}.
 * @author dennis
 * @since 6.0.1
 */
public class ValidationMessagesELResolver extends ELResolver {
    @Override
    public Object getValue(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }
        if(base==null) return null;
        
        if (base instanceof ValidationMessages) {
        	final int numOfKids = ((Integer) context.getContext(Integer.class)).intValue(); //get numOfKids, see #PathResolver
        	
        	final ValidationMessages vms = (ValidationMessages) base;
        	String[] msgs = null;
        	
        	if(property instanceof Component){
            	context.setPropertyResolved(true);
        		if(numOfKids==0){//case vmsgs[tb1]
        			msgs = vms.getMessages((Component)property);
        			if(msgs==null || msgs.length==0) return null;
        			return msgs[0];
        		}else{//case vmsgs[tb1][*]
        			return new ComponentResolver((ValidationMessages) base,(Component)property);
        		}
        	}else if(property instanceof String){
            	context.setPropertyResolved(true);
        		if("texts".equals(property)){
            		if(numOfKids==0){//case vmsgs.texts
                		return vms.getMessages();
                	}else {//case vmsgs.texts[*]
                		return new TextsResolver((ValidationMessages) base); 
                	}
            	}else{//case vmsgs['key']
            		msgs = vms.getKeyMessages((String)property);
            		if(msgs==null || msgs.length==0) return null;
            		return msgs[0];
            	}
        	}
        }else if(base instanceof ValueResolver){
        	final int numOfKids = ((Integer) context.getContext(Integer.class)).intValue(); //get numOfKids, see #PathResolver
        	return ((ValueResolver)base).getValue(context,property, numOfKids);
        }

        return null;
    }
    
    

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
    	//get type is called by setValue,see AstValue#setValue, 
    	//since this is ready only resolver, we don't need to implement it.
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property,
            Object value) throws NullPointerException,
            PropertyNotFoundException, PropertyNotWritableException,
            ELException {
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
    	return true;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }
    
    
    private static interface ValueResolver {
    	public Object getValue(ELContext context,Object property,int numOfKids);
    }
    
    private static abstract class AbstractValueResolver implements Serializable,ValueResolver{
		private static final long serialVersionUID = 1L;
		final protected ValidationMessages _vms;
    	AbstractValueResolver(ValidationMessages vms){
    		this._vms = vms;
    	}
    }
    
    //vmsgs[tb1]*
    private static class ComponentResolver extends AbstractValueResolver{
    	private static final long serialVersionUID = 1L;
    	final protected Component _comp;
    	ComponentResolver(ValidationMessages vms,Component comp) {
			super(vms);
			this._comp = comp;
		}
		public Object getValue(ELContext context,Object property,int numOfKids){
			String[] msgs = null;
			if(property instanceof String){//vmsgs[tb1]['attr']
	        	context.setPropertyResolved(true);
				msgs = _vms.getMessages(_comp,(String)property);
        		if(msgs!=null && msgs.length>0){
        			return msgs[0];
        		}
        		return null;
			}else{
				throw new PropertyNotFoundException("uknow property "+property+" for validation-messages[comp][*]");
			}
    	}
    }
    
    
    //vmsgs.texts.*
    private static class TextsResolver extends AbstractValueResolver{
		private static final long serialVersionUID = 1L;

		TextsResolver(ValidationMessages vms) {
			super(vms);
		}

		public Object getValue(ELContext context,Object property, int numOfKids) {
			String[] msgs = null;
			if(property instanceof Component){
				context.setPropertyResolved(true);
        		if(numOfKids==0){//case vmsgs.texts[tb1]
        			return _vms.getMessages((Component)property);
        		}else{//case vmsgs.texts[tb1]*
        			return new TextsComponentResolver(_vms,(Component)property);
        		}
        	}else if(property instanceof String){//case vmsgs.texts['key']
        		context.setPropertyResolved(true);
        		return _vms.getKeyMessages((String)property);
        	}else if(property instanceof Number){//case vmsgs.texts[index]
        		context.setPropertyResolved(true);
        		msgs = _vms.getMessages();
        		return msgs==null?null:msgs[((Number)property).intValue()];
        	}
			return null;
		}
    }
    
    //vmsgs.texts[tb1]*
    private static class TextsComponentResolver extends AbstractValueResolver{
    	private static final long serialVersionUID = 1L;
    	final protected Component _comp;
    	TextsComponentResolver(ValidationMessages vms,Component comp) {
			super(vms);
			this._comp = comp;
		}

		public Object getValue(ELContext context,Object property,int numOfKids){
			String[] msgs = null;
			if(property instanceof String){//vmsgs.texts[tb1]['attr']
				context.setPropertyResolved(true);
				return _vms.getMessages(_comp,(String)property);
			}else if(property instanceof Number){//vmsgs.texts[tb1][index]
				context.setPropertyResolved(true);
				msgs = _vms.getMessages(_comp);
        		return msgs==null?null:msgs[((Number)property).intValue()];
			}
			return null;
    	}
    }
}
