package ch.maxant.session_sizes.ui;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.ehcache.sizeof.SizeOf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StopWatch;

@Named
@Scope("request")
public class SessionSizeHelper {

    private static final Logger LOGGER = LoggerFactory
	    .getLogger(SessionSizeHelper.class);

    private static final int MAX_DEPTH = Integer.MAX_VALUE;

    /**
     * returns the size of the session, simply measuring all session attributes,
     * as you might typically do. measures deep size but does not care if an
     * object is referenced by another session, and as such doesnt take
     * duplicates into consideration.
     */
    private long getSessionSizeInBytes(HttpServletRequest request) {
	// assemble all attributes into one collection, rather than measuring
	// individually,
	// so that the handing of duplicates within the session is done by
	// SizeOf.
	Set<Object> attrs = new HashSet<>();
	Enumeration<String> names = request.getSession().getAttributeNames();
	while (names.hasMoreElements()) {
	    String name = names.nextElement();
	    Object attr = request.getSession().getAttribute(name);
	    attrs.add(attr);
	}
	return SizeOf.newInstance().deepSizeOf(MAX_DEPTH, false, attrs)
		.getCalculated();
    }

    /**
     * returns a pretty formatted size of the session, simply measuring all
     * session attributes, as you might typically do.
     */
    public String getSessionSize(HttpServletRequest request) {
	return FileUtils.byteCountToDisplaySize(getSessionSizeInBytes(request));
    }

    public String getSessionAttributes(HttpServletRequest request) {
	final SizeOf sizeof = SizeOf.newInstance();
	String ret = "<ul>";
	Enumeration<String> names = request.getSession().getAttributeNames();
	while (names.hasMoreElements()) {
	    String name = names.nextElement();
	    Object attr = request.getSession().getAttribute(name);
	    ret += "<li>"
		    + name
		    + " ("
		    + FileUtils.byteCountToDisplaySize(sizeof.deepSizeOf(
			    MAX_DEPTH, false, attr).getCalculated()) + ")/"
		    + attr + "</li>";
	}
	ret += "</ul>";
	return ret;
    }

    /**
     * removes the size of objects that are found in other sessions. does not
     * remove the size of the references to those objects because they are part
     * of your session.
     */
    public String getSessionSizeExcludingDuplicates(HttpServletRequest request)
	    throws IllegalArgumentException, IllegalAccessException {

	final StopWatch watch = new StopWatch();

	watch.start("calculate size of session, including duplicates");
	long bytes = getSessionSizeInBytes(request);

	watch.stop();
	watch.start("find duplicates");
	Set<Object> duplicates = SessionListener.getDuplicateObjects(request
		.getSession());

	watch.stop();
	watch.start("instantiate sizeof");
	final SizeOf sizeof = SizeOf.newInstance();

	watch.stop();
	watch.start("remove size of duplicates");
	for (Object o : duplicates) {
	    long size = sizeof.sizeOf(o); // not deep, since we have already
					  // walked the graph
	    bytes -= size;
	}

	watch.stop();
	watch.start("prettify size");
	String s = FileUtils.byteCountToDisplaySize(bytes);

	watch.stop();
	LOGGER.info(watch.prettyPrint());

	return s;
    }

}
