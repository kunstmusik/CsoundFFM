/*
    MessageCallback.java:

    Copyright (C) 2024 Steven Yi 

    This file is part of CsoundFFM.

    The CsoundFFM Library is free software; you can redistribute it
    and/or modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    CsoundFFM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with CsoundJNI; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA
 */
package com.kunstmusik.csoundffm;

import java.lang.foreign.MemorySegment;

/**
 * Single Abstract Method interface for message callbacks to be used with
 * Csound class.
 * 
 * <pre>
 * MessageCallback cb = (cs, attr, msg) -&gt; {
 *     var msgText = msg.reinterpret(Integer.MAX_VALUE).getString(0);
 *     System.out.print(msgText);
 * };
 * csound.setMessageCallback(cb);
 * </pre>
 * 
 * @author Steven Yi
 */
public interface MessageCallback {
    public void callback(MemorySegment csound, int attr, MemorySegment msg);
}
