package ch.maxant.session_sizes.ui;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** tracks all sessions known to the node */
@WebListener
public class SessionListener implements HttpSessionListener {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SessionListener.class);

	private static final Set<HttpSession> sessions = Collections
			.synchronizedSet(new HashSet<HttpSession>());

	/** ignore all immutables */
	private static Set<Class<?>> TYPES_TO_IGNORE = new HashSet<>();
	static {
		TYPES_TO_IGNORE.add(AtomicInteger.class);
		TYPES_TO_IGNORE.add(AtomicLong.class);
		TYPES_TO_IGNORE.add(BigDecimal.class);
		TYPES_TO_IGNORE.add(BigInteger.class);
		TYPES_TO_IGNORE.add(Long.class);
		TYPES_TO_IGNORE.add(Byte.class);
		TYPES_TO_IGNORE.add(Double.class);
		TYPES_TO_IGNORE.add(Float.class);
		TYPES_TO_IGNORE.add(Integer.class);
		TYPES_TO_IGNORE.add(Byte.class);
		TYPES_TO_IGNORE.add(Long.class);
		TYPES_TO_IGNORE.add(Short.class);
		TYPES_TO_IGNORE.add(String.class);
		TYPES_TO_IGNORE.add(Boolean.class);
		TYPES_TO_IGNORE.add(Date.class);
	}

	@Override
	public void sessionCreated(HttpSessionEvent e) {
		sessions.add(e.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent e) {
		sessions.remove(e.getSession());
	}

	/**
	 * returns all objects that exist in this session, but also in other
	 * sessions. only considers objects found in the attribute map of the
	 * sessions.
	 */
	public static Set<Object> getDuplicateObjects(HttpSession session)
			throws IllegalArgumentException, IllegalAccessException {

		// collect all objects known to other sessions
		LOGGER.info("collecting all known session objects");
		Map<Integer, Object> allObjectsInAllSessionsExceptGivenSession = new HashMap<>();
		for (HttpSession s : sessions) {
			if (s != session) { // skip this session, since we want the delta!
				try {
					Enumeration<String> names = s.getAttributeNames();
					while (names.hasMoreElements()) {
						String name = names.nextElement();
						Object attr = s.getAttribute(name);
						collectAllObjects(attr,
								allObjectsInAllSessionsExceptGivenSession, 0);
					}
				} catch (IllegalStateException e) {
					// happens for example if a session is invalidated but not
					// yet removed
					// ignore this session and move to the next one, ie do
					// nothing.
				}
			}
		}

		// now collect all objects known to this session
		LOGGER.info("collecting objects from given session");
		Map<Integer, Object> objectsOnlyInGivenSession = new HashMap<>();
		Enumeration<String> names = session.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Object attr = session.getAttribute(name);
			collectAllObjects(attr, objectsOnlyInGivenSession, 0);
		}

		// calculate delta
		LOGGER.info("calculating duplicates");
		Set<Object> duplicates = new HashSet<>();
		for (Map.Entry<Integer, Object> e : objectsOnlyInGivenSession
				.entrySet()) {
			if (allObjectsInAllSessionsExceptGivenSession.containsKey(e
					.getKey())) {
				duplicates.add(e.getValue());
			}
		}

		return duplicates;
	}

	/**
	 * uses reflection to recursively fetch every single basic object and stick
	 * it in the given set. only adds if not already contained.
	 */
	private static void collectAllObjects(Object obj,
			Map<Integer, Object> allKnownSessionObjects, int level)
			throws IllegalArgumentException, IllegalAccessException {
		int hash = System.identityHashCode(obj);
		if (obj != null) {
			if (allKnownSessionObjects.containsKey(hash)) {
				LOGGER.info("already visited " + hash + "\t" + obj);
			} else {
				allKnownSessionObjects.put(hash, obj);
				if (!(TYPES_TO_IGNORE.contains(obj.getClass()))) {
					Set<Field> fields = new HashSet<>();
					fields.addAll(Arrays.asList(obj.getClass().getFields()));
					fields.addAll(Arrays.asList(obj.getClass()
							.getDeclaredFields()));
					for (Field f : fields) {
						// dont worry about statics - they are not shared
						if (!java.lang.reflect.Modifier.isStatic(f
								.getModifiers())) {
							f.setAccessible(true);
							Object o = f.get(obj);
							LOGGER.info(space(level) + hash + "\t" + o);
							collectAllObjects(o, allKnownSessionObjects,
									level + 1);
						}
					}
				}
			}
		}
	}

	private static String space(int level) {
		String ret = "";
		while (ret.length() < level) {
			ret += " ";
		}
		return ret;
	}

}
