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
package br.com.sysmap.crux.core.client.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.sysmap.crux.core.client.JSEngine;
import br.com.sysmap.crux.core.client.event.Event;
import br.com.sysmap.crux.core.client.event.EventClientHandlerInvoker;
import br.com.sysmap.crux.core.client.event.Events;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Abstraction for the entire page. It encapsulate all the widgets, containers and 
 * datasources.
 * @author Thiago
 *
 */
public class Screen
{
	protected String id;
	protected Map<String, Widget> widgets = new HashMap<String, Widget>(30);
	protected List<Element> blockingDivs = new ArrayList<Element>();
	protected boolean manageHistory = false;
	protected IFrameElement historyFrame = null;
	protected HandlerManager handlerManager;
	protected ModuleComunicationSerializer serializer = null;
	
	protected Screen(String id) 
	{
		this.id = id;
		this.handlerManager = new HandlerManager(this);
		this.serializer = new ModuleComunicationSerializer();
		createControllerAccessor(this);
	}
	
	protected String getIdentifier() 
	{
		return id;
	}
	
	protected boolean isManageHistory() {
		return manageHistory;
	}

	protected void setManageHistory(boolean manageHistory) {
		if (this.manageHistory != manageHistory)
		{
			this.manageHistory = manageHistory;
			Element body = RootPanel.getBodyElement();
			if (manageHistory)
			{
				if (historyFrame == null)
				{
					historyFrame = DOM.createIFrame().cast();
					historyFrame.setSrc("javascript:''");
					historyFrame.setId("__gwt_historyFrame");
					historyFrame.getStyle().setProperty("width", Integer.toString(body.getClientWidth()));
					historyFrame.getStyle().setProperty("height", Integer.toString(body.getClientHeight()));
					historyFrame.getStyle().setProperty("border", "0");
					body.appendChild(historyFrame);
				}
			}
			else
			{
				if (historyFrame != null)
				{
					body.removeChild(historyFrame);
					historyFrame = null;
				}			
			}
		}
	}

	protected Widget getWidget(String id)
	{
		return widgets.get(id);
	}
	
	/**
	 * Generic version of <code>getWidget</code> method
	 * @param <T>
	 * @param id
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Widget> T getWidget(String id, Class<T> clazz)
	{
		Widget w = widgets.get(id);
		return (T) w;
	}
	
	protected void addWidget(String id, Widget widget)
	{
		widgets.put(id, widget);
	}
	
	protected void removeWidget(String id)
	{
		removeWidget(id, true);
	}

	protected void removeWidget(String id, boolean removeFromDOM)
	{
		Widget widget = widgets.remove(id);
		if (widget != null && removeFromDOM)
		{
			widget.removeFromParent();
		}
	}

	protected Iterator<String> iteratorWidgetsIds()
	{
		return widgets.keySet().iterator();
	}
	
	protected Iterator<Widget> iteratorWidgets()
	{
		return widgets.values().iterator();
	}

	/**
	 * Creates and shows a DIV over the screen contents
	 * @param blockingDivStyleName
	 */
	protected void showBlockDiv(String blockingDivStyleName)
	{
		if(blockingDivs.size() > 0)
		{
			Element blockingDiv = blockingDivs.get(blockingDivs.size() - 1);
			blockingDiv.getStyle().setProperty("display", "none");
		}
		
		Element body = RootPanel.getBodyElement();		
		Element blockingDiv = createBlockingDiv(blockingDivStyleName, body);
		blockingDivs.add(blockingDiv);
		body.appendChild(blockingDiv);
	}

