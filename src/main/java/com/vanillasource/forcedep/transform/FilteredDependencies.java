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

package com.vanillasource.forcedep.transform;

import com.vanillasource.forcedep.Dependencies;
import java.util.List;

/**
 * Filters based on whitelist and backlist all the objects.
 */
public final class FilteredDependencies implements Dependencies {
   private final Dependencies delegate;
   private final List<String> whitelist;
   private final List<String> blacklist;

   public FilteredDependencies(List<String> whitelist, List<String> blacklist, Dependencies delegate) {
      this.whitelist = whitelist;
      this.blacklist = blacklist;
      this.delegate = delegate;
   }

   @Override
   public void close() {
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, boolean pureInterface, String... superObjectFqns) {
      if (onWhitelist(objectFqn) && !onBlacklist(objectFqn)) {
         return delegatingObject(objectFqn, local, pureInterface, superObjectFqns);
      } else {
         return NULL_OBJECT;
      }
   }

   private boolean onWhitelist(String objectFqn) {
      return onList(objectFqn, whitelist);
   }

   private boolean onBlacklist(String objectFqn) {
      return onList(objectFqn, blacklist);
   }

   private boolean onList(String objectFqn, List<String> list) {
      return list.stream()
         .anyMatch(r -> objectFqn.matches(r));
   }

   private Dependencies.Object delegatingObject(String objectFqn, boolean local, boolean pureInterface, String... superObjectFqns) {
      return new Dependencies.Object() {
         private final Dependencies.Object object = delegate.object(objectFqn, local, pureInterface, superObjectFqns);

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            Dependencies.Method method = new Dependencies.Method() {
               private final Dependencies.Method method = object.method(methodName, local);

               @Override
               public void call(String objectsFqn, String methodName) {
                  method.call(objectsFqn, methodName);
               }

               @Override
               public void reference(String objectsFqn, String fieldName) {
                  method.reference(objectsFqn, fieldName);
               }

               @Override
               public void close() {
                  method.close();
               }
            };
            return method;
         }

         @Override
         public void field(String fieldName) {
            object.field(fieldName);
         }

         @Override
         public void close() {
            object.close();
         }
      };
   }
}


