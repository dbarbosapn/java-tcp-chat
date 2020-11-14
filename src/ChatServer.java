import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{
    // A pre-allocated buffer for the received data
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    /* A named mapping between sequences of sixteen-bit Unicode
       code units and sequences of bytes*/

    /* Returns a charset object for the named charset.*/
    static private final Charset charset = Charset.forName("UTF8");

    /* Constructs a new decoder for this charset. */
    static private final CharsetDecoder decoder = charset.newDecoder();

    /*  A selectable channel for stream-oriented listening sockets. */
    static private ServerSocketChannel ssc;

    /**/
    static private ServerSocket ss;

    /* A multiplexor of SelectableChannel objects. */
    static private Selector selector;

    static private User user;

    static private FSM stateMachine;

    static public void main(String argv[]) throws Exception
    {
	user = new User();
	stateMachine = new FSM();

	dealWithConnection(argv);

	while( true )
	    {
		doAction();
	    }
    }

    static private void dealWithConnection(String argv[]) throws Exception
    {
	int port;

	// Read port from command line and deal with usage error
	try { port = Integer.parseInt(argv[0]); }

	catch ( Exception ex )
	    {
		System.out.print("Usage: java Server <port>\n");
		return;
	    }

	try
	    {
		ssc = ServerSocketChannel.open();

		 /* Set it to non-blocking, so we can use select, i.e
		    a non-blocking socket allows I/O operation on a channel
		    without blocking the processes using it. */
		 ssc.configureBlocking(false);

		 /* Retrieves a server socket associated with this channel.*/
		 ss = ssc.socket();

		 /* Binds the channel's socket to a local address and
		    configures the socket to listen for connections. */
		 ss.bind(new InetSocketAddress(port));

		 /* A multiplexor of SelectableChannel objects.
		    Create a new Selector for selecting. */
		 selector = Selector.open();

		 /* Registers this channel with the given selector,
		    returning a selection key.*/
		 ssc.register(selector, SelectionKey.OP_ACCEPT);
		 System.out.println( "Listening on port "+port );
	    }

	catch ( Exception ie )
	    {
		System.err.println( ie );
	    }
    }

    static private void doAction() throws Exception
    {
	/* Selects a set of keys whose corresponding channels are ready
	   for I/O operations.*/
	int num = selector.select();

	/* No activity */
	if ( num == 0 )
	    return;

	/* Returns this selector's selected-key set, i.e does ready fo IO op*/
	Set <SelectionKey> keySet = selector.selectedKeys();

	/* Returns an iterator over the elements in this set. */
	Iterator <SelectionKey> keyIt = keySet.iterator();

	while ( keyIt.hasNext() )
	    {
		/* Returns the next element in the iteration */
		SelectionKey curKey = keyIt.next();

		/* Check activity */

		/* Tests whether this key's channel is ready to accept
		   a new socket connection.*/
		if ( curKey.isAcceptable() )
		    {
			/*Listens for a connection to be made to this
			  socket and accepts it. */
			Socket s = ss.accept();
			System.out.println("Got connection from " + s);

			/* Returns the unique ServerSocketChannel object
			   associated with this socket, if any.*/
			SocketChannel sc = s.getChannel();

			/* Make it non-blocking, so we can use a selector on it*/
			sc.configureBlocking(false);

			/* Register it with the selector */
			sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			user.newUser(sc, "");
			stateMachine.newState(sc, "init");

		    }

		else if ( curKey.isReadable() )
		    {
			SocketChannel sc = null;

			try
			    {
				sc = (SocketChannel)curKey.channel();
				boolean ok = processInput(sc);

				if ( !ok )
				    {
					curKey.cancel();

					user.removeUser(sc);

					Socket s = null;

					try
					    {
						s = sc.socket();
						System.out.println
						    ("Closing connection "+s );
						s.close();
					    }

					catch ( IOException ie )
					    {
						 System.err.println
						     ("Error closing socket "+s+": "+ie );
					    }
				    }
			    }

			catch ( IOException ie )
			    {
				/* On exception, remove this channel from the selector */
				 curKey.cancel();

				 try { sc.close(); }

				 catch( IOException ie2 ) { System.out.println(ie2); }

				 System.out.println( "Closed "+sc );
			    }
		    }
	    }

	// We remove the selected keys, because we've dealt with them.
	keySet.clear();
    }

    static private boolean processInput(SocketChannel sc) throws IOException
    {
	buffer.clear();
	sc.read(buffer);
	buffer.flip();

	if ( buffer.limit() == 0 )
	    return false;

	String str = stateMachine.redirect(sc, user, decoder.decode(buffer).toString());

	sc.write(charset.encode(CharBuffer.wrap(str)));

	if ( str.matches("ERROR.*") )
	    return false;

	else
	    return true;
    }

    static private void broadCast(ByteBuffer bf) throws IOException
    {
	Iterator <SelectionKey> it = selector.keys().iterator();

	while ( it.hasNext() )
	    {
		SelectionKey curKey = it.next();

		if ( curKey.isWritable() )
		    {
			int n = ((SocketChannel)curKey.channel()).write(bf);
			bf.rewind();
		    }
	    }
    }
}
