package ch.maxant.session_sizes.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Test;

public class SessionListenerTest {

    static HttpSessionEvent setupEvent(Object obj) {
	HttpSession sess = mock(HttpSession.class);
	final Vector<String> names = new Vector<>();
	names.add("name");
	when(sess.getAttributeNames()).thenReturn(names.elements());
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

	sl.sessionDestroyed(e1);
	sl.sessionDestroyed(e2);
    }

    @Test
    public void testTwoSessionsWithADuplicate1()
	    throws IllegalArgumentException, IllegalAccessException {
	testTwoSessionsWithDuplicates(true);
    }

    @Test
    public void testTwoSessionsWithADuplicate2()
	    throws IllegalArgumentException, IllegalAccessException {
	testTwoSessionsWithDuplicates(false);
    }

    private void testTwoSessionsWithDuplicates(boolean testFirstSession)
	    throws IllegalArgumentException, IllegalAccessException {
	SessionListener sl = new SessionListener();
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

	// because we are using mocks, the enumeration of attribute names cannot
	// be walked twice
	// hence we run this test twice, and test each session once
	assertDuplicates(testFirstSession ? e1 : e2);

	sl.sessionDestroyed(e1);
	sl.sessionDestroyed(e2);
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
