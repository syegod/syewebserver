package io.syewebserver;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class HttpRequest {

  private final String url;
  private final String method;
  private final Map<String, String> headers;
  private String body;

  public HttpRequest(String url, String method) {
    this.url = url;
    this.method = method;
    this.headers = new HashMap<>();
  }

  public String url() {
    return url;
  }

  public String method() {
    return method;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public String body() {
    return body;
  }

  public HttpRequest body(String body) {
    this.body = body;
    return this;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", HttpRequest.class.getSimpleName() + "[", "]")
        .add("url='" + url + "'")
        .add("method='" + method + "'")
        .add("headers=" + headers)
        .add("body='" + body + "'")
        .toString();
  }
}
