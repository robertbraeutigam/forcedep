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

/**
 * Keeps only dependencies for which objects on both sides are analyzed.
 */
public final class ExistingObjectsDependencies implements Dependencies {
   private final Dependencies delegate;
   private final Map<String, CompletableFuture<Void>> objects = new HashMap<>();

   public ExistingObjectsDependencies(Dependencies delegate) {
      this.delegate = delegate;
   }

   @Override
   public void close() {
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, String... superObjectFqns) {
      return new Dependencies.Object() {
         private final Dependencies.Object object = delegate.object(objectFqn, superObjectFqns);

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            Dependencies.Method method = new Dependencies.Method() {
               private final Dependencies.Method method = object.method(methodName, local);

               @Override
               public void call(String objectsFqn, String methodName) {
                  analyzedObjectFor(objectsFqn)
                     .thenRun(() -> method.call(objectsFqn, methodName));
               }

               @Override
               public void reference(String objectsFqn, String fieldName) {
                  analyzedObjectFor(objectsFqn)
                     .thenRun(() -> method.reference(objectsFqn, fieldName));
               }

               @Override
               public void close() {
                  method.close();
               }
            };
            return method;
         }

         @Override
         public void close() {
            object.close();
            analyzedObjectFor(objectFqn).complete(null);
         }

         private CompletableFuture<Void> analyzedObjectFor(String objectFqn) {
            return objects
               .computeIfAbsent(objectFqn, k -> new CompletableFuture<>());
         }
      };
   }
}

