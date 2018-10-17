<%@ page import="java.net.InetAddress" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.io.InputStream" %>
<%--
  Created by IntelliJ IDEA.
  User: marko (motting@qualogy.com)
  Date: 17-10-2018
  Time: 10:43

  The problem with our F5 load balancer is that it is currently TCP based. If WLS crashes but still returns TCP ACKs our
  cluster member is not removed from the load balancer server list and generates lots of errors.

  We can point the load balancer to an arbitrary page, but that does not give us application owners control over when
  to LB or not to LB. Also, this could generate load.

  This page offers a solution for these problems.

  - In operational mode: return a friendly (light weight) page with an OKAY message.
  - If a "disable" message is send in the URL a temporary file is created in the domain directory. If this file exists, the
    page will always return a HTTP 503 SERVICE UNAVAILABLE with a custom error message.
  - If an "enable" message is send, the file is removed and the OKAY page is displayed again
  - If the server is halting, as any WLS app this page returns a HTTP 500 error code.

  This way, we can tell the load balancer an individual server is not available by calling the URL or placing the temp
  file ourselves.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    // Horrible code to check if I have a cluster brother already disabled
    // Just overwrite this in your own code or whatever
    private boolean getClusterBrotherIsOkay(Properties prop){
        boolean result = true;
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if(prop.containsKey(hostname)) {
                String urlString = prop.getProperty(hostname);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                HttpURLConnection.setFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setReadTimeout(8000);
                result = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }
%>
<%
    final String LB_PARAMETER = "loadbalancer";
    boolean clusterIsOkay = true;
    String hostname = InetAddress.getLocalHost().getHostName();
    String serverName = System.getProperty("weblogic.Name");

    // This has to be done to prevent the FrontEndHost from overwriting us
    String pageURL = "http://" + hostname + ":" + request.getServerPort() + request.getRequestURI();
    String requestParam = request.getParameter(LB_PARAMETER);

    // By using the name of the server, we can run multiple checks on a single machine
    File stateFile = new File(serverName + ".state");

    ServletContext sc = config.getServletContext();
    if (requestParam != null) {
        if (requestParam.equals("enable") && stateFile.exists()) {
            stateFile.delete();
            sc.log(serverName + " no longer returns an error message for health checks and should be returned to the load balancer pool.");
        } else if (requestParam.equals("disable") && !stateFile.exists()) {

            // The server list contains a map and URLs to check. The use case is when you have a two-server cluster and want to check
            // the other server health state before allowing this one to result an error to the F5
            InputStream sis = config.getServletContext().getResourceAsStream("/WEB-INF/lib/serverlist.properties");
            Properties serverList = new Properties();
            serverList.load(sis);
            clusterIsOkay = getClusterBrotherIsOkay(serverList);
            if (clusterIsOkay) {
                stateFile.createNewFile();
                sc.log(serverName + " now returns an error message for health checks and will probably be removed from the load balancer pool.");
            } else {
                sc.log(serverName + " tried to set itself to be removed from the load balancer pool, but the other clusternode was already disabled or unavailable!");
            }
        }
    }

    if (stateFile.exists()) {
        String responseString = "Load balancer, please do not send me messages. Enable at " + pageURL + "?" + LB_PARAMETER + "=enable";
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseString);
    }
%>
<html>
<head>
    <title>Hello from  <%= serverName %>
    </title>
</head>
<body>
<h1>Hello from <%= serverName %> at <%= hostname %>
</h1>
<% if(!clusterIsOkay) {
    %>
    <h1 style="color: red;">Refused to disable the health check as the other node in the cluster is not okay or already disabled</h1>
<%
}
%>
<h2>Set the load balancer result on this server to ON with:<br>
    <a href="<%= pageURL %>?<%=LB_PARAMETER%>=enable">THIS URL</a>
</h2>
<h2>Set the load balancer result on this server to OFF with:<br>
    <a href="<%= pageURL %>?<%=LB_PARAMETER%>=disable">THIS URL</a>
</h2>
</body>
</html>
