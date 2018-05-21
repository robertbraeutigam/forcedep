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
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Merges calls from private methods to the public methods they are called by.
 */
public final class MergedPrivateMethodsDependencies implements Dependencies {
   private final Dependencies delegate;

   public MergedPrivateMethodsDependencies(Dependencies delegate) {
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
         private final Map<String, List<Consumer<Dependencies.Method>>> externalDependencies = new HashMap<>();
         private final Map<String, List<String>> localCalls = new HashMap<>();
         private final Set<String> localMethods = new HashSet<>();

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            if (local) {
               localMethods.add(methodName);
            }
            List<Consumer<Dependencies.Method>> methodExternalDependencies = new ArrayList<>();
            externalDependencies.put(methodName, methodExternalDependencies);
            return new Dependencies.Method() {
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  if (calledObjectFqn.equals(objectFqn)) {
                     localCalls.computeIfAbsent(methodName, k -> new ArrayList<>()).add(calledMethodName);
                  } else {
                     methodExternalDependencies.add(m -> m.call(calledObjectFqn, calledMethodName));
                  }
               }

               @Override
               public void reference(String referencedObjectsFqn, String referencedFieldName) {
                  methodExternalDependencies.add(m -> m.reference(referencedObjectsFqn, referencedFieldName));
               }

               @Override
               public void close() {
               }
            };
         }

         @Override
         public void close() {
            for (String methodName: externalDependencies.keySet()) {
               if (!localMethods.contains(methodName)) {
                  try (Dependencies.Method delegateMethod = object.method(methodName, false)) {
                     applyDependencies(methodName, delegateMethod);
                  }
               }
            }
            object.close();
         }

         private void applyDependencies(String methodName, Dependencies.Method delegateMethod) {
            externalDependencies
               .computeIfAbsent(methodName, k -> new ArrayList<>())
               .forEach(d -> d.accept(delegateMethod));
            localCalls
               .computeIfAbsent(methodName, k -> new ArrayList<>())
               .stream()
               .filter(m -> !localMethods.contains(m))
               .forEach(m -> delegateMethod.call(objectFqn, m));
            localCalls
               .computeIfAbsent(methodName, k -> new ArrayList<>())
               .forEach(l -> applyDependencies(l, delegateMethod));
         }
      };
   }
}


