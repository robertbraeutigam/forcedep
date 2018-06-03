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
import java.util.Map;
import java.util.HashMap;

/**
 * Keeps only unique external dependencies for each method.
 */
public final class UniqueDependencies implements Dependencies {
   private final Dependencies delegate;

   public UniqueDependencies(Dependencies delegate) {
      this.delegate = delegate;
   }

   @Override
   public void close() {
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, String... superObjectFqns) {
      return new Dependencies.Object() {
         private final Dependencies.Object object = delegate.object(objectFqn, local, superObjectFqns);

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            Dependencies.Method method = new Dependencies.Method() {
               private final Dependencies.Method method = object.method(methodName, local);
               // I'm using Map here because Set does not have a computeIfAbsent() for some reason. It uses
               // an empty String as value, because we can not use Void with null.
               private final Map<String, String> methodCalls = new HashMap<>();
               private final Map<String, String> fieldReferences = new HashMap<>();

               @Override
               public void call(String objectsFqn, String methodName) {
                  methodCalls.computeIfAbsent(objectsFqn+"."+methodName, k -> {
                     method.call(objectsFqn, methodName);
                     return "";
                  });
               }

               @Override
               public void reference(String objectsFqn, String fieldName) {
                  fieldReferences.computeIfAbsent(objectsFqn+"."+fieldName, k -> {
                     method.reference(objectsFqn, fieldName);
                     return "";
                  });
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


