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

/**
 * Saves dependencies directly to a D3 graph.
 */
public final class D3Dependencies implements Dependencies {
   private final JsonArray nodes = new JsonArray();
   private final JsonArray links = new JsonArray();
   private final File outputFile;

   public D3Dependencies(File outputFile) {
      this.outputFile = outputFile;
   }

   @Override
   public void close() {
      try {
         try (FileOutputStream output = new FileOutputStream(outputFile)) {
            JtwigTemplate template = JtwigTemplate.classpathTemplate("/com/vanillasource/forcedep/d3/force-template.html");
            JtwigModel model = JtwigModel.newModel()
               .with("nodes", nodes.toString(WriterConfig.PRETTY_PRINT))
               .with("links", links.toString(WriterConfig.PRETTY_PRINT));
            template.render(model, output);
         }
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   @Override
   public Dependencies.Object object(String objectFqn, boolean local, String... superObjectFqns) {
      return new Dependencies.Object() {
         @Override
         public Dependencies.Method method(String methodName, boolean local) {
            JsonObject jsonMethod = new JsonObject();
            jsonMethod.add("id", methodId(objectFqn, methodName));
            jsonMethod.add("class", objectFqn);
            if (objectFqn.contains("$")) {
               jsonMethod.add("ownerclass", objectFqn.substring(0, objectFqn.indexOf('$')));
            } else {
               jsonMethod.add("ownerclass", objectFqn);
            }
            jsonMethod.add("name", methodName);
            nodes.add(jsonMethod);
            return new Dependencies.Method() {
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  JsonObject jsonCall = new JsonObject();
                  jsonCall.add("source", methodId(objectFqn, methodName));
                  jsonCall.add("target", methodId(calledObjectFqn, calledMethodName));
                  links.add(jsonCall);
               }

               @Override
               public void reference(String objectsFqn, String fieldName) {
                  // TODO
               }

               @Override
               public void close() {
               }
            };
         }

         @Override
         public void close() {
         }
      };
   }

   private static String methodId(String objectFqn, String methodName) {
      return objectFqn+"."+methodName+"()";
   }
}


