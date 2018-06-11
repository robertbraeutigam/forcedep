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

package com.vanillasource.forcedep.d3;

import com.vanillasource.forcedep.Dependencies;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.jtwig.JtwigTemplate;
import org.jtwig.JtwigModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

/**
 * Saves dependencies directly to a D3 graph.
 */
public final class D3Dependencies implements Dependencies {
   private final Map<String, CompletableFuture<Boolean>> objectInterface = new HashMap<>();
   private final JsonArray nodes = new JsonArray();
   private final JsonArray links = new JsonArray();
   private final String analysisName;
   private final File outputFile;
   private final boolean active;
   private final ObjectLayout layout;
   private final int size;
   private int classCount = 0;
   private int methodCount = 0;
   private int fieldCount = 0;

   public D3Dependencies(String analysisName, File outputFile, boolean active, int size) {
      this.analysisName = analysisName;
      this.outputFile = outputFile;
      this.active = active;
      this.size = size;
      this.layout = new ObjectLayout(size);
   }

   @Override
   public void close() {
      try {
         try (FileOutputStream output = new FileOutputStream(outputFile)) {
            JtwigTemplate template = JtwigTemplate.classpathTemplate("/com/vanillasource/forcedep/d3/force-template.html");
            JtwigModel model = JtwigModel.newModel()
               .with("analysisName", analysisName)
               .with("analysisStatistics", String.format("Classes: %d, Methods %d, Fields: %d", classCount, methodCount, fieldCount))
               .with("active", active?1:0)
               .with("size", size)
               .with("nodes", nodes.toString(WriterConfig.PRETTY_PRINT))
               .with("links", links.toString(WriterConfig.PRETTY_PRINT));
            template.render(model, output);
         }
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, boolean pureInterface, String... superObjectFqns) {
      classCount++;
      objectInterface.computeIfAbsent(objectFqn, k -> new CompletableFuture<>()).complete(pureInterface);
      return new Dependencies.Object() {
         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            methodCount++;
            JsonObject jsonMethod = new JsonObject();
            jsonMethod.add("id", methodId(objectFqn, methodName));
            jsonMethod.add("type", "method");
            jsonMethod.add("class", objectFqn);
            jsonMethod.add("interface", pureInterface?1:0);
            jsonMethod.add("ownerclass", ownerClass());
            jsonMethod.add("ownersimpleclass", ownerClass().substring(ownerClass().lastIndexOf('.')+1));
            jsonMethod.add("name", methodName+"()");
            layout.layoutObject(ownerClass(), jsonMethod);
            nodes.add(jsonMethod);
            return new Dependencies.Method() {
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  JsonObject jsonCall = new JsonObject();
                  jsonCall.add("source", methodId(objectFqn, methodName));
                  jsonCall.add("target", methodId(calledObjectFqn, calledMethodName));
                  objectInterface.computeIfAbsent(calledObjectFqn, k -> new CompletableFuture<>())
                     .thenAccept(calledPureInterface -> jsonCall.add("interface", calledPureInterface));
                  links.add(jsonCall);
               }

               @Override
               public void reference(String referenceObjectFqn, String referenceFieldName) {
                  JsonObject jsonReference = new JsonObject();
                  jsonReference.add("source", methodId(objectFqn, methodName));
                  jsonReference.add("target", fieldId(referenceObjectFqn, referenceFieldName));
                  links.add(jsonReference);
               }

               @Override
               public void close() {
               }
            };
         }

         @Override
         public void field(String fieldName) {
            fieldCount++;
            JsonObject jsonField = new JsonObject();
            jsonField.add("id", fieldId(objectFqn, fieldName));
            jsonField.add("type", "field");
            jsonField.add("interface", pureInterface?1:0);
            jsonField.add("name", fieldName);
            jsonField.add("ownerclass", ownerClass());
            jsonField.add("ownersimpleclass", ownerClass().substring(ownerClass().lastIndexOf('.')+1));
            layout.layoutObject(ownerClass(), jsonField);
            nodes.add(jsonField);
         }

         private String ownerClass() {
            if (objectFqn.contains("$")) {
               return objectFqn.substring(0, objectFqn.indexOf('$'));
            } else {
               return objectFqn;
            }
         }

         @Override
         public void close() {
         }
      };
   }

   private static String methodId(String objectFqn, String methodName) {
      return objectFqn+"."+methodName+"()";
   }

   private static String fieldId(String objectFqn, String fieldName) {
      return objectFqn+"."+fieldName;
   }
}


