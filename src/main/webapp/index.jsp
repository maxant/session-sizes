<%@page import="ch.maxant.session_sizes.ui.SessionSizeHelper"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="org.ehcache.sizeof.Size"%>
<%@page import="java.util.Enumeration"%>
<%@page import="org.ehcache.sizeof.SizeOf"%>
<%@page import="ch.maxant.session_sizes.services.DataService"%>
<%@page import="org.springframework.web.context.WebApplicationContext"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%
WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(request.getSession().getServletContext());
DataService ds = wac.getBean(DataService.class);
session.setAttribute("someData", ds.generateData());
%>
<html>
<body>
I just put one meg of rubbish in your session! <br/>
The session is <%=new SessionSizeHelper().getSessionSize(request) %> large.<br/>
Session size according to library: <%=new SessionSizeHelper().getSessionSizeExcludingDuplicates(request) %><br/>

<br/>
<a href='jsfRequestScoped.xhtml'>JSF Request Scoped Bean Usage</a><br/>
<a href='jsfSessionScoped.xhtml'>JSF Session Scoped Bean Usage</a><br/>
<a href='invalidateSession.jsp'>invalidate session</a><br/>

<hr/>
<%=new SessionSizeHelper().getSessionAttributes(request) %>

</body>
</html>
