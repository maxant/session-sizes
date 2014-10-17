<%
session.invalidate();
request.getSession(true);
response.sendRedirect("index.jsp");
%>