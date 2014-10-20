package ch.maxant.session_sizes.ui;

import static ch.maxant.session_sizes.ui.SessionListenerTest.buildObject;
import static ch.maxant.session_sizes.ui.SessionListenerTest.setupEvent;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;

import org.ehcache.sizeof.SizeOf;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class SessionSizeHelperTest {

    static {
	System.setProperty("net.sf.ehcache.sizeof.verboseDebugLogging", "true");

	// setup debug logging so we can see the tree walk
	LoggerFactory.getLogger("org.ehcache.sizeof.ObjectGraphWalker");
	LogManager.getLogManager()
		.getLogger("org.ehcache.sizeof.ObjectGraphWalker")
		.setLevel(Level.FINEST);
    }

    @Test
    public void testsizeOfOneSession() throws IllegalArgumentException,
	    IllegalAccessException {

	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	Object obj = buildObject(new Object[] { "obj1" }, "name1",
		Arrays.asList("object1"));
	HttpSessionEvent e = setupEvent(obj);
	sl.sessionCreated(e);

	HttpServletRequest request = mock(HttpServletRequest.class);
	when(request.getSession()).thenReturn(e.getSession());

	SessionSizeHelper ssh = new SessionSizeHelper();
	String size = ssh.getSessionSizeExcludingDuplicates(request);

	SizeOf so = so();
	Set<Object> objs = new HashSet<>();
	objs.add(obj);

	assertEquals(so.deepSizeOf(Integer.MAX_VALUE, false, objs)
		.getCalculated() + " bytes", size);
    }

    @Test
    public void testsizeOfTwoSessionsNoDuplicates()
	    throws IllegalArgumentException, IllegalAccessException {

	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	Object obj = buildObject(new Object[] { "obj1" }, "name1",
		Arrays.asList("object1"));
	HttpSessionEvent e1 = setupEvent(obj);
	sl.sessionCreated(e1);

	obj = buildObject(new Object[] { "obj2" }, "name2",
		Arrays.asList("object2"));
	HttpSessionEvent e2 = setupEvent(obj);
	sl.sessionCreated(e2);

	HttpServletRequest request = mock(HttpServletRequest.class);
	when(request.getSession()).thenReturn(e1.getSession());

	SessionSizeHelper ssh = new SessionSizeHelper();
	String size = ssh.getSessionSizeExcludingDuplicates(request);

	SizeOf so = so();
	Set<Object> objs = new HashSet<>();
	objs.add(obj);

	assertEquals(so.deepSizeOf(Integer.MAX_VALUE, false, objs)
		.getCalculated() + " bytes", size);
    }

    @Test
    public void testsizeOfTwoSessionsWithDuplicates()
	    throws IllegalArgumentException, IllegalAccessException {

	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	Object duplicate1 = "dup1";
	Object duplicate2 = "dup2";
	String duplicate3 = "dup3";

	Object obj = buildObject(new Object[] { "obj1", duplicate1 },
		duplicate3, Arrays.asList("object1", duplicate2));
	HttpSessionEvent e1 = setupEvent(obj);
	sl.sessionCreated(e1);

	obj = buildObject(new Object[] { "obj2", duplicate1 }, duplicate3,
		Arrays.asList("object2", duplicate2));
	HttpSessionEvent e2 = setupEvent(obj);
	sl.sessionCreated(e2);

	HttpServletRequest request = mock(HttpServletRequest.class);
	when(request.getSession()).thenReturn(e1.getSession());

	Set<Object> objs = new HashSet<>();
	objs.add(obj);
	SizeOf so = so();
	long expectedBytes = 0L;
	expectedBytes += so.deepSizeOf(Integer.MAX_VALUE, false, objs)
		.getCalculated();

	// visiting java.util.HashSet@888473870
	// 16b java.util.HashSet@888473870
	// 48b java.util.HashMap@837108062
	// 80b [Ljava.util.HashMap$Node;@170144208
	// 32b java.util.HashMap$Node@482052083
	// 16b java.lang.Object@1604353554
	// 24b
	// ch.maxant.session_sizes.ui.SessionListenerTest$Container@1765795529
	// 24b java.util.Arrays$ArrayList@1720339
	// 24b [Ljava.lang.Object;@460201727
	// 24b java.lang.String@812586739
	// 24b [C@1881901842
	// 24b java.lang.String@370370379
	// 32b [C@585324508
	// 24b java.lang.String@1234250905
	// 24b [C@16868310
	// 24b [Ljava.lang.Object;@769530879
	// 24b java.lang.String@364639279
	// 24b [C@1427040229
	// 24b java.lang.String@11939193
	// 24b [C@1604002113
	// Total size: 536 bytes

	expectedBytes -= so.sizeOf(duplicate1);
	expectedBytes -= so.sizeOf(duplicate2);
	expectedBytes -= so.sizeOf(duplicate3);

	// TEST
	SessionSizeHelper ssh = new SessionSizeHelper();
	String size = ssh.getSessionSizeExcludingDuplicates(request);

	assertEquals(expectedBytes + " bytes", size);
    }

    private SizeOf so() {
	SizeOf so = SizeOf.newInstance();
	return so;
    }

}
