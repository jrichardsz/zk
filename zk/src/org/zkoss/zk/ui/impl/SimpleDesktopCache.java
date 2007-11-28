/* SimpleDesktopCache.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Tue Apr 18 13:00:34     2006, Created by tomyeh
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zk.ui.impl;

import java.util.Iterator;
import java.util.ArrayList;

import org.zkoss.util.CacheMap;
import org.zkoss.util.logging.Log;

import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zk.ui.util.Configuration;
import org.zkoss.zk.ui.util.Monitor;
import org.zkoss.zk.ui.sys.DesktopCache;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.sys.WebAppCtrl;

/**
 * A simple implementation of {@link DesktopCache}. It manages
 * all desktop in a {@link CacheMap} instance.
 *
 * @author tomyeh
 */
public class SimpleDesktopCache implements DesktopCache, java.io.Serializable {
	private static final Log log = Log.lookup(SimpleDesktopCache.class);
    private static final long serialVersionUID = 20060622L;

	/** Used to purge obsolete desktops. */
	private final Cache _desktops;
	/** The next available key. */
	private int _nextKey = ((int)System.currentTimeMillis()) & 0xffff;
		//to reduce the chance that two browsers with the same desktop ID
		//it is possible if we re-boot the server

	public SimpleDesktopCache(Configuration config) {
		_desktops = new Cache(config);
	}

	//-- DesktopCache --//
	public int getNextKey() {
		synchronized (this) {
			return _nextKey++;
		}
	}
	public Desktop getDesktopIfAny(String desktopId) {
		synchronized (_desktops) {
			return (Desktop)_desktops.get(desktopId);
		}
	}
	public Desktop getDesktop(String desktopId) {
		final Desktop desktop;
		synchronized (_desktops) {
			desktop = (Desktop)_desktops.get(desktopId);
		}
		if (desktop == null)
			throw new ComponentNotFoundException("Desktop not found: "+desktopId);
		return desktop;
	}
	public void addDesktop(Desktop desktop) {
		final boolean added;
		final Object old;
		synchronized (_desktops) {
			old = _desktops.put(desktop.getId(), desktop);
		}
		if (old != null) {
			_desktops.put(((Desktop)old).getId(), old); //recover
			log.warning(
				desktop == old ? "Register a desktop twice: "+desktop:
					"Replicated ID: "+desktop+"; already used by "+old);
		}
		//if (log.debugable()) log.debug("After added, desktops: "+_desktops);
	}
	public void removeDesktop(Desktop desktop) {
		final Object old;
		synchronized (_desktops) {
			old = _desktops.remove(desktop.getId());
		}
		if (old == null)
			log.warning("Removing non-existent desktop: "+desktop);
		desktopDestroyed(desktop);
	}
	private static void desktopDestroyed(Desktop desktop) {
		final WebApp wapp = desktop.getWebApp();
		wapp.getConfiguration().invokeDesktopCleanups(desktop);
			//Feature 1767347: call DesktopCleanup before desktopDestroyed
			//such that app dev has a chance to manipulate the desktop
		((WebAppCtrl)wapp).getUiEngine().desktopDestroyed(desktop);

		final Monitor monitor = desktop.getWebApp().getConfiguration().getMonitor();
		if (monitor != null) {
			try {
				monitor.desktopDestroyed(desktop);
			} catch (Throwable ex) {
				log.error(ex);
			}
		}
	}

	/** Invokes {@link DesktopCtrl#sessionWillPassivate} for each
	 * desktops it cached.
	 */
	public void sessionWillPassivate(Session sess) {
		synchronized (_desktops) {
			for (Iterator it = _desktops.values().iterator(); it.hasNext();)
				((DesktopCtrl)it.next()).sessionWillPassivate(sess);
		}
	}
	/** Invokes {@link DesktopCtrl#sessionDidActivate} for each
	 * desktops it cached.
	 */
	public void sessionDidActivate(Session sess) {
		synchronized (_desktops) {
			for (Iterator it = _desktops.values().iterator(); it.hasNext();)
				((DesktopCtrl)it.next()).sessionDidActivate(sess);
		}
	}

	public void stop() {
		synchronized (_desktops) {
			if (log.debugable()) log.debug("Invalidated and remove: "+_desktops);
			for (Iterator it = new ArrayList(_desktops.values()).iterator();
			it.hasNext();) {
				desktopDestroyed((Desktop)it.next());
			}
			_desktops.clear();
		}
	}

	/** Holds desktops. */
	private static class Cache extends CacheMap { //serializable
		private Cache(Configuration config) {
			super(13);

			int v = config.getSessionMaxDesktops();
			setMaxSize(v >= 0 ? v: Integer.MAX_VALUE / 2);

			v = config.getDesktopMaxInactiveInterval();
			setLifetime(v >= 0 ? v * 1000: Integer.MAX_VALUE / 2);
		}
		protected boolean shallExpunge() {
			return super.shallExpunge()
				|| sizeWithoutExpunge() > (getMaxSize() << 1);
				//to minimize memory use, expunge even if no GC
		}
		protected int canExpunge(Value v) {
			if (((Desktop)v.getValue()).getExecution() != null)
				return EXPUNGE_NO|EXPUNGE_CONTINUE;
			return super.canExpunge(v);
		}
		protected void onExpunge(Value v) {
			super.onExpunge(v);

			desktopDestroyed((Desktop)v.getValue());
			if (log.debugable()) log.debug("Expunge desktop: "+v.getValue());
		}
	}
}