	/**
	 * Creates a DIV element to display over the screen contents
	 * @param blockingDivStyleName
	 * @param body
	 * @return
	 */
	private Element createBlockingDiv(String blockingDivStyleName, Element body)
	{
		Element blockingDiv = DOM.createDiv();
		
		int width = body.getScrollWidth();
		int height = body.getScrollHeight();
		
		if(body.getClientWidth() > width)
		{
			width = body.getClientWidth();
		}
		
		if(body.getClientHeight() > height)
		{
			height = body.getClientHeight();
		}	
		
		blockingDiv.getStyle().setProperty("position","absolute");
		blockingDiv.getStyle().setPropertyPx("top", 0);
		blockingDiv.getStyle().setPropertyPx("left", 0);
		blockingDiv.getStyle().setPropertyPx("width", width);
		blockingDiv.getStyle().setPropertyPx("height", height);
		
		if(blockingDivStyleName != null)
		{
			blockingDiv.setClassName(blockingDivStyleName);
		}
		else
		{
			blockingDiv.getStyle().setProperty("cursor", "wait");
			blockingDiv.getStyle().setProperty("backgroundColor", "white");
			blockingDiv.getStyle().setProperty("opacity", ".01");
			blockingDiv.getStyle().setProperty("filter", "alpha(opacity=1)");

			body.getStyle().setProperty("cursor", "wait");
		}
		
		return blockingDiv;
	}
	
	/**
	 * Hides the DIV that is blocking the Screen contents
	 */
	protected void hideBlockDiv()
	{
		if(blockingDivs.size() > 0)
		{
			int last = blockingDivs.size() - 1;
			
			Element blockingDiv = blockingDivs.get(last);
			blockingDivs.remove(last);
			
			Element body = RootPanel.getBodyElement();
			body.removeChild(blockingDiv);
			body.getStyle().setProperty("cursor", "");
		}
		
		if(blockingDivs.size() > 0)
		{
			Element blockingDiv = blockingDivs.get(blockingDivs.size() - 1);
			blockingDiv.getStyle().setProperty("display", "block");
		}
	}
	
	/**
	 * 
	 * @param element
	 */
	protected void parse(Element element) 
	{
		String manageHistoryStr = element.getAttribute("_manageHistory");
		if (manageHistoryStr != null)
		{
			setManageHistory("true".equals(manageHistoryStr));
		}
		String title = element.getAttribute("_title");
		if (title != null && title.length() >0)
		{
			Window.setTitle(ScreenFactory.getInstance().getDeclaredMessage(title));
		}
		final Event eventClosing = Events.getEvent(Events.EVENT_CLOSING, element.getAttribute(Events.EVENT_CLOSING));
		if (eventClosing != null)
		{
			Window.addWindowClosingHandler(new Window.ClosingHandler(){
				public void onWindowClosing(ClosingEvent closingEvent) 
				{
					Events.callEvent(eventClosing, closingEvent);
				}
			});
		}

		final Event eventClose = Events.getEvent(Events.EVENT_CLOSE, element.getAttribute(Events.EVENT_CLOSE));
		if (eventClose != null)
		{
			Window.addCloseHandler(new CloseHandler<Window>(){
				public void onClose(CloseEvent<Window> event) 
				{
					Events.callEvent(eventClose, event);				
				}
			});
		}

		final Event eventResized = Events.getEvent(Events.EVENT_RESIZED, element.getAttribute(Events.EVENT_RESIZED));
		if (eventResized != null)
		{
			Window.addResizeHandler(new ResizeHandler(){
				public void onResize(ResizeEvent event) 
				{
					Events.callEvent(eventResized, event);
				}
			});
		}
		final Event eventLoad = Events.getEvent(Events.EVENT_LOAD, element.getAttribute(Events.EVENT_LOAD));
		if (eventLoad != null)
		{
			addLoadHandler(new ScreenLoadHandler(){
				public void onLoad(ScreenLoadEvent screenLoadEvent) 
				{
					Events.callEvent(eventLoad, screenLoadEvent);
				}
			});
		}
	}

	/**
	 * Adds an event handler that is called only once, when the screen is loaded
	 * @param handler
	 */
	protected void addLoadHandler(final ScreenLoadHandler handler) 
	{
		handlerManager.addHandler(ScreenLoadEvent.TYPE, handler);
	}

	/**
	 * Fires the load event. This method has no effect when called more than one time.
	 */
	protected void load() 
	{
		if (handlerManager.getHandlerCount(ScreenLoadEvent.TYPE) > 0)
		{
			new Timer()
			{
				public void run() 
				{
					try 
					{
						DeferredCommand.addCommand(new Command() {
							public void execute() 
							{
								ScreenLoadEvent.fire(Screen.this);
							}
						});							
					} 
					catch (RuntimeException e) 
					{
						GWT.log(e.getLocalizedMessage(), e);
					}
				}
			}.schedule(1); // Waits for browser starts the rendering process
		}
	}

