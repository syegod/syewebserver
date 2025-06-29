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
    var server = ServerSocketChannel.open();
    server.bind(new InetSocketAddress(8080));
    server.configureBlocking(false);

    var selector = Selector.open();
    server.register(selector, SelectionKey.OP_ACCEPT);

    while (true) {
      selector.select();

      var keys = selector.selectedKeys();
      var iter = keys.iterator();

      while (iter.hasNext()) {
        var key = iter.next();
        iter.remove();

        if (key.isAcceptable()) {
          var client = server.accept();
          client.configureBlocking(false);
          client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
          var client = (SocketChannel) key.channel();
          final var channelData = readFromChannel(client);
          if (!channelData.isBlank()) {
            final var clientRequest = parseRequest(channelData);
            System.out.println(clientRequest);
            client.register(selector, SelectionKey.OP_WRITE);
          } else {
            client.close();
          }
        } else if (key.isWritable()) {
          var client = (SocketChannel) key.channel();
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
      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static HttpRequest parseRequest(String req) {
    var rows = req.split("\r\n");
    var firstRow = rows[0].split(" ");
    var request = new HttpRequest(firstRow[1], firstRow[0]);
    for (int i = 1; i < rows.length; i++) {
      if (rows[i].contains(":")) {
        var split = rows[i].split(": ");
        request.headers().put(split[0], split[1]);
      }
    }
    return request;
  }
}
