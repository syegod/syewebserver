package io.syewebserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class Program {

  public static void main(String[] args) throws IOException {
    final var server = ServerSocketChannel.open();
    server.bind(new InetSocketAddress(8080));
    server.configureBlocking(false);

    final var selector = Selector.open();
    server.register(selector, SelectionKey.OP_ACCEPT);

    while (true) {
      selector.select();

      var keys = selector.selectedKeys();
      var iter = keys.iterator();

      while (iter.hasNext()) {
        var key = iter.next();
        iter.remove();

        if (key.isAcceptable()) {
          final var client = server.accept();
          client.configureBlocking(false);
          client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
          final var client = (SocketChannel) key.channel();
          final var channelData = readFromChannel(client);
          if (!channelData.isBlank()) {
            final var clientRequest = parseRequest(channelData);
            System.out.println(clientRequest);
            client.register(selector, SelectionKey.OP_WRITE);
          } else {
            client.close();
          }
        } else if (key.isWritable()) {
          final var client = (SocketChannel) key.channel();
          client.write(ByteBuffer.wrap(
              ("Hello World! \n\r\r" + LocalDateTime.now().toLocalTime()).getBytes(
                  StandardCharsets.UTF_8)));
          client.close();
        }
      }
    }
  }

  static String readFromChannel(SocketChannel socket) {
    try {
      var buf = ByteBuffer.allocate(512);
      var sb = new StringBuilder();
      while (true) {
        int bytesRead = socket.read(buf);
        if (bytesRead == 0 || bytesRead == -1) {
          break;
        }
        buf.flip();
        while (buf.hasRemaining()) {
          sb.append((char) buf.get());
        }
        buf.clear();
      }
      System.out.println(sb);
      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static HttpRequest parseRequest(String rawRequest) {
    String[] lines = rawRequest.split("\r\n");
    if (lines.length == 0) {
      throw new IllegalArgumentException("Empty request");
    }

    String[] requestLine = lines[0].split(" ");
    if (requestLine.length < 2) {
      throw new IllegalArgumentException("Malformed request line: " + lines[0]);
    }

    String method = requestLine[0].trim();
    String path = requestLine[1].trim();

    HttpRequest request = new HttpRequest(path, method);

    boolean parsingHeaders = true;
    StringBuilder bodyBuilder = new StringBuilder();

    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];

      if (parsingHeaders) {
        if (line.isEmpty()) {
          parsingHeaders = false; // конец заголовков
        } else {
          int colonIndex = line.indexOf(":");
          if (colonIndex != -1) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            request.headers().put(key, value);
          }
        }
      } else {
        bodyBuilder.append(line).append("\r\n");
      }
    }

    String body = bodyBuilder.toString().trim();
    if (!body.isEmpty()) {
      request.body(body);
    }

    return request;
  }
}
