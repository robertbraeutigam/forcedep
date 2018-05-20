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
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Simulates a <code>super</code> call for each overridden method. For interfaces too, where
 * the call will go to the originating interface.
 */
public final class OverrideDependencies implements Dependencies {
   private final Dependencies delegate;
   private final Map<String, CompletableFuture<AnalyzedObject>> objects = new HashMap<>();

   public OverrideDependencies(Dependencies delegate) {
      this.delegate = delegate;
   }

   @Override
   public void close() {
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, String... superObjectFqns) {
      return new AnalyzedObject() {
         private final Dependencies.Object object = delegate.object(objectFqn, superObjectFqns);
         private final Set<String> methods = new HashSet<>();

         @Override
         public Dependencies.Method method(String methodName) {
            methods.add(methodName);
            Dependencies.Method method = new Dependencies.Method() {
               private final Dependencies.Method method = object.method(methodName);

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
            findOverridesFor(method, methodName);
            return method;
         }

         @Override
         public void potentiallyOverriddenBy(Dependencies.Method method, String methodName) {
            if (methods.contains(methodName)) {
               // Place call from overriding method to this overridden method
               method.call(objectFqn, methodName);
            } else {
               // Try superclass
               findOverridesFor(method, methodName);
            }
         }

         @Override
         public void close() {
            object.close();
            analyzedObjectFor(objectFqn).complete(this);
         }

         private void findOverridesFor(Dependencies.Method method, String methodName) {
            for (String superObjectFqn: superObjectFqns) {
               analyzedObjectFor(superObjectFqn)
                  .thenAccept(obj -> obj.potentiallyOverriddenBy(method, methodName));
            }
         }

         private CompletableFuture<AnalyzedObject> analyzedObjectFor(String objectFqn) {
            return objects
               .computeIfAbsent(objectFqn, k -> new CompletableFuture<>());
         }
      };
   }

   interface AnalyzedObject extends Dependencies.Object {
      void potentiallyOverriddenBy(Dependencies.Method method, String methodName);
   }
}

