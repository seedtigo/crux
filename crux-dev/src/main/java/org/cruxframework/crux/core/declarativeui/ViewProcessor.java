/*
 * Copyright 2011 cruxframework.org.
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
package org.cruxframework.crux.core.declarativeui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cruxframework.crux.core.config.ConfigurationFactory;
import org.cruxframework.crux.core.declarativeui.template.TemplatesPreProcessor;
import org.cruxframework.crux.core.declarativeui.view.ViewLoader;
import org.cruxframework.crux.core.server.Environment;
import org.cruxframework.crux.core.utils.RegexpPatterns;
import org.cruxframework.crux.core.utils.StreamUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Process Crux view files, extracting metadata and generating the host html for
 * application pages.
 * 
 * @author Thiago da Rosa de Bustamante
 */
public class ViewProcessor
{
	private static DocumentBuilder documentBuilder = null;
	// Makes it easier to read the output files
	private static boolean forceIndent = false;

	private static final Lock lock = new ReentrantLock();
	private static final Log log = LogFactory.getLog(ViewProcessor.class);
	private static String outputCharset = "UTF-8";
	private List<CruxXmlPreProcessor> preProcessors;

	public ViewProcessor(ViewLoader viewProvider)
    {
		initDocumentBuilder();
		initPreprocessors(viewProvider);
    }

	private void initPreprocessors(ViewLoader viewProvider)
    {
	    preProcessors = new ArrayList<CruxXmlPreProcessor>();
		if (viewProvider != null)
		{
			preProcessors.add(new TemplatesPreProcessor(viewProvider.getTemplateLoader()));
		}
		String xmlPreProcessors = ConfigurationFactory.getConfigurations().cruxXmlPreProcessors();
		
		if (xmlPreProcessors != null && xmlPreProcessors.trim().length() > 0)
		{
			try
			{
				String[] processors = RegexpPatterns.REGEXP_COMMA.split(xmlPreProcessors);
				for(String processor: processors)
				{
					Class<?> processorClass = Class.forName(processor.trim());
					preProcessors.add((CruxXmlPreProcessor) processorClass.newInstance());
				}
			}
			catch (Exception e)
			{
				log.error("Error registering a custom cruxXmlPreProcessor. "
						+ "Please check your this expression on your Crux.properties file: ["
					    + xmlPreProcessors + "]. Message: " + e.getMessage(), e);
			}
		}
    }
	
	/**
	 * Extract the widgets metadata from the view page.
     *
	 * @param viewId
	 * @param viewSource
	 * @param xhmltInput
	 * @return
	 */
	public JSONObject extractWidgetsMetadata(String viewId, Document viewSource, boolean xhmltInput)
	{
		try
		{
			ViewParser viewParser = new ViewParser(viewId, true, mustIndent(), xhmltInput);
			String metadata = viewParser.extractCruxMetaData(viewSource);
			return new JSONObject(metadata);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Generate the HTML code from the view page.
	 * 
	 * @param viewId
	 * @param view
	 * @param out
	 */
	public void generateHTML(String viewId, Document view, OutputStream out)
	{
		try
		{
			StringWriter buff = new StringWriter();
			ViewParser viewParser = new ViewParser(viewId, false, mustIndent(), true);
			viewParser.generateHTMLHostPage(view, buff);
			String result = buff.toString();
			String outCharset = getOutputCharset();
			if (outCharset == null || outCharset.length() == 0)
			{
				throw new DeclarativeUITransformerException("Outputcharset is undefined. Check your web.xml file to ensure that DevModeInitializerListener is correctly configured.");
			}
			StreamUtils.write(new ByteArrayInputStream(result.getBytes(outCharset)), out, false);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return
	 */
	public String getOutputCharset()
	{
		return outputCharset;
	}	
	
	/**
	 * 
	 * @param file
	 * @param device
	 * @return
	 */
	public Document getView(InputStream file, String filename, String device)
	{
		if (file == null)
		{
			return null;
		}
		
		return loadCruxPage(file, filename, device);
	}

	/**
	 * Makes it easier to read the output files
	 * @param force
	 */
	public void setForceIndent(boolean force)
	{
		forceIndent = force;
	}	

	/**
	 * @param outputCharset
	 */
	public void setOutputCharset(String charset)
	{
		outputCharset = charset;
	}
	
	/**
	 * Loads Crux view page
	 * @param fileName
	 * @return
	 * @throws ViewParserException
	 */
	private Document loadCruxPage(InputStream file, String filename, String device)
	{
		try
		{
			Document document = documentBuilder.parse(file);
			return preprocess(document, device);
		}
		catch (Exception e)
		{
			log.error("Error parsing file: ["+filename+"] for DeviceAdaptive interface ["+device+"]: " + e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * @return
	 */
	private boolean mustIndent()
	{
		return !Environment.isProduction() || forceIndent;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 */
	private Document preprocess(Document doc, String device)
	{
		for (CruxXmlPreProcessor preProcessor : preProcessors)
		{
			doc = preProcessor.preprocess(doc, device);
		}
		
		return doc;
	}
	
	/**
	 * Initializes the static resources
	 */
	private static void initDocumentBuilder()
	{
		if (documentBuilder == null)
		{
			lock.lock();

			if (documentBuilder == null)
			{
				try
				{
					DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
					builderFactory.setNamespaceAware(true);
					builderFactory.setIgnoringComments(true);
					builderFactory.setIgnoringElementContentWhitespace(true);
					
					documentBuilder = builderFactory.newDocumentBuilder();
					documentBuilder.setEntityResolver(new EntityResolver() {
						@Override
						public InputSource resolveEntity(String publicId, String systemId)
								throws SAXException, IOException {
							if (systemId.contains("crux-view.dtd"))
							{
								return new InputSource(new ByteArrayInputStream(getValidEntities().getBytes()));
							}
							else 
							{
								return null;
							}
						}

						private String getValidEntities() {
							StringBuffer sb = new StringBuffer();
							sb.append("<!ENTITY quot    \"&#34;\">");
							sb.append("<!ENTITY amp     \"&#38;\">");
							sb.append("<!ENTITY apos    \"&#39;\">");
							sb.append("<!ENTITY lt      \"&#60;\">");
							sb.append("<!ENTITY gt      \"&#62;\">");
							sb.append("<!ENTITY nbsp    \"&#160;\">");
							return sb.toString();
						}
					});
				}
				catch (Throwable e)
				{
					log.error("Error initializing cruxToHtmlTransformer.", e);
				}
				finally
				{
					lock.unlock();
				}
			}
		}
	}
}