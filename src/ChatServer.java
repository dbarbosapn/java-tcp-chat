import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
	/* Returns a charset object for the named charset. */
	static public final Charset charset = Charset.forName("UTF8");

	/* Constructs a new decoder for this charset. */
	static public final CharsetDecoder decoder = charset.newDecoder();

	/* A selectable channel for stream-oriented listening sockets. */
	static private ServerSocketChannel ssc;

	static private ServerSocket ss;

	/* A multiplexor of SelectableChannel objects. */
	static private Selector selector;

        private enum BufferState {WAIT, READY, ABORT};


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
					BufferState bufferState = getSocketInput(sc);

					if ( bufferState == BufferState.ABORT ) {
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
					}

					else if ( bufferState == BufferState.READY ) {
					    ByteBuffer buffer = User.getBufferByChannel(sc);
					    String socketInput = decoder.decode(buffer).toString();

					    /* Count the number of commands/messages in the buffer,
					       i.e the number o \n*/
					    int count = 0;

					    for( int i=0; i < socketInput.length(); i++ )
						if ( socketInput.charAt(i) == '\n' )
						    count++;

					    /* Get the various tokens with \n as delimiter*/
					    String[] token =  socketInput.split("\n");

					    /* For each \n(aka command or message) we do something*/
					    int i;
					    for( i=0; i<count; i++ )
						Protocol.processInput(token[i], sc);

					    /* Condition test if after we parsed the buffer there is
					       still something there that didn't end with new line*/
					    if ( i > count )
						{
						    /* Atention to this statement the correct position
						     WILL change with the different types of encoding
						     we use!!!*/
						    buffer.position(buffer.limit()-token[i].length());

						    buffer.compact();
						}

					    /* Means that the last byte/bytes were the end of line if so
					       we can simply clear it*/
					    else
					        buffer.clear();

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

	static private BufferState getSocketInput(SocketChannel sc) throws IOException {
	    ByteBuffer buffer = User.getBufferByChannel(sc);

	    /* Nothing was read but getSOcketInput was still called
	       meaning user must have closed the connection*/
	    if ( sc.read(buffer) <= 0 )
		return BufferState.ABORT;

	    /* Save buffer curent possition*/
	    int prevPos = buffer.position();

	    /* Put buffer in a configuration to be possible to read it.
	       We cannot simply check last byte in buffer */
	    buffer.flip();
	    String input = decoder.decode(buffer).toString();

	    /* Revert back to the previous configuration*/
	    buffer.position(prevPos);
	    buffer.limit(buffer.capacity());

	    if ( input.contains("\n") )
		{
		    buffer.flip();
		    return BufferState.READY;
		}

	    else
		return BufferState.WAIT;

	}
}
