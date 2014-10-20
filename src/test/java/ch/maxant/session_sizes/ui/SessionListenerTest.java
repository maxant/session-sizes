package ch.maxant.session_sizes.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SessionListenerTest {

    static HttpSessionEvent setupEvent(Object obj) {
	HttpSession sess = mock(HttpSession.class);
	when(sess.getAttributeNames()).thenAnswer(
		new Answer<Enumeration<String>>() {
		    @Override
		    public Enumeration<String> answer(
			    InvocationOnMock invocation) throws Throwable {
			final Vector<String> names = new Vector<>();
			names.add("name");
			return names.elements();
		    }
		});
	when(sess.getAttribute("name")).thenReturn(obj);
	HttpSessionEvent e = new HttpSessionEvent(sess);
	return e;
    }

    static Object buildObject(Object[] objs, String string,
	    List<? extends Object> objList) {
	return new Container(objs, string, objList);
    }

    @Test
    public void testOneSession() throws IllegalArgumentException,
	    IllegalAccessException {
	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	Object obj = buildObject(new Object[] { "obj1" }, "name1",
		Arrays.asList("object1"));
	HttpSessionEvent e = setupEvent(obj);
	sl.sessionCreated(e);

	Set<Object> duplicateObjects = SessionListener.getDuplicateObjects(e
		.getSession());

	assertEquals(0, duplicateObjects.size());

	sl.sessionDestroyed(e);
    }

    @Test
    public void testTwoSessionsWithNoDuplicates()
	    throws IllegalArgumentException, IllegalAccessException {
	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	Object obj1 = buildObject(new Object[] { new String("obj1") }, "name1",
		Arrays.asList(new String("object1")));
	HttpSessionEvent e1 = setupEvent(obj1);
	sl.sessionCreated(e1);

	Object obj2 = buildObject(new Object[] { new String("obj2") }, "name2",
		Arrays.asList(new String("object2")));
	HttpSessionEvent e2 = setupEvent(obj2);
	sl.sessionCreated(e2);

	Set<Object> duplicateObjects = SessionListener.getDuplicateObjects(e1
		.getSession());
	assertEquals(0, duplicateObjects.size());

	duplicateObjects = SessionListener.getDuplicateObjects(e2.getSession());
	assertEquals(0, duplicateObjects.size());
    }

    @Test
    public void testTwoSessionsWithADuplicate()
	    throws IllegalArgumentException, IllegalAccessException {
	SessionListener sl = new SessionListener();
	sl.clearAllSessionForTesting();

	final Object duplicate1 = "duplicate1";
	final Object duplicate2 = "duplicate2";
	final String duplicate3 = "duplicate3";
	Object obj1 = buildObject(new Object[] { duplicate1 }, duplicate3,
		Arrays.asList("object1", duplicate2));
	HttpSessionEvent e1 = setupEvent(obj1);
	sl.sessionCreated(e1);

	Object obj2 = buildObject(new Object[] { duplicate1 }, duplicate3,
		Arrays.asList("object2", duplicate2));
	HttpSessionEvent e2 = setupEvent(obj2);
	sl.sessionCreated(e2);

	assertDuplicates(e1);
	assertDuplicates(e2);
    }

    private void assertDuplicates(HttpSessionEvent e)
	    throws IllegalAccessException {
	Set<Object> duplicateObjects = SessionListener.getDuplicateObjects(e
		.getSession());
	assertEquals(3, duplicateObjects.size());
	assertTrue(duplicateObjects.contains("duplicate1"));
	assertTrue(duplicateObjects.contains("duplicate2"));
	assertTrue(duplicateObjects.contains("duplicate3"));
    }

    private static final class Container {
	Object[] objectArray;
	String someName;
	List<? extends Object> objectList;

	Container(Object[] objectArray, String someName,
		List<? extends Object> objectList) {
	    this.objectArray = objectArray;
	    this.someName = someName;
	    this.objectList = objectList;
	}
    }
}
