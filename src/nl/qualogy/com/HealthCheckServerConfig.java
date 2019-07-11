package nl.qualogy.com;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.HttpURLConnection ;
import java.util.Map;
import java.util.TreeMap;

public class HealthCheckServerConfig {
    private TreeMap<String,String> attributes = new TreeMap<String, String>();

    public HealthCheckServerConfig(String name, String host, Integer port, String baseURI) {
        attributes.put("name", name);
        attributes.put("port", port.toString());
        attributes.put("host",host);
        attributes.put("baseURL","http://" + host + ":" + port + baseURI);
        attributes.put("stateURL", getURLString(ZZHealthCheck.STATE_URI_PART));
        attributes.put("disableURL", getURLString(ZZHealthCheck.DISABLE_URI_PART));
        attributes.put("enableURL", getURLString(ZZHealthCheck.ENABLE_URI_PART));
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public String getURLString(String part){
        return attributes.get("baseURL") + part;
    }

    public int getStatus() {
        int returnVal = ZZHealthCheck.STATE_UNKNOWN;
        HttpURLConnection request = null;
        try {
            URL url = new URL(getURLString(ZZHealthCheck.LB_URI_PART));
            request = (HttpURLConnection) url.openConnection();

            // This should be configureable in the web.xml
            request.setReadTimeout(5000);
            request.setConnectTimeout(5000);
            request.connect();
            if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
                returnVal = ZZHealthCheck.STATE_LB_ENABLED;
            } else if (request.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                returnVal = ZZHealthCheck.STATE_NO_CHECK;
            } else if (request.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
                returnVal = ZZHealthCheck.STATE_LB_DISABLED;
            }
        } catch (SocketTimeoutException e) {
            returnVal = ZZHealthCheck.STATE_TIMEOUT;
        } catch (IOException e) {
            returnVal = ZZHealthCheck.STATE_NO_SERVER;
        } catch (Exception e) {
            returnVal = ZZHealthCheck.STATE_UNKNOWN;
        } finally {
            if(request != null) {
                request.disconnect();
            }
        }
        return returnVal;
    }


    public String getAsJSON() {
        StringBuilder json = new StringBuilder("\"" + attributes.get("name"));
        json.append("\":{");
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            appendToJSONSB(json, attribute.getKey(), attribute.getValue());
        }
        int state = getStatus();
        appendToJSONSB(json, "state", Integer.toString(state));
        appendToJSONSB(json, "statelabel", ZZHealthCheck.getStateLabel(state));
        json.deleteCharAt(json.length() -1);
        json.append("}");
        return json.toString();
    }

    public void appendToJSONSB(StringBuilder sb, String key, String value) {
        sb.append('"');
        sb.append(key);
        sb.append("\": \"");
        sb.append(value);
        sb.append("\",");
    }
}
