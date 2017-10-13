/*
 * Copyright (c)  2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.sql2;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a type that represents an ARRAY SQL type.
 * Any type to which this annotation is applied must either extend or implement
 * java.util.List. Additionally the type must have at least one of the following:
 * <ul>
 * <li>a public zero-arg constructor</li>
 * <li>a public constructor Constructor(int initialCapacity)</li>
 * <li>a public constructor Constructor(&lt;? super List&lt;?&gt;&gt;)</li>
 * <li>a public static factory method of(&lt;? super List&lt;?&gt;&gt;)</li>
 * </ul>
 * If more than one of the above is supported it is implementation dependent which
 * is used.
 * 
 * 
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface SqlArray {
  public String elementSqlTypeName();
}
