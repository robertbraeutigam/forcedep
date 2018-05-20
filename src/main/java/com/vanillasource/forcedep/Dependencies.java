/**
 * Copyright (C) 2018 VanillaSource
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.vanillasource.forcedep;

/**
 * Dependencies between a number of objects.
 */
public interface Dependencies extends AutoCloseable {
   Object object(String objectFqn, String... superObjectFqns);

   @Override
   void close();

   interface Object extends AutoCloseable {
      Method method(String methodName, boolean local);

      @Override
      void close();
   }

   interface Method extends AutoCloseable {
      void call(String objectsFqn, String methodName);

      void reference(String objectsFqn, String fieldName);

      @Override
      void close();
   }
}
