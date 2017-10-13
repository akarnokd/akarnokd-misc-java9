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

/**
 * Remove dependence on java.sql.
 */
public enum JdbcType implements SqlType {

  /**
   *
   */
  ARRAY,

  /**
   *
   */
  BIGINT,

  /**
   *
   */
  BINARY,

  /**
   *
   */
  BIT,

  /**
   *
   */
  BOOLEAN,

  /**
   *
   */
  CHAR,

  /**
   *
   */
  CLOB,

  /**
   *
   */
  CURSOR,

  /**
   *
   */
  DATALINK,

  /**
   *
   */
  DATE,

  /**
   *
   */
  DECIMAL,

  /**
   *
   */
  DISTINCT,

  /**
   *
   */
  DOUBLE,

  /**
   *
   */
  FLOAT,

  /**
   *
   */
  INTEGER,

  /**
   *
   */
  JAVA_OBJECT,

  /**
   *
   */
  LONGNVARCHAR,

  /**
   *
   */
  LONGVARBINARY,

  /**
   *
   */
  LONGVARCHAR,

  /**
   *
   */
  NCHAR,

  /**
   *
   */
  NCLOB,

  /**
   *
   */
  NULL,

  /**
   *
   */
  NUMERIC,

  /**
   *
   */
  NVARCHAR,

  /**
   *
   */
  OTHER,

  /**
   *
   */
  REAL,

  /**
   *
   */
  REF,

  /**
   *
   */
  REF_CURSOR,

  /**
   *
   */
  ROWID,

  /**
   *
   */
  SMALLINT,

  /**
   *
   */
  SQLXML,

  /**
   *
   */
  STRUCT,

  /**
   *
   */
  TIME,

  /**
   *
   */
  TIME_WITH_TIME_ZONE,

  /**
   *
   */
  TIMESTAMP_WITH_TIME_ZONE,

  /**
   *
   */
  TINYINT,

  /**
   *
   */
  VARBINARY,

  /**
   *
   */
  VARCHAR;

  /**
   *
   * @return
   */
  @Override
  public String getName() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   *
   * @return
   */
  @Override
  public String getVendor() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   *
   * @return
   */
  @Override
  public Integer getVendorTypeNumber() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
