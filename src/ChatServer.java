import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
	// A pre-allocated buffer for the received data
	static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

	/* Returns a charset object for the named charset. */
	static public final Charset charset = Charset.forName("UTF8");

	/* Constructs a new decoder for this charset. */
	static public final CharsetDecoder decoder = charset.newDecoder();

	/* A selectable channel for stream-oriented listening sockets. */
	static private ServerSocketChannel ssc;

	static private ServerSocket ss;

	/* A multiplexor of SelectableChannel objects. */
	static private Selector selector;

	static public void main(String argv[]) throws Exception {
		int port;

		// Try to read port from the command arguments
		try {
			port = Integer.parseInt(argv[0]);
		} catch (Exception ex) {
			System.out.print("Usage: java ChatServer <port>\n");
			return;
		}

		openSocket(port);

		while (true) {
			run();
		}
	}

	static private void openSocket(int port) throws Exception {
		try {
			ssc = ServerSocketChannel.open();

			/*
			 * Set it to non-blocking, so we can use select, i.e a non-blocking socket
			 * allows I/O operation on a channel without blocking the processes using it.
			 */
			ssc.configureBlocking(false);

			/* Retrieves a server socket associated with this channel. */
			ss = ssc.socket();

			/*
			 * Binds the channel's socket to a local address and configures the socket to
			 * listen for connections.
			 */
			ss.bind(new InetSocketAddress(port));

			/*
			 * A multiplexor of SelectableChannel objects. Create a new Selector for
			 * selecting.
			 */
			selector = Selector.open();

			/*
			 * Registers this channel with the given selector, returning a selection key.
			 */
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Listening on port " + port);
		} catch (Exception ie) {
			System.err.println(ie);
		}
	}

	static private void run() throws Exception {
		/*
		 * Selects a set of keys whose corresponding channels are ready for I/O
		 * operations.
		 */
		int num = selector.select();

		/* No activity */
		if (num == 0)
			return;

		/* Returns this selector's selected-key set, i.e does ready fo IO op */
		Set<SelectionKey> keySet = selector.selectedKeys();

		/* Returns an iterator over the elements in this set. */
		Iterator<SelectionKey> keyIt = keySet.iterator();

		while (keyIt.hasNext()) {
			/* Returns the next element in the iteration */
			SelectionKey curKey = keyIt.next();

			/* Check activity */

			/*
			 * Tests whether this key's channel is ready to accept a new socket connection.
			 */
			if (curKey.isAcceptable()) {
				/*
				 * Listens for a connection to be made to this socket and accepts it.
				 */
				Socket s = ss.accept();
				System.out.println("Got connection from " + s);

				/*
				 * Returns the unique ServerSocketChannel object associated with this socket, if
				 * any.
				 */
				SocketChannel sc = s.getChannel();

				/* Make it non-blocking, so we can use a selector on it */
				sc.configureBlocking(false);

				/* Register it with the selector */
				sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

				/* Create a new user */
				User.create(sc);
			} else if (curKey.isReadable()) {
				SocketChannel sc = (SocketChannel) curKey.channel();

				try {
					String socketInput = getSocketInput(sc);

					if (socketInput == null) {
						User u = User.getByChannel(sc);
						if (u != null)
							u.delete();

						curKey.cancel();

						Socket s = null;

						try {
							s = sc.socket();
							System.out.println("Closing connection " + s);
							s.close();
						} catch (IOException ie) {
							System.err.println("Error closing socket " + s + ": " + ie);
						}
					} else {
						Protocol.processInput(socketInput, sc);
					}
				} catch (IOException ie) {
					User u = User.getByChannel(sc);
					if (u != null)
						u.delete();

					curKey.cancel();

					try {
						sc.close();
					} catch (IOException ie2) {
						System.out.println(ie2);
					}

					System.out.println("Closed " + sc);
				}
			}
		}

		// We remove the selected keys, because we've dealt with them.
		keySet.clear();
	}

	static private String getSocketInput(SocketChannel sc) throws IOException {
		buffer.clear();
		sc.read(buffer);
		buffer.flip();

		if (buffer.limit() == 0)
			return null;

		return decoder.decode(buffer).toString();
	}
}
