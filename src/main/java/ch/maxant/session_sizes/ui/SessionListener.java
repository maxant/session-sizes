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

/** tracks all sessions known to the node where this listener runs. */
@WebListener
public class SessionListener implements HttpSessionListener {

    private static final Logger LOGGER = LoggerFactory
	    .getLogger(SessionListener.class);

    /** all active sessions */
    private static final Set<HttpSession> sessions = Collections
	    .synchronizedSet(new HashSet<HttpSession>());

    /** ignore all immutables/primatives because it doesnt help to inspect them */
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
	logActiveSessions();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent e) {
	sessions.remove(e.getSession());
	logActiveSessions();
    }

    private void logActiveSessions() {
	LOGGER.info("there are {} active sessions", sessions.size());
    }

    /**
     * returns all objects that exist in this session, but also in other
     * sessions. only considers objects found in the attribute map of the
     * sessions.
     */
    public static Set<Object> getDuplicateObjects(HttpSession session)
	    throws IllegalArgumentException, IllegalAccessException {

	Map<Integer, Object> allObjectsInAllSessionsExceptGivenSession = collectAllObjectsKnownToOtherSessions(session);

	Map<Integer, Object> objectsOnlyInGivenSession = collectAllObjectsKnownToGivenSession(session);

	Set<Object> duplicates = calculateDelta(
		allObjectsInAllSessionsExceptGivenSession,
		objectsOnlyInGivenSession);

	return duplicates;
    }

    private static Set<Object> calculateDelta(
	    Map<Integer, Object> allObjectsInAllSessionsExceptGivenSession,
	    Map<Integer, Object> objectsOnlyInGivenSession) {
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

    private static Map<Integer, Object> collectAllObjectsKnownToGivenSession(
	    HttpSession session) throws IllegalAccessException {
	LOGGER.info("collecting objects from given session");
	Map<Integer, Object> objectsOnlyInGivenSession = new HashMap<>();
	Enumeration<String> names = session.getAttributeNames();
	while (names.hasMoreElements()) {
	    String name = names.nextElement();
	    Object attr = session.getAttribute(name);
	    collectAllObjects(attr, objectsOnlyInGivenSession, 0);
	}
	return objectsOnlyInGivenSession;
    }

    private static Map<Integer, Object> collectAllObjectsKnownToOtherSessions(
	    HttpSession session) throws IllegalAccessException {
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
	return allObjectsInAllSessionsExceptGivenSession;
    }

    /**
     * uses reflection to recursively fetch every single basic object and stick
     * it in the given set. only adds if not already contained.
     */
    private static void collectAllObjects(Object obj,
	    Map<Integer, Object> allKnownSessionObjects, int depth)
	    throws IllegalArgumentException, IllegalAccessException {
	int hash = System.identityHashCode(obj);
	if (LOGGER.isTraceEnabled()) {
	    LOGGER.trace("{}\t{}", space(depth) + hash, obj);
	}
	if (obj != null) {
	    if (allKnownSessionObjects.containsKey(hash)) {
		if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("already visited {}\t{}", hash, obj);
		}
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
			    collectAllObjects(o, allKnownSessionObjects,
				    depth + 1);
			}
		    }

		    // was it an array of objects? then also analyse each
		    // individual object
		    if (obj.getClass().isArray()) {
			try {
			    Object[] os = (Object[]) obj;
			    for (Object o : os) {
				collectAllObjects(o, allKnownSessionObjects,
					depth + 1);
			    }
			} catch (ClassCastException e) {
			    // ok, not worth investigating...
			}
		    }
		}
	    }
	}
    }

    private static String space(int level) {
	StringBuilder ret = new StringBuilder("");
	while (ret.length() < level) {
	    ret.append(" ");
	}
	return ret.toString();
    }

}
