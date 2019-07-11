  The problem with our F5 load balancer is that it is currently TCP based. If WebLogic crashes but still returns TCP ACKs our
  cluster member is not removed from the load balancer server list and generates lots of errors.

  We can point the load balancer to an arbitrary page, but that does not give us application owners control over when
  to LB or not to LB. Also, this could generate load or sessions. 

  This small app offers a solution for these problems.

  - In operational mode: return a friendly (light weight) page with an OKAY message.
  - If "disabled" in the gui or using the URL the load balancer check URL will always return a HTTP 503 SERVICE UNAVAILABLE with a custom error message. 
  - If an "enable" message is send, the file is removed and the OKAY page is displayed again
  
  This way, we can tell the load balancer an individual server is not available by calling the URL or placing the temp
  file ourselves. Just deploy the dist/zzwlshealth.war it to your WebLogic server and target the deployment to any managed server.  
  
  ![Alt text](/stscreenshots/screenshot.png?raw=true "App screenshot")
  
   Relevant URLs:
  
  - <server url>/zzwlshealth/health - the GUI page, showing the status of all servers
  - <server url>/zzwlshealth/health/disable - tell the load balancer URL to return a HTTP 503
  - <server url>/zzwlshealth/health/enable - tell the load balancer URL to return a HTTP 200
  - <server url>/zzwlshealth/health/lb - the URL you will configure in your load balancer health check
  - <server url>/zzwlshealth/health/info - a backend info page returning all info in JSON format
  
  There's probably a thousand things I could add, feel free to create an issue. 