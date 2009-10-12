/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.intro.contentproviders;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Content provider which allows a news reader to be included in intro
 * @since 3.4
 */

public class EclipseRSSViewer implements IIntroContentProvider {

	private final int SOCKET_TIMEOUT = 6000; //milliseconds

	private static final String INTRO_SHOW_IN_BROWSER = "http://org.eclipse.ui.intro/openBrowser?url="; //$NON-NLS-1$

	private static final String HREF_BULLET = "bullet"; //$NON-NLS-1$

	private HashMap params;

	private IIntroContentProviderSite site;

	private boolean disposed;

	private String id;

	private List items;

	private Composite parent;

	private FormToolkit toolkit;

	private FormText formText;

	private Image bulletImage;

	private boolean threadRunning = false;

	/**
	 * Initialize the content provider
	 * @param site an object which allows rcontainer reflows to be requested
	 */ 
	public void init(IIntroContentProviderSite site) {
		this.site = site;
		refresh();
	}

	/**
	 * Create the html content for this newsreader
	 * @param id
	 * @param out a writer where the html will be written
	 */
	public void createContent(String id, PrintWriter out) {	
		if (disposed)
			return;
		this.id = id;
		params = setParams(id);

		
		if (items==null)
			createNewsItems();
		
		if (items == null || threadRunning) {
			out.print("<p class=\"status-text\">"); //$NON-NLS-1$
			out.print(Messages.RSS_Loading);
			out.println("</p>"); //$NON-NLS-1$
		} else {
			if (items.size() > 0) {
				out.println("<ul id=\"news-feed\" class=\"news-list\">"); //$NON-NLS-1$
				for (int i = 0; i < items.size(); i++) {
					NewsItem item = (NewsItem) items.get(i);
					out.print("<li>"); //$NON-NLS-1$
					out.print("<a class=\"topicList\" href=\""); //$NON-NLS-1$
					out.print(createExternalURL(item.url));

					out.print("\">"); //$NON-NLS-1$
					out.print(item.label);
					out.print("</a>"); //$NON-NLS-1$
					out.print("</li>\n"); //$NON-NLS-1$
				}
				out.println("</ul>"); //$NON-NLS-1$
			} else {
				out.print("<p class=\"status-text\">"); //$NON-NLS-1$
				out.print(Messages.RSS_No_news_please_visit);
				out.print(" <a href=\""); //$NON-NLS-1$
				out.print(createExternalURL(getParameter("no_news_url"))); //$NON-NLS-1$
				out.print("\">"); //$NON-NLS-1$
				out.print(getParameter("no_news_text")); //$NON-NLS-1$
				out.print("</a>"); //$NON-NLS-1$
				out.println("</p>"); //$NON-NLS-1$
			}
			URL url = null;
			try {
				url = new URL(getParameter("url")); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				IntroPlugin.logError("Bad URL: "+url, e); //$NON-NLS-1$
			}
			if (url != null) {
				out.println("<p><span class=\"rss-feed-link\">"); //$NON-NLS-1$
				out.println("<a href=\""); //$NON-NLS-1$
				out.println(createExternalURL(url.toString()));
				out.println("\">"); //$NON-NLS-1$
				out.println(Messages.RSS_Subscribe);
				out.println("</a>"); //$NON-NLS-1$
				out.println("</span></p>"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Create widgets to display the newsreader when using the SWT presentation
	 */
	public void createContent(String id, Composite parent, FormToolkit toolkit) {
		if (disposed)
			return;
		this.id = id;
		params = setParams(id);
		
		if (formText == null) {
			// a one-time pass
			formText = toolkit.createFormText(parent, true);
			formText.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					doNavigate((String) e.getHref());
				}
			});
			bulletImage = createImage(new Path("icons/arrow.gif")); //$NON-NLS-1$
			if (bulletImage != null)
				formText.setImage(HREF_BULLET, bulletImage);
			this.parent = parent;
			this.toolkit = toolkit;
			this.id = id;
			params = setParams(id);

		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>"); //$NON-NLS-1$
		
		
		if (items==null)
			createNewsItems();
		
		if (items == null || threadRunning) {
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append(Messages.RSS_Loading);
			buffer.append("</p>"); //$NON-NLS-1$
		} else {
			if (items.size() > 0) {
				for (int i = 0; i < items.size(); i++) {
					NewsItem item = (NewsItem) items.get(i);
					buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					buffer.append(HREF_BULLET);
					buffer.append("\">"); //$NON-NLS-1$
					buffer.append("<a href=\""); //$NON-NLS-1$
					buffer.append(createExternalURL(item.url));
					buffer.append("\">"); //$NON-NLS-1$
					buffer.append(item.label);
					buffer.append("</a>"); //$NON-NLS-1$
					buffer.append("</li>"); //$NON-NLS-1$

				}
			} else {

				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(Messages.RSS_No_news);
				buffer.append("</p>"); //$NON-NLS-1$
			}
		}

		buffer.append("</form>"); //$NON-NLS-1$
		
		String text = buffer.toString();
		text = text.replaceAll("&{1}", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
		formText.setText(text, true, false);
	}

	
	private String createExternalURL(String url) {
		try {
			return INTRO_SHOW_IN_BROWSER + URLEncoder.encode(url, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return INTRO_SHOW_IN_BROWSER + url;
		} 
	}

	public void dispose() {
		if (bulletImage != null) {
			bulletImage.dispose();
			bulletImage = null;
		}
		disposed = true;
	}

	/**
	 * Method is responsible for gathering RSS data.
	 * 
	 * Kicks off 2 threads:
	 * 
	 * 	The first (ContentThread) is to actually query the feeds URL to find RSS entries.
	 *  When it finishes, it calls a refresh to display the entires it found (if any).
	 * 
	 *  [Esc RATLC00319786]
	 *  The second (TimeoutThread) waits for SOCKET_TIMEOUT ms to see if the content thread
	 *  has finished reading RSS.  If it has finished, nothing further happens.  If it has
	 *  not finished, the TimeoutThread sets the threadRunning boolean to false and refreshes
	 *  the page (basically telling the UI that no content could be found, and removes
	 *  the 'Loading...' text).
	 * 
	 */
	private void createNewsItems() {
		
		ContentThread contentThread = new ContentThread();
		contentThread.start();
		TimeoutThread timeThread = new TimeoutThread();
		timeThread.start();
	}
	
	/**
	 * Reflows the page using an UI thread.
	 */
	private void refresh()
	{
		Thread newsWorker = new Thread(new NewsFeed());
		newsWorker.start();
	}

	private Image createImage(IPath path) {
		Bundle bundle = Platform.getBundle(IntroPlugin.PLUGIN_ID); 
		URL url = FileLocator.find(bundle, path, null);
		try {
			url = FileLocator.toFileURL(url);
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			return desc.createImage();
		} catch (IOException e) {
			return null;
		}
	}

	private void doNavigate(final String url) {
		BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(),
				new Runnable() {
					public void run() {
						IIntroURL introUrl = IntroURLFactory
								.createIntroURL(url);
						if (introUrl != null) {
							// execute the action embedded in the IntroURL
							introUrl.execute();
							return;
						}
						// delegate to the browser support
						openBrowser(url);
					}
				});
	}

	private void openBrowser(String href) {
		try {
			URL url = new URL(href);
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench()
					.getBrowserSupport();
			support.getExternalBrowser().openURL(url);
		} catch (PartInitException e) {
		} catch (MalformedURLException e) {
		}
	}

	static class NewsItem {
		String label;

		String url;

		void setLabel(String label) {
			this.label = label;
		}

		void setUrl(String url) {
			this.url = url;
		}
	}

	class NewsFeed implements Runnable {
		public void run() {
			// important: don't do the work if the
			// part gets disposed in the process
			if (disposed)
				return;

			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					if (parent != null) {
						// we must recreate the content
						// for SWT because we will use
						// a gentle incremental reflow.
						// HTML reflow will simply reload the page.
						createContent(id, parent, toolkit);
//						reflow(formText);
					}
					site.reflow(EclipseRSSViewer.this, true);
				}
			});
		}
	}

