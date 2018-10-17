  The problem with our F5 load balancer is that it is currently TCP based. If WLS crashes but still returns TCP ACKs our
  cluster member is not removed from the load balancer server list and generates lots of errors.

  We can point the load balancer to an arbitrary page, but that does not give us application owners control over when
  to LB or not to LB. Also, this could generate load.

  This page offers a solution for these problems.

  - In operational mode: return a friendly (light weight) page with an OKAY message.
  - If a "disable" message is send in the URL a temporary file is created in the domain directory. If this file exists, the
    page will always return a HTTP 503 SERVICE UNAVAILABLE with a custom error message. There's an optional list of HTTP checks to perform to prevent us from disabling all cluster nodes.
  - If an "enable" message is send, the file is removed and the OKAY page is displayed again
  - If the server is halting, as any WLS app this page returns a HTTP 500 error code.

  This way, we can tell the load balancer an individual server is not available by calling the URL or placing the temp
  file ourselves.