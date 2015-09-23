/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import TMARKERPluginInterface.Pluggable;
import java.security.Permission;

/**
 * This Security Manager is very liberal and allows almost everything. Some methods 
 * (e.g. exiting the program) are not allowed for plugins, but only for the main TMARKER
 * program.
 * @author Peter J. Schueffler
 */
public class TMARKERSecurityManager extends java.lang.SecurityManager {
    
    /**
     * This is the basic method that tests whether there is a class loaded
     * by a ClassLoader anywhere on the stack. If so, it means that that
     * untrusted code is trying to perform some kind of sensitive operation.
     * We prevent it from performing that operation by throwing an exception.
     * trusted() is called by most of the check...() methods below.
     */
    protected void trusted() {
        //if (inClassLoader()) throw new SecurityException();
    }
    
    /**
     * This is the method that tests whether a method was called by a Plugin. If so, 
     * a security exception is thrown such that the process cannot continue. If the
     * call came from the main TMARKER program, it is trusted.
     */
    protected void trusted_notForPlugins() {
        boolean isPluggable = false;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement elem: stackTraceElements) {
            try {
                isPluggable |= Thread.currentThread().getContextClassLoader().loadClass(elem.getClassName()).newInstance() instanceof Pluggable;
                //isPluggable |= Class.forName(elem.getClassName()).newInstance() instanceof Pluggable;
            } catch (ClassNotFoundException ex) {
                //Logger.getLogger(TMARKERSecurityManager.class.getName()).log(Level.SEVERE, null, ex);
                isPluggable = true; // happens only for Plugins
            } catch (Exception ex) {
                //Logger.getLogger(TMARKERSecurityManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        if (isPluggable) throw new SecurityException();
    }
    
    /**
	 * These are all the specific checks that a security manager can
	 * perform. They all just call one of the methods above and throw a
	 * SecurityException if the operation is not allowed. This 
	 * SecurityManager subclass is perhaps a little too liberal.
	 */
    	public void checkCreateClassLoader() { trusted(); }
        public void checkAccess (Thread g) { trusted(); }
	public void checkAccess (ThreadGroup g) { trusted(); }
	public void checkExit (int status) { trusted_notForPlugins(); } // DONT LET THE PLUGIN CLOSE TMARKER (especially the closing operation of JFrame Plugins should be "hide")!!
	public void checkExec (String cmd) { trusted(); }
	public void checkLink (String lib) { trusted(); }
	public void checkRead (java.io.FileDescriptor fd) { trusted(); }
	public void checkRead (String file) {
//		String path = new File(file).getParentFile().getAbsolutePath();
//		if (! path.endsWith(pluginDir))
			trusted();
	}
	public void checkRead (String file, Object context) { trusted(); }
	public void checkWrite (java.io.FileDescriptor fd) { trusted(); }
	public void checkWrite (String file) { trusted(); }
	public void checkDelete (String file) { trusted(); }
	public void checkConnect (String host, int port) { trusted(); }
	public void checkConnect (String host,int port,Object context) {trusted();}
	public void checkListen (int port) { trusted(); }
	public void checkAccept (String host, int port) { trusted(); }
	public void checkMulticast (java.net.InetAddress maddr) { trusted(); }
	public void checkMulticast (java.net.InetAddress maddr, byte ttl) { trusted(); }
	public void checkPropertiesAccess() { trusted(); }
	public void checkPropertyAccess (String key) {
//		if (! key.equals("user.dir"))
			trusted();
	}
	public void checkPrintJobAccess() { trusted(); }
	public void checkSystemClipboardAccess() { trusted(); }
	public void checkAwtEventQueueAccess() { trusted(); }
	public void checkSetFactory() { trusted(); }
	public void checkMemberAccess (Class clazz, int which) { trusted(); }
	public void checkSecurityAccess (String provider) { trusted(); }

	/** Loaded code can only load classes from java.* packages */
	public void checkPackageAccess (String pkg) { 
		//if (inClassLoader() && !pkg.startsWith("java.") && !pkg.startsWith("javax."))
		//	throw new SecurityException();
	}

	/** Loaded code can't define classes in java.* or sun.* packages */
	public void checkPackageDefinition (String pkg) { 
		//if (inClassLoader() && ((pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("sun."))))
		//	throw new SecurityException();
	}
        
        public void checkPermission (Permission Perm, Object context) {trusted(); }
        public void checkPermission (Permission Perm) {trusted(); }
        
	/** 
	 * This is the one SecurityManager method that is different from the
	 * others. It indicates whether a top-level window should display an
	 * "untrusted" warning. The window is always allowed to be created, so
	 * this method is not normally meant to throw an exception. It should
	 * return true if the window does not need to display the warning, and
	 * false if it does. In this example, however, our text-based Service
	 * classes should never need to create windows, so we will actually
	 * throw an exception to prevent any windows from being opened.
	 **/
        public boolean checkTopLevelWindow (Object window) { 
		trusted();
		return true; 
	}
}