	/**
	 * Handles RSS XML and populates the items list with at most
	 * MAX_NEWS_ITEMS items.
	 */
	private class RSSHandler extends DefaultHandler {

		private static final String ELEMENT_RSS = "rss"; //$NON-NLS-1$
		private static final String ELEMENT_CHANNEL = "channel"; //$NON-NLS-1$
		private static final String ELEMENT_ITEM = "item"; //$NON-NLS-1$
		private static final String ELEMENT_TITLE = "title"; //$NON-NLS-1$
		private static final String ELEMENT_LINK = "link"; //$NON-NLS-1$

		private Stack stack = new Stack();
		private StringBuffer buf;
		private NewsItem item;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			stack.push(qName);
			// it's a title/link in an item
			if ((ELEMENT_TITLE.equals(qName) || ELEMENT_LINK.equals(qName))
					&& (item != null)) {
				// prepare the buffer; we're expecting chars
				buf = new StringBuffer();
			}
			// it's an item in a channel in rss
			else if (ELEMENT_ITEM.equals(qName)
					&& (ELEMENT_CHANNEL.equals(stack.get(1)))
					&& (ELEMENT_RSS.equals(stack.get(0)))
					&& (stack.size() == 3)
					&& (items.size() < Integer
							.parseInt(getParameter("welcome_items")))) { //$NON-NLS-1$

				// prepare the item
				item = new NewsItem();
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			stack.pop();
			if (item != null) {
				if (buf != null) {
					if (ELEMENT_TITLE.equals(qName)) {
						item.setLabel(buf.toString().trim());
						buf = null;
					} else if (ELEMENT_LINK.equals(qName)) {
						item.setUrl(buf.toString().trim());
						buf = null;
					}
				} else {
					if (ELEMENT_ITEM.equals(qName)) {
						// ensure we have a valid item
						if (item.label != null && item.label.length() > 0
								&& item.url != null && item.url.length() > 0) {
							items.add(item);
						}
						item = null;
					}
				}
			}
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			// were we expecting chars?
			if (buf != null) {
				buf.append(new String(ch, start, length));
			}
		}
	}