	/**
	 * Fires the load event. This method has no effect when called more than one time.
	 */
	protected void fireEvent(ScreenLoadEvent event) 
	{
		handlerManager.fireEvent(event);
	}
	
	/**
	 * Update widgets on screen that have the same id of fields mapped with ValueObject
	 * @param eventHandler
	 */
	protected void updateScreenWidgets(Object eventHandler)
	{
		if (eventHandler != null)
		{
			if (!(eventHandler instanceof EventClientHandlerInvoker))
			{
				throw new ClassCastException(JSEngine.messages.screenInvalidHandlerError());
			}

			((EventClientHandlerInvoker) eventHandler).updateScreenWidgets();
		}
	}
	
	/**
	 * Update fields mapped with ValueObject from widgets that have similar names.
	 * @param eventHandler
	 */
	protected void updateControllerObjects(Object eventHandler)
	{
		if (eventHandler != null)
		{
			if (!(eventHandler instanceof EventClientHandlerInvoker))
			{
				throw new ClassCastException(JSEngine.messages.screenInvalidHandlerError());
			}
			((EventClientHandlerInvoker) eventHandler).updateControllerObjects();

		}
	}

	private native void createControllerAccessor(Screen handler)/*-{
		$wnd._cruxScreenControllerAccessor = function(call, serializedData){
			return handler.@br.com.sysmap.crux.core.client.component.Screen::invokeController(Ljava/lang/String;Ljava/lang/String;)(call, serializedData);
		};
	}-*/;

