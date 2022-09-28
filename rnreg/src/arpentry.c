/* $CwuRes: src/regi/www/webapps/rnreg/src/arpentry.c,v 1.1 2003/02/20 00:37:34 hartd Exp $
 *
 *
 *
 *
 *
 */

#include <sys/types.h>
#include <sys/socket.h>
#ifdef linux
#include <linux/sockios.h>
#include <string.h>
#include <sys/ioctl.h>
#else
#include <sys/sockio.h>
#include <strings.h>
#include <unistd.h>
#include <stropts.h>
#endif
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <net/if_arp.h>
#include <stdlib.h>

#include <jni.h>
#include "ArpCache.h"

char *getmac(const char *addr);

/* int main(int argc, char *argv[]) { */

/*   char *mac; */

/*   if (argc !=2) { */
/*     printf("usage: test address\n"); */
/*     exit(-1); */
/*   } */

/*   if ((mac = getmac(argv[1])) != NULL) */
/*     printf("Ethernet address for %s: %s\n", argv[1], mac); */
/*   else */
/*     printf("%s: entry not found\n", argv[1]); */

/*   exit(0); */
  
/* } */


JNIEXPORT jstring JNICALL Java_ArpCache_getmac
(JNIEnv *env, jclass me, jstring addr) {

  const char* addrStr = (*env)->GetStringUTFChars(env, addr, 0);

  char* result = getmac(addrStr);

  jstring res = (*env)->NewStringUTF(env, result);
  return res;

}
 

char *getmac(const char *addr) {

  struct hostent *host;
  int sock;
  struct sockaddr_in *sin;
  struct arpreq arpreq;
  unsigned char *ha;
  char *hex_ha;

  if ((host = gethostbyname(addr)) == NULL)
    return NULL;

  sock = socket(AF_INET, SOCK_DGRAM, 0);
  sin = (struct sockaddr_in *) &arpreq.arp_pa;
  bzero(sin, sizeof(struct sockaddr_in));
  sin->sin_family = AF_INET;
  // need to retrieve IF list and cycle through it
  #ifdef linux
  strcpy(arpreq.arp_dev, "eth1");
  #endif
  memcpy(&sin->sin_addr, *host->h_addr_list, sizeof(struct in_addr));
  if (ioctl(sock, SIOCGARP, &arpreq) < 0)
    return NULL;

  ha = &arpreq.arp_ha.sa_data[0];
  if ((hex_ha = (char*)calloc(13, sizeof(char))) == NULL)
    return NULL;
  sprintf(hex_ha, "%02x%02x%02x%02x%02x%02x", *ha, \
	  *(ha + 1), *(ha + 2), *(ha + 3), *(ha + 4), *(ha + 5));

  return hex_ha;

}

