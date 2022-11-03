/*
Copyright (c) 2007 Thomas Boerkel, All Rights Reserved

Disclaimer:
===========
This code is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This code is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
 */

package hudson.util.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author TB
 */
public interface WINBASE {
/*
typedef struct _SECURITY_ATTRIBUTES {
  DWORD nLength;
  LPVOID lpSecurityDescriptor;
  BOOL bInheritHandle;
} SECURITY_ATTRIBUTES,
 *PSECURITY_ATTRIBUTES,
 *LPSECURITY_ATTRIBUTES;*/
  @SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", justification = "JNA Data Structure")
  class SECURITY_ATTRIBUTES extends Structure {
    public int nLength;
    public Pointer lpSecurityDescriptor;
    public boolean bInheritHandle;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList("nLength", "lpSecurityDescriptor",
                "bInheritHandle");
    }
  }

/*
typedef struct _FILETIME {
    DWORD dwLowDateTime;
    DWORD dwHighDateTime;
} FILETIME, *PFILETIME, *LPFILETIME;*/
  @SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", justification = "JNA Data Structure")
  class FILETIME extends Structure {
    public int dwLowDateTime;
    public int dwHighDateTime;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList("dwLowDateTime", "dwHighDateTime");
    }
  }
}
