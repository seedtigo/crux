/*
 * Copyright 2014 cruxframework.org.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.cruxframework.crux.core.rebind.rest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.cruxframework.crux.core.client.utils.EscapeUtils;
import org.cruxframework.crux.core.rebind.AbstractProxyCreator.SourcePrinter;
import org.cruxframework.crux.core.rebind.context.RebindContext;
import org.cruxframework.crux.core.utils.JClassUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameter;

public class JsonPRestCreatorHelper
{
	protected RebindContext context;
	private JClassType javascriptObjectType;

	public JsonPRestCreatorHelper(RebindContext context)
	{
		this.context = context;
		javascriptObjectType = context.getGeneratorContext().getTypeOracle().findType(JavaScriptObject.class.getCanonicalName());
		
	}
	
	protected void generateJSONPInvocation(RestMethodInfo methodInfo, SourcePrinter srcWriter, JParameter callbackParameter, 
			String callbackResultTypeName, String callbackParameterName, String restURIParam, String jsonPCallbackParam, String jsonPFailureCallbackParam)
    {
		srcWriter.println("try{");
	    srcWriter.println("JsonpRequestBuilder builder = new JsonpRequestBuilder();");
	    if (jsonPCallbackParam.length() > 0)
		{
	    	srcWriter.println("builder.setCallbackParam("+EscapeUtils.quote(jsonPCallbackParam)+");");
		}
	    
	    if (jsonPFailureCallbackParam.length() > 0)
		{
	    	srcWriter.println("builder.setFailureCallbackParam("+EscapeUtils.quote(jsonPFailureCallbackParam)+");");
		}
	    
	    JClassType callbackResultType = JClassUtils.getTypeArgForGenericType(callbackParameter.getType().isClassOrInterface());

	    if (callbackResultType.isEnum() != null || callbackResultType.getQualifiedSourceName().equals(String.class.getCanonicalName())
	    	|| callbackResultType.getQualifiedSourceName().equals(BigDecimal.class.getCanonicalName())	
	    	|| callbackResultType.getQualifiedSourceName().equals(BigInteger.class.getCanonicalName()))	
	    {
	    	generateRequestAndOnSuccessMethodForString(srcWriter, callbackResultTypeName, callbackParameterName, restURIParam, callbackResultType);
	    }
	    else if (callbackResultType.getQualifiedSourceName().equals(Boolean.class.getCanonicalName()))
	    {
	    	generateRequestAndOnSuccessMethodForBoolean(srcWriter, callbackResultTypeName, callbackParameterName, restURIParam, callbackResultType);
	    }
	    else if (callbackResultType.getQualifiedSourceName().equals(Integer.class.getCanonicalName()) 
	    		|| callbackResultType.getQualifiedSourceName().equals(Short.class.getCanonicalName())
	    		|| callbackResultType.getQualifiedSourceName().equals(Byte.class.getCanonicalName()))
	    {
	    	generateRequestAndOnSuccessMethodForInteger(srcWriter, callbackResultTypeName, callbackParameterName, restURIParam, callbackResultType);
	    }
	    else if (callbackResultType.getQualifiedSourceName().equals(Double.class.getCanonicalName()) 
	    		|| callbackResultType.getQualifiedSourceName().equals(Long.class.getCanonicalName())
	    		|| callbackResultType.getQualifiedSourceName().equals(Date.class.getCanonicalName()))
	    {
	    	generateRequestAndOnSuccessMethodForDouble(srcWriter, callbackResultTypeName, callbackParameterName, restURIParam, callbackResultType);
	    }
	    else
	    {
	    	generateRequestAndOnSuccessMethodForObject(srcWriter, callbackResultTypeName, callbackParameterName, restURIParam, callbackResultType);
	    }
		srcWriter.println("public void onFailure(Throwable exception){");
		srcWriter.println(callbackParameterName+".onError(new RestError(-1, Crux.getMessages().restServiceUnexpectedError(exception.getMessage())));");
		srcWriter.println("}");
		srcWriter.println("});");

		srcWriter.println("}catch (Exception e){");
		srcWriter.println(callbackParameterName+".onError(new RestError(-1, Crux.getMessages().restServiceUnexpectedError(e.getMessage())));");
		srcWriter.println("}");
    }

	protected void generateRequestAndOnSuccessMethodForString(SourcePrinter srcWriter, String callbackResultTypeName, String callbackParameterName, String restURIParam, JClassType callbackResultType)
    {
	    srcWriter.println("builder.requestString("+restURIParam+", new AsyncCallback<String>(){");

	    srcWriter.println("public void onSuccess(String jsonText){");
		srcWriter.println("try{");

		srcWriter.println("JSONValue jsonValue = new JSONString(jsonText);");
		String serializerName = new JSonSerializerProxyCreator(context, callbackResultType).create();
		srcWriter.println(callbackResultTypeName+" result = new "+serializerName+"().decode(jsonValue);");
		
		srcWriter.println(callbackParameterName+".onSuccess(result);");
		srcWriter.println("}catch (Exception e){");
		srcWriter.println("if (LogConfiguration.loggingIsEnabled()){");
		srcWriter.println("__log.log(Level.SEVERE, e.getMessage(), e);");
		srcWriter.println("}");
		srcWriter.println("}");
		srcWriter.println("}");
    }

	protected void generateRequestAndOnSuccessMethodForBoolean(SourcePrinter srcWriter, String callbackResultTypeName, String callbackParameterName, String restURIParam, JClassType callbackResultType)
    {
	    srcWriter.println("builder.requestBoolean("+restURIParam+", new AsyncCallback<Boolean>(){");

	    srcWriter.println("public void onSuccess(Boolean result){");
		srcWriter.println("try{");
		srcWriter.println(callbackParameterName+".onSuccess(result);");
		srcWriter.println("}catch (Exception e){");
		srcWriter.println("if (LogConfiguration.loggingIsEnabled()){");
		srcWriter.println("__log.log(Level.SEVERE, e.getMessage(), e);");
		srcWriter.println("}");
		srcWriter.println("}");
		srcWriter.println("}");
    }

	protected void generateRequestAndOnSuccessMethodForDouble(SourcePrinter srcWriter, String callbackResultTypeName, String callbackParameterName, String restURIParam, JClassType callbackResultType)
    {
	    srcWriter.println("builder.requestDouble("+restURIParam+", new AsyncCallback<Double>(){");

	    srcWriter.println("public void onSuccess(Double value){");
		srcWriter.println("try{");

		srcWriter.println("JSONValue jsonValue = new JSONNumber(value);");
		String serializerName = new JSonSerializerProxyCreator(context, callbackResultType).create();
		srcWriter.println(callbackResultTypeName+" result = new "+serializerName+"().decode(jsonValue);");
		srcWriter.println(callbackParameterName+".onSuccess(result);");
		srcWriter.println("}catch (Exception e){");
		srcWriter.println("if (LogConfiguration.loggingIsEnabled()){");
		srcWriter.println("__log.log(Level.SEVERE, e.getMessage(), e);");
		srcWriter.println("}");
		srcWriter.println("}");
		srcWriter.println("}");
    }

	protected void generateRequestAndOnSuccessMethodForInteger(SourcePrinter srcWriter, String callbackResultTypeName, String callbackParameterName, String restURIParam, JClassType callbackResultType)
    {
	    srcWriter.println("builder.requestInteger("+restURIParam+", new AsyncCallback<Integer>(){");

	    srcWriter.println("public void onSuccess(Integer value){");
		srcWriter.println("try{");
		srcWriter.println("JSONValue jsonValue = new JSONNumber(value);");
		String serializerName = new JSonSerializerProxyCreator(context, callbackResultType).create();
		srcWriter.println(callbackResultTypeName+" result = new "+serializerName+"().decode(jsonValue);");
		srcWriter.println(callbackParameterName+".onSuccess(result);");
		srcWriter.println("}catch (Exception e){");
		srcWriter.println("if (LogConfiguration.loggingIsEnabled()){");
		srcWriter.println("__log.log(Level.SEVERE, e.getMessage(), e);");
		srcWriter.println("}");
		srcWriter.println("}");
		srcWriter.println("}");
    }

	protected void generateRequestAndOnSuccessMethodForObject(SourcePrinter srcWriter, String callbackResultTypeName, String callbackParameterName, String restURIParam, JClassType callbackResultType)
    {
	    srcWriter.println("builder.requestObject("+restURIParam+", new AsyncCallback<"+JavaScriptObject.class.getCanonicalName()+">(){");

	    srcWriter.println("public void onSuccess("+JavaScriptObject.class.getCanonicalName()+" obj){");
		srcWriter.println("try{");

		if (callbackResultType != null && callbackResultType.isAssignableTo(javascriptObjectType))
		{
			srcWriter.println(callbackResultTypeName+" result = obj!=null?obj.cast():null;");
		}
		else
		{
			srcWriter.println("JSONValue jsonValue = new JSONObject(obj);");
			String serializerName = new JSonSerializerProxyCreator(context, callbackResultType).create();
			srcWriter.println(callbackResultTypeName+" result = new "+serializerName+"().decode(jsonValue);");
		}
		srcWriter.println(callbackParameterName+".onSuccess(result);");
		srcWriter.println("}catch (Exception e){");
		srcWriter.println("if (LogConfiguration.loggingIsEnabled()){");
		srcWriter.println("__log.log(Level.SEVERE, e.getMessage(), e);");
		srcWriter.println("}");
		srcWriter.println("}");
		srcWriter.println("}");
    }
}