	private HashMap setParams(String query) {
		HashMap _params = new HashMap();
		//String[] t = _query.split("?");
		//String query = t[1];
		if (query != null && query.length() > 1) {
			//String qs = query.substring(1);
			String[] kvPairs = query.split("##"); //$NON-NLS-1$
			for (int i = 0; i < kvPairs.length; i++) {
				String[] kv = kvPairs[i].split("=", 2); //$NON-NLS-1$
				if (kv.length > 1) {
					_params.put(kv[0], kv[1]);
				} else {
					_params.put(kv[0], ""); //$NON-NLS-1$
				}
			}
		}
		return _params;
	}

	private String getParameter(String name) {
		return (String) params.get(name);
	}
	
	private class ContentThread extends Thread{

		public void run()
		{
			threadRunning = true;
			items = Collections.synchronizedList(new ArrayList());

			InputStream in = null;

			try {
				IntroPlugin.logDebug("Open Connection: "+getParameter("url")); //$NON-NLS-1$ //$NON-NLS-2$
				URL url = new URL(getParameter("url")); //$NON-NLS-1$
				URLConnection conn = url.openConnection();

				// set connection timeout to 6 seconds
				setTimeout(conn, SOCKET_TIMEOUT); // Connection timeout to 6 seconds
				conn.connect();
				in = url.openStream();
				SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(in, new RSSHandler());
				refresh();
				
			} catch (Exception e) {
				IntroPlugin.logError(
						NLS.bind(
								Messages.RSS_Malformed_feed, 
								getParameter("url"))); //$NON-NLS-1$
				refresh();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
				}
				threadRunning = false;
			}

		}

		private void setTimeout(URLConnection conn, int milliseconds) {
			Class conClass = conn.getClass();
			try {
				Method timeoutMethod = conClass.getMethod(
						"setConnectTimeout", new Class[]{ int.class } ); //$NON-NLS-1$
				timeoutMethod.invoke(conn, new Object[] { new Integer(milliseconds)} );
			} catch (Exception e) {
			     // If running on a 1.4 JRE an exception is expected, fall through
			} 
		}
	}
	
	private class TimeoutThread extends Thread
	{
		public void run()
		{
			try{
				Thread.sleep(SOCKET_TIMEOUT);
			}catch(Exception ex){
				IntroPlugin.logError("Timeout failed.", ex); //$NON-NLS-1$
			}
			if (threadRunning)
			{
				threadRunning = false;
				refresh();
			}
		}
	}
}