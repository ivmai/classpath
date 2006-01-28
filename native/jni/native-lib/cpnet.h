/* cpnet.h -
   Copyright (C) 2003, 2004, 2005, 2006  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

#ifndef _CLASSPATH_NET_H_INCLUDED
#define _CLASSPATH_NET_H_INCLUDED

#include <jni.h>
#include <jcl.h>

typedef struct {
  jint len;
  char data[1];
} cpnet_address;

JNIEXPORT jint cpnet_openSocketStream(jint *fd);
JNIEXPORT jint cpnet_openSocketDatagram(jint *fd);
JNIEXPORT jint cpnet_close(jint fd);
JNIEXPORT jint cpnet_connect(jint fd, cpnet_address *addr);
JNIEXPORT jint cpnet_getLocalAddr(jint fd, cpnet_address **addr);

static inline cpnet_address *cpnet_newIPV4Address(JNI_Env * env)
{
  cpnet_address *addr = (cpnet_address *)JCL_malloc(env, sizeof(cpnet_address) + sizeof(struct sockaddr_in));
  addr->len = sizeof(struct sockaddr_in);
}

static inline void cpnet_freeAddress(JNI_Env * env, cpnet_address *addr)
{
  JCL_free(env, addr);
}

static inline void cpnet_IPV4AddressToBytes(cpnet_address *netaddr, unsigned char *octets)
{
  struct sockaddr_in *ipaddr = (struct sockaddr_in *)netaddr;
  jint sysaddr = ipaddr->sin_addr.s_addr;

  octets[0] = (sysaddr >> 24) & 0xff;
  octets[1] = (sysaddr >> 16) & 0xff;
  octets[2] = (sysaddr >> 8) & 0xff;
  octets[3] = sysaddr & 0xff;
}

static inline void cpnet_bytesToIPV4Address(cpnet_address *netaddr, unsigned char *octets)
{
  jint sysaddr;
  struct sockaddr_in *ipaddr = (struct sockaddr_in *)netaddr;

  sysaddr = ((jint)octets[0]) << 24;
  sysaddr |= ((jint)octets[1]) << 16;
  sysaddr |= ((jint)octets[2]) << 8;
  sysaddr |= ((jint)octets[3]);

  ipaddr->sin_addr.s_addr = sysaddr;
  
  return netaddr;
}

#endif
