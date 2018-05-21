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
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Merges calls from anonymous inner classes to the method using the class.
 */
public final class MergedAnonymousClassesDependencies implements Dependencies {
   private final Dependencies delegate;
   private final Map<String, List<Consumer<Dependencies.Method>>> localObjects = new HashMap<>();
   private final Map<String, List<String>> localObjectInstantiations = new HashMap<>();
   private final List<Runnable> topObjects = new ArrayList<>();

   public MergedAnonymousClassesDependencies(Dependencies delegate) {
      this.delegate = delegate;
   }

   @Override
   public void close() {
      topObjects.forEach(Runnable::run);
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, String... superObjectFqns) {
      if (local) {
         return localObject(objectFqn, superObjectFqns);
      } else {
         return topObject(objectFqn, superObjectFqns);
      }
   }

   /**
    * All method calls for local objects will be merged to the instantiation method.
    */
   private Dependencies.Object localObject(String objectFqn, String... superObjectFqns) {
      List<Consumer<Dependencies.Method>> localObjectDependencies =
         localObjects.computeIfAbsent(objectFqn, k -> new ArrayList<>());
      return new Dependencies.Object() {
         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            Dependencies.Method method = new Dependencies.Method() {
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  if (calledMethodName.equals("<init>")) {
                     localObjectInstantiations.computeIfAbsent(objectFqn, k -> new ArrayList<>()).add(calledObjectFqn);
                  } else {
                     localObjectDependencies.add(m -> {
                        if (!localObjects.containsKey(calledObjectFqn)) {
                           m.call(calledObjectFqn, calledMethodName);
                        }
                     });
                  }
               }

               @Override
               public void reference(String referencedObjectsFqn, String referencedFieldName) {
                  localObjectDependencies.add(m -> m.reference(referencedObjectsFqn, referencedFieldName));
               }

               @Override
               public void close() {
               }
            };
            return method;
         }

         @Override
         public void close() {
         }
      };
   }

   private Dependencies.Object topObject(String objectFqn, String... superObjectFqns) {
      return new Dependencies.Object() {
         private final List<Consumer<Dependencies.Object>> methods = new ArrayList<>();

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            Dependencies.Method method = new Dependencies.Method() {
               private final List<Consumer<Dependencies.Method>> dependencies = new ArrayList<>();
               private final List<String> instantiations = new ArrayList<>();
                  
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  if (calledMethodName.equals("<init>")) {
                     instantiations.add(calledObjectFqn);
                  } else {
                     dependencies.add(m -> {
                        if (!localObjects.containsKey(calledObjectFqn)) {
                           m.call(calledObjectFqn, calledMethodName);
                        }
                     });
                  }
               }

               @Override
               public void reference(String referencedObjectsFqn, String referencedFieldName) {
                  dependencies.add(m -> m.reference(referencedObjectsFqn, referencedFieldName));
               }

               @Override
               public void close() {
                  methods.add(object -> {
                     try (Dependencies.Method m = object.method(methodName, local)) {
                        dependencies.forEach(d -> d.accept(m));
                        instantiations
                           .stream()
                           .filter(o -> !localObjects.containsKey(o))
                           .forEach(o -> m.call(o, "<init>"));
                        instantiations
                           .stream()
                           .filter(o -> localObjects.containsKey(o))
                           .forEach(o -> merge(o, m));
                     }
                  });
               }

               private void merge(String instantiatedObjectFqn, Dependencies.Method owner) {
                  localObjects
                     .computeIfAbsent(instantiatedObjectFqn, k -> new ArrayList<>())
                     .forEach(c -> c.accept(owner));
                  localObjectInstantiations
                     .computeIfAbsent(instantiatedObjectFqn, k -> new ArrayList<>())
                     .forEach(o -> merge(o, owner));
               }
            };
            return method;
         }

         @Override
         public void close() {
            topObjects.add(() -> {
               try (Dependencies.Object object = delegate.object(objectFqn, false, superObjectFqns)) {
                  methods.forEach(m -> m.accept(object));
               }
            });
         }
      };
   }
}

