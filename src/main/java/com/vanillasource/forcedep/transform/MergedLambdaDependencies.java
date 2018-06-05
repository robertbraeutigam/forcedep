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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import org.apache.log4j.Logger;

/**
 * Merges technical methods created by lambda expressions.
 */
public final class MergedLambdaDependencies implements Dependencies {
   private static final Logger LOGGER = Logger.getLogger(MergedLambdaDependencies.class);
   private final Dependencies delegate;
   private final Map<String, CompletableFuture<Consumer<Dependencies.Method>>> lambdaMethodDependencies = new HashMap<>();

   public MergedLambdaDependencies(Dependencies delegate) {
      this.delegate = delegate;
   }

   @Override
   public void close() {
      delegate.close();
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, boolean pureInterface, String... superObjectFqns) {
      return new Dependencies.Object() {
         private final Dependencies.Object object = delegate.object(objectFqn, local, pureInterface, superObjectFqns);

         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            if (methodName.startsWith("access$")) {
               CompletableFuture<Consumer<Dependencies.Method>> methodFuture = lambdaMethodDependencies.computeIfAbsent(
                     objectFqn+"."+methodName, k -> new CompletableFuture<>());
               return method(methodFuture, methodName, local);
            } else {
               CompletableFuture<Consumer<Dependencies.Method>> methodFuture = new CompletableFuture<Consumer<Dependencies.Method>>();
               methodFuture.thenAccept(c -> {
                     LOGGER.debug("finished reading all dependencies to non-lambda method "+objectFqn+"."+methodName);
                     Dependencies.Method method = object.method(methodName, local);
                     c.accept(method);
                     method.close();
                  });
               return method(methodFuture, methodName, local);
            }
         }

         private Dependencies.Method method(CompletableFuture<Consumer<Dependencies.Method>> methodDependencies, String methodName, boolean local) {
            return new Dependencies.Method() {
               private final List<Consumer<Dependencies.Method>> dependencies = new ArrayList<>();
               private final List<CompletableFuture<Void>> externalDependencies = new ArrayList<>();

               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  if (calledMethodName.startsWith("access$")) {
                     externalDependencies.add(
                           lambdaMethodDependencies.computeIfAbsent(calledObjectFqn+"."+calledMethodName, k -> new CompletableFuture<>())
                           .thenAccept(dependencies::add));
                  } else {
                     dependencies.add(m -> m.call(calledObjectFqn, calledMethodName));
                  }
               }

               @Override
               public void reference(String referencedObjectsFqn, String referencedFieldName) {
                  dependencies.add(m -> m.reference(referencedObjectsFqn, referencedFieldName));
               }

               @Override
               public void close() {
                  LOGGER.debug("closing method "+objectFqn+"."+methodName+", had "+externalDependencies.size()+" lambda dependencies");
                  CompletableFuture.allOf(externalDependencies.toArray(new CompletableFuture[] {}))
                     .thenRun(() -> {
                        LOGGER.debug("completing "+objectFqn+"."+methodName);
                        methodDependencies.complete(method -> {
                           dependencies.forEach(c -> c.accept(method));
                        });
                     });
               }
            };
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


