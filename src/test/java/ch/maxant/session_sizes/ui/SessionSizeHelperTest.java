package ch.maxant.session_sizes.ui;

import static ch.maxant.session_sizes.ui.SessionListenerTest.buildObject;
import static ch.maxant.session_sizes.ui.SessionListenerTest.setupEvent;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;

import org.ehcache.sizeof.SizeOf;
import org.junit.Test;

public class SessionSizeHelperTest {

    @Test
    public void testsizeOfOneSession() throws IllegalArgumentException,
	    IllegalAccessException {

	SessionListener sl = new SessionListener();
	Object obj = buildObject(new Object[] { "obj1" }, "name1",
		Arrays.asList("object1"));
	HttpSessionEvent e = setupEvent(obj);
	sl.sessionCreated(e);

	HttpServletRequest request = mock(HttpServletRequest.class);
	when(request.getSession()).thenReturn(e.getSession());

	SessionSizeHelper ssh = new SessionSizeHelper();
	String size = ssh.getSessionSizeExcludingDuplicates(request);

	assertEquals("456 bytes", size);
    }

    @Test
    public void testsizeOfTwoSessionsNoDuplicates()
	    throws IllegalArgumentException, IllegalAccessException {

	SessionListener sl = new SessionListener();

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

	assertEquals("456 bytes", size);
    }

    @Test
    public void testsizeOfTwoSessionsWithDuplicates()
	    throws IllegalArgumentException, IllegalAccessException {

	SessionListener sl = new SessionListener();

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
	SizeOf so = SizeOf.newInstance();
	long expectedBytes = 0L;
	expectedBytes += so.deepSizeOf(Integer.MAX_VALUE, false, objs)
		.getCalculated();
	expectedBytes -= so.sizeOf(duplicate1);
	expectedBytes -= so.sizeOf(duplicate2);
	expectedBytes -= so.sizeOf(duplicate3);

	// TEST
	SessionSizeHelper ssh = new SessionSizeHelper();
	String size = ssh.getSessionSizeExcludingDuplicates(request);

	assertEquals(expectedBytes + " bytes", size);
    }

}
