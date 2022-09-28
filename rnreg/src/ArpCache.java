/* $CwuRes: src/regi/www/webapps/rnreg/src/ArpCache.java,v 1.1 2003/02/20 00:37:34 hartd Exp $
 *
 * wrapper for native method to retrieve MAC address from 
 * arp cache
 *
 */


public class ArpCache {

  static {
    System.loadLibrary("jarp");
  }

  public static native String getmac(String addr);

}
