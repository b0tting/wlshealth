package nl.qualogy.com;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;

public class HealthCheckServlet extends HttpServlet {
    private static ZZHealthCheck zhc = null;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(zhc == null) {
            zhc = new ZZHealthCheck(req.getContextPath() + req.getServletPath(),
                    getServletContext().getInitParameter("ignoreServers"));
        }

        //Actually, I can do this better, we know what machines are involved, we can do a
        //very specific CORS header.
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        resp.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
        resp.addHeader("Access-Control-Max-Age", "1728000");

        String path = req.getPathInfo();
        System.out.println("PATGH " + path);
        if(path == null) {
            RequestDispatcher requestDispatcher = req.getRequestDispatcher("/index.html");
            requestDispatcher.forward(req, resp);
        } else if(path.equals(ZZHealthCheck.LB_URI_PART)) {
            if(zhc.getThisServerEnabled()) {
                zhc.lbHit();
                doJSONResponse(resp, "{\"is_enabled\": " + zhc.getThisServerEnabled() + "}");
            } else {
                PrintWriter out = resp.getWriter();
                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF-8");
                resp.setStatus(HttpURLConnection.HTTP_UNAVAILABLE);
                out.print("This server has requested to be removed from the load balancer");
                out.flush();
            }
        } else if(path.equals(ZZHealthCheck.STATE_URI_PART)) {
            doJSONResponse(resp, "{\"is_enabled\": " + zhc.getThisServerEnabled() + ", \"hits\":\""+zhc.getHitCount()+"\"}");
        } else if(path.equals(ZZHealthCheck.DISABLE_URI_PART)) {
            zhc.setThisServerDisabled();
            doJSONResponse(resp, "{\"is_enabled\": " + zhc.getThisServerEnabled() + "}");
        } else if(path.equals(ZZHealthCheck.ENABLE_URI_PART)) {
            zhc.setThisServerEnabled();
            doJSONResponse(resp, "{\"is_enabled\": " + zhc.getThisServerEnabled() + "}");
        } else if(path.equals(ZZHealthCheck.INFO_URI_PART)) {
            HashMap<String, HealthCheckServerConfig> servers = zhc.getServerInfo();
            StringBuilder returnJson = new StringBuilder("{");
            for(String server: servers.keySet()) {
                HealthCheckServerConfig serverConfig = servers.get(server);
                returnJson.append(serverConfig.getAsJSON());
                returnJson.append(",");
            }
            returnJson.deleteCharAt(returnJson.length() -1);
            returnJson.append("}");
            doJSONResponse(resp, returnJson.toString());
        } else {
            resp.getWriter().println("NOPE");
        }
    }

    private void doJSONResponse(HttpServletResponse resp, String json) throws IOException {
        PrintWriter out = resp.getWriter();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        out.print(json);
        out.flush();
    }
}
