import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.Object.*;

public class User
{
    private HashSet <String> nameMap;
    private HashMap <SocketChannel, String> userMap;

    public User()
    {
	nameMap = new HashSet <String> ();
	userMap = new HashMap <SocketChannel, String> ();
    }

    protected boolean newUser(SocketChannel sc, String name)
    {
	if ( !userMap.containsKey(sc) )
	    {
		if ( !nameMap.contains(name) )
		    {
			nameMap.add(name);
			userMap.put(sc, name);
			return true;
		    }

		else
		    return false;
	    }

	else if ( !nameMap.contains(name) )
	    {
		nameMap.add(name);
		userMap.replace(sc, name);
		return true;
	    }

	else
	    return false;
    }

    private boolean removeName(String name) { return nameMap.remove(name); }

    protected void removeUser(SocketChannel sc)
    {
	removeName(userMap.get(sc));

	userMap.remove(sc);
    }
}
