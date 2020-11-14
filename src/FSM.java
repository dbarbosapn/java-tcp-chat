import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.Object.*;

/* Define protocols here!*/

public class FSM
{
    private HashMap <SocketChannel, String> stateMap;

    public FSM()
    {
	stateMap = new HashMap <SocketChannel, String> ();
    }

    protected void newState(SocketChannel sc, String state)
    {
	if ( !stateMap.containsKey(sc) )
	    stateMap.put(sc, state);

	else
	    stateMap.replace(sc, state);
    }

    protected void removeState(SocketChannel sc) { stateMap.remove(sc); }

    protected String redirect(SocketChannel sc, User user, String str)
    {
	switch( stateMap.get(sc) )
	    {
	    case "init":
		return initState(sc, user, str);

	    case "inside":
		return insideState(sc, user, str);

	    case "outside":
		return outsideState(sc, user, str);

	    default:
		return "SOME ERROR 1\n";
	    }
    }

    private String initState(SocketChannel sc, User user, String str)
    {
	System.out.println(str);

	if ( str.matches("/nick .*\n") )
	    return changeName(sc, user, str.replaceFirst("/nick ", ""));

	else
	    return "SOME ERROR 2\n";
    }

    private String changeName(SocketChannel sc, User user, String str)
    {
	if ( user.newUser(sc, str) )
	    {
		newState(sc, "inside");
		return "OK";
	    }

	else
	    return "ERROR";
    }

    private String insideState(SocketChannel sc, User user, String str)
    {

	return "";
    }

    private String outsideState(SocketChannel sc, User user, String str)
    {

	return "";
    }
}
