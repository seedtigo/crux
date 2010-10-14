/*
 * Copyright 2009 Sysmap Solutions Software e Consultoria Ltda.
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
package br.com.sysmap.crux.core.rebind.screen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.sysmap.crux.core.client.screen.DeclaredLazyWidgets;
import br.com.sysmap.crux.core.client.utils.EscapeUtils;
import br.com.sysmap.crux.core.rebind.AbstractInterfaceWrapperProxyCreator;
import br.com.sysmap.crux.core.rebind.CruxGeneratorException;
import br.com.sysmap.crux.core.rebind.scanner.module.Module;
import br.com.sysmap.crux.core.rebind.scanner.module.Modules;
import br.com.sysmap.crux.core.rebind.scanner.screen.Screen;
import br.com.sysmap.crux.core.rebind.scanner.screen.Widget;
import br.com.sysmap.crux.core.rebind.scanner.screen.config.WidgetConfig;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
public class DeclaredLazyWidgetsProxyCreator extends AbstractInterfaceWrapperProxyCreator
{
	/**
	 * Constructor
	 * @param logger
	 * @param context
	 */
	public DeclaredLazyWidgetsProxyCreator(TreeLogger logger, GeneratorContext context, JClassType invokerIntf)
    {
	    super(logger, context, invokerIntf);
    }
	
	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractInterfaceWrapperProxyCreator#getImports()
	 */
	@Override
    protected String[] getImports()
    {
		String[] imports = new String[] {
				Map.class.getCanonicalName(),
				HashMap.class.getCanonicalName(),
				DeclaredLazyWidgets.class.getCanonicalName()
		};
		return imports;       
    }

	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateProxyContructor(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateProxyContructor(SourceWriter srcWriter) throws CruxGeneratorException
	{
	}

	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateProxyFields(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateProxyFields(SourceWriter srcWriter) throws CruxGeneratorException
	{
	}

	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateProxyMethods(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateProxyMethods(SourceWriter srcWriter) throws CruxGeneratorException
	{
		srcWriter.println("public Map<String, String> getLazyWidgets(String screenId){");
		srcWriter.indent();
		srcWriter.println("Map<String, String> result = new HashMap<String, String>();");
		
		List<Screen> screens = getScreens();
		boolean first = true;
		for (Screen screen : screens)
		{
			if (!first)
			{
				srcWriter.print("else ");
			}
			first = false;
			generateGetLazyBlock(srcWriter, screen);
		}
		
		srcWriter.println("return result;");
		srcWriter.outdent();
		srcWriter.println("}");
	}

	/**
	 * @param srcWriter
	 * @param screen
	 */
	private void generateGetLazyBlock(SourceWriter srcWriter, Screen screen)
	{
		srcWriter.println("if (screenId.endsWith("+EscapeUtils.quote(getScreenId(screen))+")){");
		srcWriter.indent();

		Iterator<Widget> widgets = screen.iterateWidgets();
		while (widgets.hasNext())
		{
			Widget widget = widgets.next();
			Widget parent = widget.getParent();
			
			while (parent != null)
			{
				if (WidgetConfig.isLazyType(parent.getType()))
				{
					srcWriter.println("result.put("+EscapeUtils.quote(widget.getId())+", "+EscapeUtils.quote(parent.getId())+");");
					break;
				}
				else
				{
					parent = parent.getParent();
				}
			}
		}
		srcWriter.outdent();
		srcWriter.println("}");
	}

	/**
	 * @param screen
	 * @return
	 */
	private String getScreenId(Screen screen)
	{
		Module module = Modules.getInstance().getModule(screen.getModule());
		return Modules.getInstance().getRelativeScreenId(module, screen.getId());
	}

	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateSubTypes(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateSubTypes(SourceWriter srcWriter) throws CruxGeneratorException
	{
	}
}