	@SuppressWarnings("unused") // called by native code
	private String invokeController(String call, String serializedData)
	{
		Event event = Events.getEvent("_onInvokeController", call);
		InvokeControllerEvent controllerEvent = new InvokeControllerEvent();
		
		if (serializedData != null)
		{
			try
			{
				controllerEvent.setData(serializer.deserialize(serializedData));
				Object result = Events.callEvent(event, controllerEvent, true);
				return serializer.serialize(result); 
			}
			catch (ModuleComunicationException e)
			{
				GWT.log(e.getLocalizedMessage(), e);
				Window.alert(e.getLocalizedMessage());
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the current screen
	 * @return
	 */
	public static Screen get()
	{
		return ScreenFactory.getInstance().getScreen();
	}
	
	/**
	 * Gets a widget on the current screen
	 * @param id
	 * @return
	 */
	public static Widget get(String id)
	{
		return Screen.get().getWidget(id);
	}
	
	/**
	 * 
	 * @param <T>
	 * @param id
	 * @param clazz
	 * @return
	 */
	public static <T extends Widget> T get(String id, Class<T> clazz)
	{
		return Screen.get().getWidget(id, clazz);
	}
	
	/**
	 * 
	 * @param id
	 * @param widget
	 */
	public static void add(String id, Widget widget)
	{
		Screen.get().addWidget(id, widget);
	}

	/**
	 * 
	 * @return
	 */
	public static Iterator<String> iterateWidgetsIds()
	{
		return Screen.get().iteratorWidgetsIds();
	}
	
	/**
	 * 
	 * @return
	 */
	public static Iterator<Widget> iterateWidgets()
	{
		return Screen.get().iteratorWidgets();
	}

	/**
	 * Remove a widget on the current screen
	 * @param id
	 */
	public static void remove(String id)
	{
		Screen.get().removeWidget(id);
	}
	
	/**
	 * Remove a widget on the current screen
	 * @param id
	 * @param removeFromDOM
	 */
	public static void remove(String id, boolean removeFromDOM)
	{
		Screen.get().removeWidget(id, removeFromDOM);
	}
	
	/**
	 * Update widgets on screen that have the same id of fields mapped with ValueObject
	 * 
	 * @param eventHandler
	 */
	public static void updateScreen(Object eventHandler)
	{
		Screen.get().updateScreenWidgets(eventHandler);
	}
	
	/**
	 * Update fields mapped with ValueObject from widgets that have similar names.
	 * 
	 * @param eventHandler
	 */
	public void updateController(Object eventHandler)
	{
		Screen.get().updateControllerObjects(eventHandler);
	}

	/**
	 * 
	 */
	public static void blockToUser()
	{
		Screen.get().showBlockDiv(null);
	}
	
	/**
	 * 
	 */
	public static void blockToUser(String blockingDivStyleName)
	{
		Screen.get().showBlockDiv(blockingDivStyleName);
	}
	
	/**
	 * 
	 */
	public static void unblockToUser()
	{
		Screen.get().hideBlockDiv();
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getId() 
	{
		return Screen.get().getIdentifier();
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean isHistoryManaged() 
	{
		return Screen.get().isManageHistory();
	}

	/**
	 * 
	 * @param manageHistory
	 */
	public static void setHistoryManaged(boolean manageHistory) 
	{
		Screen.get().setManageHistory(manageHistory);
	}
	
	public static ModuleComunicationSerializer getModuleShareableSerializer()
	{
		return Screen.get().serializer;
	}
	
	/**
	 * 
	 * @param call
	 * @throws ModuleComunicationException
	 */
	public static void invokeControllerOnTop(String call) throws ModuleComunicationException
	{
		invokeControllerOnTop(call, null, Object.class);
	}

	/**
	 * @param call
	 * @param param
	 * @throws ModuleComunicationException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeControllerOnTop(String call, Object param, Class<T> resultType) throws ModuleComunicationException
	{
		return (T) Screen.get().serializer.deserialize(callTopControllerAccessor(call, Screen.get().serializer.serialize(param)));
	}

	/**
	 * 
	 * @param call
	 * @throws ModuleComunicationException
	 */
	public static void invokeControllerOnOpener(String call) throws ModuleComunicationException
	{
		invokeControllerOnOpener(call, null, Object.class);
	}

	/**
	 * 
	 * @param call
	 * @param param
	 * @throws ModuleComunicationException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T  invokeControllerOnOpener(String call, Object param, Class<T> resultType) throws ModuleComunicationException
	{
		return (T) Screen.get().serializer.deserialize(callOpenerControllerAccessor(call, Screen.get().serializer.serialize(param)));
	}

	/**
	 * @param call
	 * @throws ModuleComunicationException
	 */
	public static void invokeControllerOnParent(String call) throws ModuleComunicationException
	{
		invokeControllerOnParent(call, null, Object.class);
	}

	/**
	 * @param call
	 * @param param
	 * @throws ModuleComunicationException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T  invokeControllerOnParent(String call, Object param, Class<T> resultType) throws ModuleComunicationException
	{
		return (T) Screen.get().serializer.deserialize(callParentControllerAccessor(call, Screen.get().serializer.serialize(param)));
	}
	
	/**
	 * @param call
	 * @throws ModuleComunicationException
	 */
	public static void invokeControllerOnSelf(String call)
	{
		invokeControllerOnSelf(call, null, Object.class);
	}

	/**
	 * @param call
	 * @param param
	 * @throws ModuleComunicationException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeControllerOnSelf(String call, Object param, Class<T> resultType)
	{
		try
		{
			Event event = Events.getEvent("_onInvokeController", call);
			InvokeControllerEvent controllerEvent = new InvokeControllerEvent();
			controllerEvent.setData(param);
			Object result = Events.callEvent(event, controllerEvent, false);
			return (T) result; 
		}
		catch (RuntimeException e)
		{
			GWT.log(e.getLocalizedMessage(), e);
			Window.alert(e.getLocalizedMessage());
			return null;
		}
	}
	
	/**
	 * @param call
	 * @param serializedData
	 * @return
	 */
	private static native String callTopControllerAccessor(String call, String serializedData)/*-{
		return $wnd.top._cruxScreenControllerAccessor(call, serializedData);
	}-*/;
	
	/**
	 * @param call
	 * @param serializedData
	 * @return
	 */
	private static native String callOpenerControllerAccessor(String call, String serializedData)/*-{
		return $wnd.opener._cruxScreenControllerAccessor(call, serializedData);
	}-*/;
	
	/**
	 * @param call
	 * @param serializedData
	 * @return
	 */
	private static native String callParentControllerAccessor(String call, String serializedData)/*-{
		return $wnd.parent._cruxScreenControllerAccessor(call, serializedData);
	}-*/;
}
