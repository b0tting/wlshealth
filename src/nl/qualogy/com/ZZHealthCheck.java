package nl.qualogy.com;

import javax.management.*;
import javax.naming.InitialContext;
import java.net.HttpURLConnection;
import java.util.*;

public class ZZHealthCheck {
    private static HashMap<String, HealthCheckServerConfig> servers = null;
    private static boolean enabled = true;
    private String baseURL = null;

    public static final int UNAVAILABLE_RESPONSE_CODE = HttpURLConnection.HTTP_UNAVAILABLE;

    public static final int STATE_LB_ENABLED = 0;
    public static final int STATE_LB_DISABLED = 1;
    public static final int STATE_NO_CHECK = 2;
    public static final int STATE_NO_SERVER = 3;
    public static final int STATE_UNKNOWN = 4;
    public static final int STATE_TIMEOUT = 5;


    public static final String STATE_URI_PART = "/state";
    public static final String ENABLE_URI_PART = "/enable";
    public static final String DISABLE_URI_PART = "/disable";
    public static final String INFO_URI_PART = "/info";
    public static final String LB_URI_PART = "/lb";

    public ZZHealthCheck(String baseURL) {
        this.baseURL = baseURL;
    }

    private static long hitcount = 0;

    public static String getStateLabel(int status) {
        String label = "unknown";
        switch(status) {
            case STATE_LB_ENABLED: label = "enabled"; break;
            case STATE_LB_DISABLED: label = "disabled"; break;
            case STATE_NO_CHECK: label = "no lb check deployed"; break;
            case STATE_NO_SERVER: label = "server unavailable"; break;
            case STATE_UNKNOWN: label = "unknown state"; break;
            case STATE_TIMEOUT: label = "check timed out"; break;
        }
        return label;
    }

    public void setFromJMX() {
        try{
            InitialContext ctx = new InitialContext();
            MBeanServer mbServer = (MBeanServer)ctx.lookup("java:comp/env/jmx/runtime");
            Set<ObjectInstance> mbResult = mbServer.queryMBeans(new ObjectName("com.bea:Name=*,Type=Server"), null);
            for (ObjectInstance instance : mbResult) {
                String listenAddress = (String)mbServer.getAttribute(instance.getObjectName(), "ListenAddress");
                String name = (String)mbServer.getAttribute(instance.getObjectName(), "Name");
                Integer port = (Integer)mbServer.getAttribute(instance.getObjectName(), "ListenPort");
                servers.put(name, new HealthCheckServerConfig(name, listenAddress, port, baseURL));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, HealthCheckServerConfig> getServerInfo() {
        if(servers == null) {
            servers = new HashMap<>();
            setFromJMX();
        }
        return servers;
    }

    public void lbHit() {
        hitcount++;
    }

    public String getHitCount() {
        return Long.toString(hitcount);
    }

    public boolean getThisServerEnabled() {
        return enabled;
    }

    public void setThisServerEnabled() {
        enabled = true;
    }

    public void setThisServerDisabled() {
        enabled = false;
    }
}
